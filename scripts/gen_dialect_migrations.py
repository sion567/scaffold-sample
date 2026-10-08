#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
gen_dialect_migrations.py —— 从各服务的 h2 基线生成方言家族的 Flyway 迁移脚本。

背景与映射关系见 docs/multi-database-guide.md：9 库归并 6 方言，每个服务在
db/migration/ 下除 h2/（本地零安装基线，事实源）外，还应有 mysql/、oracle/、
postgresql/、sqlserver/、db2/ 家族目录（oracle 含达梦，postgresql 含
openGauss/瀚高/金仓 PG 模式，复用同目录）。

用法：
    python scripts/gen_dialect_migrations.py            # 生成全部缺失家族（已存在的跳过）
    python scripts/gen_dialect_migrations.py --force    # 覆盖重新生成（h2 基线演进后）

约定：
- 输出为各家族单个 V1__baseline.sql（多份 h2 迁移按版本顺序合并，同
  services/audit 的 oracle/V1__baseline.sql 既有约定）；
- 生成物只依赖 h2 基线翻译，手工改动会在下次 --force 时丢失——方言特有
  修正应改本脚本的映射表，而不是改生成文件；
- 翻译规则：类型映射 + identity 方言化 + 注释方言化（COMMENT ON / 内联
  COMMENT / sp_addextendedproperty）+ schema 前缀剥离 + SYSTIMESTAMP 等令牌映射；
- 生成后建议用 H2 对应兼容模式做语法冒烟（见脚本尾部注释），真实库行为
  仍按 docs/multi-database-guide.md §6 接入清单回归。
