# sql/ —— 数据库说明（脚手架通用版，H2）

> 数据库统一使用 H2（MODE=Oracle 兼容模式）：零安装、零外部依赖，
> Flyway 启动时自动建表，**不要手工建业务表**。
>
> Flyway 迁移脚本放在各服务模块的 `src/main/resources/db/migration/h2/` 下，
> 服务启动时自动执行（`spring.flyway.locations=classpath:db/migration/h2`，
> 约定见 `common/core/src/main/resources/scaffold-defaults.yml`）。

## 服务 → 数据库对应（脚手架默认）

| 服务 | H2 库文件 | 说明 |
|---|---|---|
| system（系统管理） | `./data/h2/system` | 用户/角色/菜单/部门/字典等；auth 无独立表，共用本库 |
| job（定时任务） | `./data/h2/job` | 任务与执行日志表 |
| gen（代码生成） | `./data/h2/gen` | 代码生成配置表 |
| gateway / auth / file | 无库 | 网关、认证（Redis 会话）、文件（本地/MinIO）默认不落库；如需可仿照新增 |

## 启动步骤（新环境）

```bash
# ── 第 1 步：配置连接（默认 local profile 已内置，无需额外配置）──
#   spring.datasource.url = jdbc:h2:file:./data/h2/<svc>;MODE=Oracle;AUTO_SERVER=TRUE

# ── 第 2 步：起服务，Flyway 自动建表 ──
#   启动顺序：nacos → gateway → auth → system → job → gen
#   迁移脚本位置：services/<svc>/src/main/resources/db/migration/h2/V*.sql
#   注意：多个服务共用同一 H2 库时，必须各自设置
#   spring.flyway.table: flyway_history_<模块名> 防版本撞号（scaffold-defaults.yml 已注明）
```

## 验证

```sql
-- 确认 Flyway 已建表且历史表存在
SELECT table_name FROM information_schema.tables ORDER BY table_name;
SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;
```

> 说明：H2 URL 中的 `MODE=Oracle` 是 H2 的兼容模式开关（空串即 NULL、SYSDATE、
> MERGE INTO 等语义），与 Oracle 数据库无关；迁移脚本按该方言编写。
