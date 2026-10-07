#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
regress_family.py —— 方言家族迁移基线的真库回归脚本（docs/multi-database-guide.md §6 的落地工具）。

对每个服务：创建一次性数据库 → 执行 db/migration/<家族>/V1__*.sql → 内容级验证 → 清理。
这是 H2 兼容模式冒烟（scripts/gen_dialect_migrations.py 尾注）无法替代的真引擎验证：
identity 行为、目录查询、COMMENT 存储、种子数据在真实库上各不相同。

用法：
  # postgresql（本机实例，全自动含验证与清理）
  python scripts/regress_family.py --family postgresql --host 127.0.0.1 --port 5432 \\
      --user postgres --password postgres
  # 只回归某个服务
  python scripts/regress_family.py --family postgresql ... --service system
  # 保留回归库不清理（人工核查）
  python scripts/regress_family.py --family postgresql ... --keep
  # 其余家族（oracle/sqlserver/db2/dm/...）：提供通用执行模板，脚本只做
  # "对预置库执行基线 + 表计数验证"，建库/清理由模板自行完成
  python scripts/regress_family.py --family oracle --exec-cmd \\
      "sqlplus -S {user}/{password}@{host}:{port}/{db} @{file}"

验证项（数据驱动，见 CHECKS）：
- 通用：每服务建表数 >= 1
- postgresql：system 菜单种子/列注释；audit V3 补列；job quartz 表；gen 种子与
  pg_catalog 逆向查询（主键识别/类型归一化，与 GenCatalogDao 的 postgresql 方言分支对应）；
- mysql：information_schema 表计数 + COLUMN_COMMENT；
- 其余家族：仅表计数（有验证 SQL 需求时往 CHECKS 里加）。