"""

import argparse
import glob
import io
import os
import re
import sys

REPO = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))

# 各服务 h2 基线目录（事实源）；audit/message 已有手工维护的 oracle/，不覆盖
SERVICES = {
    "services/system": None,
    "services/gen": None,
    "services/job": None,
    "services/message": None,
    "services/audit": None,
    "samples/sample-app": None,
}
FAMILIES = ["oracle", "mysql", "postgresql", "sqlserver", "db2"]

SCHEMA_PREFIX = re.compile(r"\b(SYSTEM_DB|JOB_DB|GEN_DB|SAMPLE_DB|AUDIT_DB|MESSAGE_DB)\.", re.IGNORECASE)

IDENTITY_RE = re.compile(
    r"GENERATED\s+BY\s+DEFAULT\s+(?:ON\s+NULL\s+)?AS\s+IDENTITY"
    r"(?:\s*\(\s*START\s+WITH\s+(\d+)\s+INCREMENT\s+BY\s+(\d+)\s*\))?",
    re.IGNORECASE,
)

# 表注释同样支持可选 schema 前缀（与下方 COLUMN 一致）；否则 COMMENT ON TABLE XXX_DB.T
# 不匹配、表注释在家族基线中被静默丢弃，而落入 DML 透传分支后 mysql 无法执行
COMMENT_TABLE_RE = re.compile(r"^\s*COMMENT\s+ON\s+TABLE\s+(?:[\w]+\.)?([\w]+)\s+IS\s+'(.*)'\s*;?\s*$", re.IGNORECASE)
# 可选 schema 前缀：SYSTEM_DB.SYS_DEPT.DEPT_ID → 表=SYS_DEPT 列=DEPT_ID（.+? 非贪婪会把三段错位成两段，必须显式可选段）
COMMENT_COLUMN_RE = re.compile(r"^\s*COMMENT\s+ON\s+COLUMN\s+(?:[\w]+\.)?([\w]+)\.([\w]+)\s+IS\s+'(.*)'\s*;?\s*$", re.IGNORECASE)

CREATE_TABLE_RE = re.compile(r"^\s*CREATE\s+TABLE\s+(?:[\w]+\.)?([A-Za-z_]\w*)\s*\(", re.IGNORECASE)
CREATE_INDEX_RE = re.compile(r"^\s*CREATE\s+(UNIQUE\s+)?INDEX\s+", re.IGNORECASE)
ALTER_RESTART_RE = re.compile(
    r"^\s*ALTER\s+TABLE\s+(?:[\w]+\.)?([A-Za-z_]\w*)\s+ALTER\s+COLUMN\s+([A-Za-z_]\w*)\s+RESTART\s+WITH\s+(\d+)\s*;?\s*$",
    re.IGNORECASE,
)
INSERT_RE = re.compile(r"^\s*INSERT\s+INTO\s+(?:[\w]+\.)?([A-Za-z_]\w*)\b", re.IGNORECASE)
ALTER_FK_RE = re.compile(r"^\s*ALTER\s+TABLE\s+", re.IGNORECASE)

# h2 基线中的 Oracle 风格类型 → 各方言
# NUMBER 精度规则单独处理：p>=9 → BIGINT，否则 INT；带标度 → DECIMAL(p,s)
TYPE_MAP = {
    "oracle":      {"VARCHAR2": "VARCHAR", "NVARCHAR2": "NVARCHAR", "VARCHAR": "VARCHAR", "CHAR": "CHAR", "CLOB": "CLOB", "BLOB": "BLOB", "TIMESTAMP": "TIMESTAMP", "DATE": "DATE"},
    "mysql":       {"VARCHAR2": "VARCHAR", "NVARCHAR2": "VARCHAR", "VARCHAR": "VARCHAR", "CHAR": "CHAR", "CLOB": "LONGTEXT", "BLOB": "LONGBLOB", "TIMESTAMP": "DATETIME", "DATE": "DATE"},
    "postgresql":  {"VARCHAR2": "VARCHAR", "NVARCHAR2": "VARCHAR", "VARCHAR": "VARCHAR", "CHAR": "CHAR", "CLOB": "TEXT", "BLOB": "BYTEA", "TIMESTAMP": "TIMESTAMP", "DATE": "DATE"},
    "sqlserver":   {"VARCHAR2": "NVARCHAR", "NVARCHAR2": "NVARCHAR", "VARCHAR": "NVARCHAR", "CHAR": "NCHAR", "CLOB": "NVARCHAR(MAX)", "BLOB": "VARBINARY(MAX)", "TIMESTAMP": "DATETIME2", "DATE": "DATE"},
    "db2":         {"VARCHAR2": "VARCHAR", "NVARCHAR2": "VARGRAPHIC", "VARCHAR": "VARCHAR", "CHAR": "CHAR", "CLOB": "CLOB", "BLOB": "BLOB", "TIMESTAMP": "TIMESTAMP", "DATE": "DATE"},
}

# 种子数据 / 默认值中的当前时间令牌
NOW_TOKEN = {
    "oracle": "SYSTIMESTAMP",
    "mysql": "CURRENT_TIMESTAMP",
    "postgresql": "CURRENT_TIMESTAMP",
    "sqlserver": "SYSDATETIME()",
    "db2": "CURRENT TIMESTAMP",
}
DEFAULT_NOW = {
    "oracle": "CURRENT_TIMESTAMP",
    "mysql": "CURRENT_TIMESTAMP",
    "postgresql": "CURRENT_TIMESTAMP",
    "sqlserver": "CURRENT_TIMESTAMP",
    "db2": "CURRENT TIMESTAMP",
}


def strip_prefix(text):
    return SCHEMA_PREFIX.sub("", text)


def map_number(precision, scale, family):
    if scale:
        return "DECIMAL(%s,%s)" % (precision, scale)
    return "BIGINT" if int(precision) >= 9 else "INT"


def parse_type(def_text):
    """从列定义中解析 (base_type, length, scale)；不匹配已知类型则返回 (None, None, None)。"""
    m = re.match(r"^(NUMBER|VARCHAR2|NVARCHAR2|VARCHAR|CHAR|NCHAR)\s*\(\s*(\d+)\s*(?:,\s*(\d+))?\s*\)", def_text, re.IGNORECASE)
    if m:
        return m.group(1).upper(), m.group(2), m.group(3)
    m = re.match(r"^NUMBER\b(?!\s*\()", def_text, re.IGNORECASE)
    if m:
        return "NUMBER_BARE", None, None
    m = re.match(r"^TIMESTAMP\s*\(\s*(\d+)\s*\)", def_text, re.IGNORECASE)
    if m:
        return "TIMESTAMP", m.group(1), None
    m = re.match(r"^(TIMESTAMP|DATE|INT|INTEGER|BIGINT|SMALLINT|TINYINT|FLOAT|DOUBLE|REAL|BOOLEAN|BLOB|CLOB)\b", def_text, re.IGNORECASE)
    if m:
        return m.group(1).upper(), None, None
    return None, None, None


def esc_quote(text):
    return text.replace("'", "''")


class Table:
    def __init__(self, name):
        self.name = name
        self.columns = []          # dict: name, base, length, scale, identity(start,inc), default, not_null
        self.constraints = []      # 原文（CONSTRAINT ... / PRIMARY KEY ... / UNIQUE ...）
        self.comment = None
        self.col_comments = {}     # col_name(lower) -> text


def parse_h2_scripts(files):
    """把按版本排序的 h2 脚本解析为语句列表 + 表模型字典。"""
    tables = {}
    statements = []  # ('table', Table) | ('raw', text) | ('comment_table', tab, text) | ('comment_col', tab, col, text) | ('restart', tab, col, n) | ('insert', tab, text) | ('index', text) | ('alter', text)
    current = None

    for path in files:
        with io.open(path, encoding="utf-8") as fh:
            lines = fh.readlines()
        i = 0
        while i < len(lines):
            line = lines[i]
            stripped = line.strip()

            m = CREATE_TABLE_RE.match(line)
            if m:
                current = Table(m.group(1))
                tables[current.name.lower()] = current
                i += 1
                depth = 1
                body = []
                while i < len(lines) and depth > 0:
                    for ch in lines[i]:
                        if ch == "(":
                            depth += 1
                        elif ch == ")":
                            depth -= 1
                    if depth <= 0:
                        break
                    body.append(lines[i].rstrip())
                    i += 1
                parse_body(current, body)
                statements.append(("table", current))
                # 跳过收尾 ");"
                i += 1
                current = None
                continue

            m = COMMENT_TABLE_RE.match(line)
            if m:
                tab = strip_prefix(m.group(1)).strip().lower()
                if tab in tables:
                    tables[tab].comment = m.group(2)
                statements.append(("comment_table", tab, m.group(2)))
                i += 1
                continue

            m = COMMENT_COLUMN_RE.match(line)
            if m:
                tab = strip_prefix(m.group(1)).strip().lower()
                col = m.group(2).strip().lower()
                if tab in tables:
                    tables[tab].col_comments[col] = m.group(3)
                statements.append(("comment_col", tab, col, m.group(3)))
                i += 1
                continue

            m = ALTER_RESTART_RE.match(line)
            if m:
                statements.append(("restart", m.group(1).lower(), m.group(2).lower(), m.group(3)))
                i += 1
                continue

            if INSERT_RE.match(line):
                buf = [stripped]
                while not re.search(r";\s*$", stripped) and i + 1 < len(lines):
                    i += 1
                    stripped = lines[i].strip()
                    buf.append(stripped)
                if not re.search(r";\s*$", stripped):
                    raise SystemExit("INSERT 语句未正常结束: %s" % " ".join(buf)[:80])
                whole = " ".join(buf)
                statements.append(("insert", INSERT_RE.match(buf[0]).group(1).lower(), whole))
                i += 1
                continue

            if CREATE_INDEX_RE.match(line) or ALTER_FK_RE.match(line):
                if re.search(r";\s*$", stripped):
                    statements.append(("raw", strip_prefix(stripped)))
                    i += 1
                else:
                    buf = [line.rstrip()]
                    i += 1
                    while i < len(lines) and not re.search(r";\s*$", lines[i]):
                        buf.append(lines[i].rstrip())
                        i += 1
                    if i < len(lines):
                        buf.append(lines[i].rstrip())
                        i += 1
                    statements.append(("raw", strip_prefix("\n".join(buf))))
                continue

            if stripped.startswith("--") or not stripped:
                statements.append(("raw", stripped))
                i += 1
                continue

            # 其余语句（UPDATE/DELETE/MERGE 等 DML）：整句透传，emit 时统一剥
            # schema 前缀并映射时间令牌。没有此兜底时新增迁移里的 DML 会被静默丢弃。
            if re.search(r";\s*$", stripped):
                statements.append(("raw", strip_prefix(stripped)))
                i += 1
                continue
            buf = [line.rstrip()]
            i += 1
            while i < len(lines) and not re.search(r";\s*$", lines[i]):
                buf.append(lines[i].rstrip())
                i += 1
            if i >= len(lines):
                raise SystemExit("语句未正常结束(缺分号): %s" % " ".join(buf)[:80])
            buf.append(lines[i].rstrip())
            i += 1
            statements.append(("raw", strip_prefix("\n".join(buf))))
            continue
    return tables, statements


def parse_body(table, body_lines):
    for raw in body_lines:
        line = raw.strip()
        if not line or line.startswith("--"):
            continue
        line = strip_prefix(line)
        bare = line.rstrip(",").strip()
        if re.match(r"^(CONSTRAINT|PRIMARY\s+KEY|UNIQUE|FOREIGN\s+KEY|CHECK)\b", bare, re.IGNORECASE):
            table.constraints.append(bare)
            continue
        m = re.match(r"^([A-Za-z_]\w*)\s+(.*)$", bare)
        if not m:
            continue
        name, rest = m.group(1), m.group(2).strip()
        identity = IDENTITY_RE.search(rest)
        # 先剔除 identity 子句再找 DEFAULT，避免误吞 "BY DEFAULT AS" 里的 AS
        rest_wo_identity = IDENTITY_RE.sub("", rest) if identity else rest
        dm = re.search(r"\bDEFAULT\s+('(?:[^']*)'|[\w()]+)(?!\s+AS\b)", rest_wo_identity, re.IGNORECASE)
        default = dm.group(1) if dm else None
        not_null = re.search(r"\bNOT\s+NULL\b", rest, re.IGNORECASE) is not None
        # 列级内联 PRIMARY KEY（如 audit 的 role_id BIGINT ... IDENTITY PRIMARY KEY）转为表级约束，避免丢失
        inline_pk = re.search(r"\bPRIMARY\s+KEY\b", rest_wo_identity, re.IGNORECASE) is not None
        base, length, scale = parse_type(rest)
        if identity:
            ident = (identity.group(1) or "1", identity.group(2) or "1")
        else:
            ident = None
        table.columns.append({
            "name": name, "base": base, "length": length, "scale": scale,
            "identity": ident,
            "default": default, "not_null": not_null,
            "inline_pk": inline_pk,
        })


def render_type(col, family):
    base, length, scale = col["base"], col["length"], col["scale"]
    if base == "NUMBER":
        return map_number(length, scale, family)
    if base == "NUMBER_BARE":
        # Oracle 无精度 NUMBER（仓库内仅 sort 一列）：整数语义足够，统一 DECIMAL(20,0)
        return "DECIMAL(20,0)"
    mapped = TYPE_MAP[family].get(base, base)
    if length and base in ("VARCHAR2", "NVARCHAR2", "VARCHAR", "CHAR", "NCHAR"):
        return "%s(%s)" % (mapped, length)
    if base == "TIMESTAMP" and length:
        # TIMESTAMP(0)：秒级精度，各方言同名保留精度位（mysql 映射为 DATETIME(0)）
        return "%s(%s)" % (mapped, length)
    return mapped


def render_identity(col, family):
    start, inc = col["identity"]
    if family == "mysql":
        return "AUTO_INCREMENT"
    if family == "sqlserver":
        return "IDENTITY(%s,%s)" % (start, inc)
    return "GENERATED BY DEFAULT AS IDENTITY (START WITH %s INCREMENT BY %s)" % (start, inc)


def emit_table(table, family, out):
    cols_sql = []
    auto_inc_start = None
    for col in table.columns:
        if col["base"] is None:
            raise SystemExit("无法识别列类型: %s.%s" % (table.name, col["name"]))
        parts = [col["name"], render_type(col, family)]
        if col["identity"]:
            parts.append(render_identity(col, family))
            if family == "mysql" and int(col["identity"][0]) > 1:
                auto_inc_start = int(col["identity"][0])
        if col["not_null"]:
            parts.append("NOT NULL")
        if col["default"] is not None:
            d = col["default"]
            if re.fullmatch(r"CURRENT_TIMESTAMP|SYSTIMESTAMP", d, re.IGNORECASE):
                d = DEFAULT_NOW[family]
            parts.append("DEFAULT %s" % d)
        if family == "mysql" and col["name"].lower() in table.col_comments:
            parts.append("COMMENT '%s'" % esc_quote(table.col_comments[col["name"].lower()]))
        cols_sql.append("    " + " ".join(parts))
        if col["inline_pk"]:
            cols_sql.append("    PRIMARY KEY (%s)" % col["name"])

    for c in table.constraints:
        cols_sql.append("    " + c)

    options = []
    if family == "mysql":
        options.append("ENGINE=InnoDB")
        if auto_inc_start:
            options.append("AUTO_INCREMENT=%d" % auto_inc_start)
        if table.comment:
            options.append("COMMENT='%s'" % esc_quote(table.comment))
    out.append("CREATE TABLE %s (\n%s\n)%s;" % (table.name, ",\n".join(cols_sql), (" " + " ".join(options)) if options else ""))

    # 注释方言化：oracle/pg/db2 用 COMMENT ON；sqlserver 用扩展属性（gen 逆向读 MS_Description）
    if family in ("oracle", "postgresql", "db2"):
        if table.comment:
            out.append("COMMENT ON TABLE %s IS '%s';" % (table.name, esc_quote(table.comment)))
        for col in table.columns:
            text = table.col_comments.get(col["name"].lower())
            if text:
                out.append("COMMENT ON COLUMN %s.%s IS '%s';" % (table.name, col["name"], esc_quote(text)))
    elif family == "sqlserver":
        if table.comment:
            out.append(sp_addprop(table.name, None, table.comment))
        for col in table.columns:
            text = table.col_comments.get(col["name"].lower())
            if text:
                out.append(sp_addprop(table.name, col["name"], text))


def sp_addprop(table, column, text):
    level = ("@level0type=N'SCHEMA', @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'%s'" % table)
    if column:
        level += ", @level2type=N'COLUMN', @level2name=N'%s'" % column
    return "EXEC sp_addextendedproperty @name=N'MS_Description', @value=N'%s', %s;" % (esc_quote(text), level)


def emit_restart(table, col, num, family, out):
    n = int(num)
    if family == "oracle":
        out.append("ALTER TABLE %s MODIFY (%s GENERATED BY DEFAULT AS IDENTITY (START WITH %d));" % (table, col, n))
    elif family == "mysql":
        out.append("ALTER TABLE %s AUTO_INCREMENT = %d;" % (table, n))
    elif family == "postgresql":
        out.append("ALTER TABLE %s ALTER COLUMN %s RESTART WITH %d;" % (table, col, n))
    elif family == "sqlserver":
        out.append("DBCC CHECKIDENT ('%s', RESEED, %d);" % (table, n - 1))
    else:
        out.append("ALTER TABLE %s ALTER COLUMN %s RESTART WITH %d;" % (table, col, n))


def transform_insert(text, family):
    text = strip_prefix(text)
    return re.sub(r"\bSYSTIMESTAMP\b", NOW_TOKEN[family], text, flags=re.IGNORECASE)


GENERATED_MARK = "-- GENERATED-BY gen_dialect_migrations.py"


def generate(service_dir, family, force):
    h2_dir = os.path.join(REPO, service_dir, "src/main/resources/db/migration/h2")
    files = sorted(glob.glob(os.path.join(h2_dir, "V*.sql")))
    if not files:
        return None
    out_dir = os.path.join(REPO, service_dir, "src/main/resources/db/migration", family)
    target = os.path.join(out_dir, "V1__baseline.sql")
    if os.path.exists(target):
        # 只允许覆盖本脚本生成的文件（带标记）；audit/message 手工维护的 oracle 基线等不受 --force 影响
        with io.open(target, encoding="utf-8") as fh:
            head = fh.read(400)
        if GENERATED_MARK not in head:
            return "skip(handwritten)"
    os.makedirs(out_dir, exist_ok=True)

    tables, statements = parse_h2_scripts(files)
    identity_tables = {t.name.lower() for t in tables.values() if any(c["identity"] for c in t.columns)}

    out = [
        "-- %s %s 方言家族基线（由 scripts/gen_dialect_migrations.py 从 h2 基线生成，勿手工编辑；" % (service_dir, family),
        GENERATED_MARK,
        "-- 修正应改脚本映射表后 --force 重新生成；真实库接入回归见 docs/multi-database-guide.md §6）",
        "",
    ]
    id_insert_open = None
    for st in statements:
        kind = st[0]
        if kind == "table":
            out.append("")
            emit_table(st[1], family, out)
        elif kind in ("comment_table", "comment_col"):
            # 注释已并入 emit_table（oracle/pg/db2/sqlserver）或内联（mysql），此处跳过
            continue
        elif kind == "raw":
            text = strip_prefix(st[1])
            text = re.sub(r"\bSYSTIMESTAMP\b", NOW_TOKEN[family], text, flags=re.IGNORECASE)
            out.append(text)
        elif kind == "restart":
            out.append("")
            emit_restart(st[1], st[2], st[3], family, out)
        elif kind == "insert":
            tab = st[1]
            if family == "sqlserver" and tab in identity_tables:
                if id_insert_open != tab:
                    if id_insert_open:
                        out.append("SET IDENTITY_INSERT %s OFF;" % id_insert_open)
                    out.append("SET IDENTITY_INSERT %s ON;" % tab)
                    id_insert_open = tab
            else:
                if id_insert_open:
                    out.append("SET IDENTITY_INSERT %s OFF;" % id_insert_open)
                    id_insert_open = None
            out.append(transform_insert(st[2], family))
    if id_insert_open:
        out.append("SET IDENTITY_INSERT %s OFF;" % id_insert_open)

    with io.open(target, "w", encoding="utf-8", newline="\n") as fh:
        fh.write("\n".join(out).rstrip() + "\n")
    return "wrote"


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--force", action="store_true", help="覆盖已存在的家族基线")
    args = ap.parse_args()
    for service_dir in SERVICES:
        for family in FAMILIES:
            result = generate(service_dir, family, args.force)
            if result:
                print("%-22s %-11s %s" % (service_dir, family, result))


if __name__ == "__main__":
    main()

# 生成后的 H2 兼容模式语法冒烟（h2 jar 在本地仓库）：
#   java -cp <h2.jar> org.h2.tools.RunScript -url "jdbc:h2:mem:t;MODE=Oracle" -user sa -script <oracle/V1__baseline.sql>
#   postgresql/db2 家族同法换 MODE；mysql 家族先去掉内联 COMMENT；sqlserver 家族
#   先去掉 sp_addextendedproperty / SET IDENTITY_INSERT / DBCC 行并把 DATETIME2/NVARCHAR 映射回 H2 类型
