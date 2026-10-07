# scripts 目录说明

| 脚本 | 用途 |
|---|---|
| `gen_dialect_migrations.py` | 从 h2 基线生成各数据库方言家族迁移（见 docs/multi-database-guide.md） |
| `parse_pit_reports.py` | 汇总 PIT 变异测试存活变异体 → target/pit-survived.csv |
| `new-project.ps1` / `.sh` | 从脚手架生成新项目（占位符替换） |
| `regress_family.py` | 方言家族回归 |

## Dubbo Triple 桩重新生成（api 三模块）

`api/system-api`、`api/audit-api`、`api/file-api` 的 protobuf 消息类与 Dubbo Triple
适配桩是**签入的生成物**，位于各模块 `src/generated/java`。原生成器 `dubbo-maven-plugin`
（`dubboGenerateType=tri`）没有 Gradle 等价插件，Gradle 迁移后重新生成的两种方式：

1. **临时回退 Maven**（推荐，直到 dubbo 官方提供 Gradle 插件）：在任意旧 Maven 环境
   （或 `git checkout` 一个含 pom 的历史提交）执行
   `mvn generate-sources -pl api/system-api,api/audit-api,api/file-api`，
   然后把 `target/generated-sources/protobuf/java` 的内容同步回 `src/generated/java`。
2. **等 dubbo 官方 Gradle 插件**：跟踪 dubbo 仓库 `dubbo-maven-plugin` 的 Gradle 支持计划。

`.proto` 文件仍是契约唯一事实源；`src/generated/java` 内容勿手改。