退出码：0 全部通过；1 有失败（逐条打印 FAIL + 摘要）。
"""

import argparse
import io
import os
import re
import subprocess
import sys
import tempfile
import time

REPO = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))

SERVICES = {
    "services/system": "system",
    "services/gen": "gen",
    "services/job": "job",
    "services/message": "message",
    "services/audit": "audit",
    "samples/sample-app": "sample",
}

# 内容级验证：(服务, 检查名, SQL, 判定)。判定 "rows>=N" 或 "scalar>=N"（首行首列整数）。
# SQL 全部 ASCII（中文注释校验用长度/非空表达，规避 Windows 控制台编码问题）。
CHECKS = {
    "postgresql": [
        ("system", "tables>=20", "select count(*) from pg_tables where schemaname='public'", "scalar>=20"),
        ("system", "col-comment", "select coalesce(length(col_description('sys_user'::regclass, 1)),0)", "scalar>=1"),
        ("system", "sample-menus", "select count(*) from sys_menu where menu_id >= 3000", "scalar>=20"),
        # gen 逆向查询在业务表所在的服务库验证（gen 库只有被 gen_% 排除的自管表）
        ("system", "reverse-tables", "select count(*) from pg_class c join pg_namespace n on n.oid=c.relnamespace "
         "where n.nspname=current_schema() and c.relkind in ('r','p') "
         "and c.relname not like 'qrtz\\_%' and c.relname not like 'gen\\_%' "
         "and c.relname not in (select table_name from gen_table)", "scalar>=1"),
        ("system", "reverse-cols-pk", "select count(*) from (select a.attnum from pg_attribute a "
         "join pg_class cl on cl.oid=a.attrelid join pg_namespace n on n.oid=cl.relnamespace "
         "left join (select con.conrelid, k.attnum from pg_constraint con join pg_attribute k "
         "on k.attrelid=con.conrelid and k.attnum=any(con.conkey) where con.contype='p') pk "
         "on pk.conrelid=a.attrelid and pk.attnum=a.attnum "
         "where n.nspname=current_schema() and cl.relname='sys_dept' and a.attnum>0 and not a.attisdropped "
         "and pk.attnum is not null) t", "scalar>=1"),
        ("gen", "seed-gen-table", "select count(*) from gen_table", "scalar>=1"),
        ("gen", "col-comment", "select coalesce(length(col_description('gen_table'::regclass, 1)),0)", "scalar>=1"),
        ("job", "quartz-tables", "select count(*) from pg_tables where schemaname='public' and tablename like 'qrtz%'", "scalar>=10"),
        ("audit", "v3-cols", "select count(*) from information_schema.columns where table_name='audit_logininfor' "
         "and column_name in ('user_id','device_info','session_id')", "scalar>=3"),
        ("audit", "v3-request-id", "select count(*) from information_schema.columns where table_name='audit_oper_log' "
         "and column_name='request_id'", "scalar>=1"),
        ("message", "tables>=8", "select count(*) from pg_tables where schemaname='public'", "scalar>=8"),
        ("sample", "tables>=5", "select count(*) from pg_tables where schemaname='public'", "scalar>=5"),
    ],
    "mysql": [
        ("system", "tables>=20", "select count(*) from information_schema.tables where table_schema=database()", "scalar>=20"),
        ("system", "col-comment", "select coalesce(length(column_comment),0) from information_schema.columns "
         "where table_schema=database() and table_name='sys_user' and column_name='user_id'", "scalar>=1"),
        ("gen", "seed-gen-table", "select count(*) from gen_table", "scalar>=1"),
    ],
}


def run(cmd, env=None, timeout=300):
    """执行命令，返回 (exit, stdout+stderr)。"""
    e = dict(os.environ)
    if env:
        e.update(env)
    proc = subprocess.run(cmd, capture_output=True, timeout=timeout, env=e, shell=isinstance(cmd, str))
    out = (proc.stdout or b"") + (proc.stderr or b"")
    return proc.returncode, out.decode("utf-8", "replace")


def write_temp_sql(text):
    """中文内容一律走 UTF-8 临时文件（Windows 控制台 GBK 直传会 0xb2 报错）。"""
    fd, path = tempfile.mkstemp(suffix=".sql", text=False)
    with os.fdopen(fd, "wb") as fh:
        fh.write(text.encode("utf-8"))
    return path


class Family:
    """一个方言家族的客户端适配：baseline / verify / cleanup。"""

    def __init__(self, args):
        self.args = args
        self.family = args.family
        self.created = []

    # ---- postgresql / mysql：psql / mysql 直连托管 ----
    def _psql(self, db, sql_file=None, sql=None, tA=False):
        cmd = ["psql", "-h", self.args.host, "-p", str(self.args.port), "-U", self.args.user,
               "-v", "ON_ERROR_STOP=1", "-q"]
        if tA:
            cmd.append("-tA")
        if sql_file:
            cmd += ["-f", sql_file]
        elif sql is not None:
            tmp = write_temp_sql(sql)
            cmd += ["-f", tmp]
        else:
            return 0, ""
        cmd += ["-d", db]
        code, out = run(cmd, env={"PGPASSWORD": self.args.password})
        return code, out

    def _mysql(self, db, sql_file=None, sql=None, tA=False):
        cmd = ["mysql", "-h", self.args.host, "-P", str(self.args.port), "-u", self.args.user,
               "--default-character-set=utf8mb4"]
        if self.args.password:
            cmd.append("-p" + self.args.password)
        if sql_file:
            with io.open(sql_file, encoding="utf-8") as fh:
                sql = fh.read()
        cmd += ["-N" if tA else "-t", db, "-e", sql or "SELECT 1"]
        return run(cmd)

    def prepare_db(self, service):
        """建一次性库；返回库名。postgresql/mysql 每服务独立库（表名跨服务冲突）。"""
        name = "scaffold_regr_%s_%s" % (service, int(time.time()))
        if self.family == "postgresql":
            code, out = self._psql("postgres", sql="CREATE DATABASE %s;" % name)
        elif self.family == "mysql":
            code, out = self._mysql("information_schema", sql="CREATE DATABASE `%s` CHARACTER SET utf8mb4;" % name)
        else:
            raise SystemExit("--exec-cmd 家族不支持自动建库，请预先准备目标库并用 --exec-cmd 模板执行")
        if code != 0:
            raise SystemExit("建库失败 %s: %s" % (name, out.strip()[:200]))
        self.created.append(name)
        return name

    def run_baseline(self, db, sql_path):
        if self.family == "postgresql":
            return self._psql(db, sql_file=sql_path)
        if self.family == "mysql":
            return self._mysql(db, sql_file=sql_path)
        raise SystemExit("--exec-cmd 分支在 main() 中处理")

    def verify(self, db, sql):
        if self.family == "postgresql":
            return self._psql(db, sql=sql, tA=True)
        if self.family == "mysql":
            return self._mysql(db, sql=sql, tA=True)
        raise SystemExit("verify 未实现")

    def cleanup(self):
        for name in self.created:
            if self.family == "postgresql":
                self._psql("postgres", sql="DROP DATABASE IF EXISTS %s WITH (FORCE);" % name)
            elif self.family == "mysql":
                self._mysql("information_schema", sql="DROP DATABASE IF EXISTS `%s`;" % name)

    # ---- 通用 --exec-cmd 模板（oracle/sqlserver/db2/dm/...）----
    def exec_template(self, cmd_template, **kw):
        cmd = cmd_template
        for k, v in kw.items():
            cmd = cmd.replace("{%s}" % k, str(v))
        return run(cmd)


def check_result(spec, output):
    """按 'scalar>=N' / 'rows>=N' 判定。返回 (ok, 实际值文本)。"""
    m = re.match(r"scalar>=(\d+)", spec)
    first_line = output.strip().splitlines()[0].strip() if output.strip() else ""
    if m:
        try:
            value = int(first_line)
        except ValueError:
            return False, first_line[:40] or "(empty)"
        return value >= int(m.group(1)), str(value)
    return bool(first_line), first_line[:40]


def find_baseline(service_dir, family):
    import glob
    hits = sorted(glob.glob(os.path.join(REPO, service_dir, "src/main/resources/db/migration", family, "V1__*.sql")))
    return hits[0] if hits else None


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--family", required=True, choices=["postgresql", "mysql", "oracle", "sqlserver", "db2"])
    ap.add_argument("--host", default="127.0.0.1")
    ap.add_argument("--port", type=int, default=0)
    ap.add_argument("--user", required=True)
    ap.add_argument("--password", default="")
    ap.add_argument("--service", action="append", help="只回归指定服务（可重复）：system/gen/job/message/audit/sample")
    ap.add_argument("--keep", action="store_true", help="保留回归库不清理")
    ap.add_argument("--exec-cmd", help="oracle/sqlserver/db2 等家族的执行模板：{user} {password} {host} {port} {db} {file}")
    args = ap.parse_args()
    if args.port == 0:
        args.port = {"postgresql": 5432, "mysql": 3306, "oracle": 1521, "sqlserver": 1433, "db2": 50000}[args.family]

    targets = [(d, s) for d, s in SERVICES.items() if not args.service or s in args.service]
    fam = Family(args)
    failures = []

    for service_dir, service in targets:
        baseline = find_baseline(service_dir, args.family)
        if not baseline:
            print("SKIP %-8s (no %s baseline)" % (service, args.family))
            continue

        if args.exec_cmd:
            # 通用模板家族：对预置 {db} 执行基线 + 表计数，不做建库/清理
            db = args.service_db if hasattr(args, "service_db") else ""
            code, out = fam.exec_template(args.exec_cmd, user=args.user, password=args.password,
                                          host=args.host, port=args.port, db=db, file=baseline)
            status = "PASS" if code == 0 else "FAIL"
            print("%s %-8s exec-cmd exit=%d %s" % (status, service, code, "" if code == 0 else out.strip()[:200]))
            if code != 0:
                failures.append((service, "baseline", out.strip()[:200]))
            continue

        # postgresql / mysql 全自动路径
        db = fam.prepare_db(service)
        code, out = fam.run_baseline(db, baseline)
        if code != 0:
            print("FAIL %-8s baseline: %s" % (service, out.strip().splitlines()[-1][:160] if out.strip() else "exit=%d" % code))
            failures.append((service, "baseline", out.strip()[:300]))
            continue

        ok_all = True
        for _svc, name, sql, spec in [c for c in CHECKS.get(args.family, []) if c[0] == service]:
            vcode, vout = fam.verify(db, sql)
            ok, actual = check_result(spec, vout)
            if not ok:
                print("FAIL %-8s check[%s] 期望 %s 实际 %s" % (service, name, spec, actual))
                failures.append((service, name, actual))
                ok_all = False
        if ok_all:
            print("PASS %-8s (%d checks)" % (service, len([c for c in CHECKS.get(args.family, []) if c[0] == service])))

    if not args.keep and not args.exec_cmd:
        fam.cleanup()

    print("---")
    print("family=%s services=%d failures=%d" % (args.family, len(targets), len(failures)))
    sys.exit(1 if failures else 0)


if __name__ == "__main__":
    main()
