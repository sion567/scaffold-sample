# AGENTS.md — scaffold 工程约定（唯一事实源）

> 本文件面向**所有写代码的角色**：人类开发者、以及任何 AI 编码助手（Claude / Cursor / Copilot / Codex / ZCode …）。
> 各 AI 工具的指令文件（`CLAUDE.md`、`.cursor/rules/`、`.github/copilot-instructions.md`）**只是指向本文件的指针**，
> 不另立规矩；修改约定只改这里，并同步给团队。
>
> **会话中形成的新约定必须回写本文件——没写在这里的约定视为不存在。**

---

## 0. 这个工程是什么（30 秒）

业务无关的微服务脚手架（Java 17 · Spring Boot 3.5.x · Spring Cloud · Nacos · Dubbo 3 Triple · Spring Data JPA + Flyway · H2 本地零安装），新项目从它一键生成。工程结构、模块清单、快速开始见 **[README.md](README.md)**；新增模块的完整步骤见 **[docs/new-module-guide.md](docs/new-module-guide.md)**。

## 1. 改动放哪：模块归属决策（先判断再动手）

| 你要写的东西 | 放哪 | 判断标准 |
|---|---|---|
| 业务无关的纯库（工具类、拦截器、门面） | `common/<域>/` | 无 Spring Boot 自动装配需求，或只是被别人依赖 |
| 业务无关、引入即生效的能力 | `starters/<域>/` | 需要 auto-configuration + 配置开关；命名 `scaffold-spring-boot-starter-<域>` |
| 平台级独立部署的服务 | `services/<名>/` | 独立进程、有自己的 Flyway 与配置 |
| 跨服务 RPC 契约 | `api/<名>-api/` | Dubbo Triple（proto IDL 或纯 Java 接口）；只放接口 + DTO，不放实现 |

铁律：

- **业务逻辑禁止进 `common/` 和 `starters/`**——底座被所有下游项目引用，漂移成本最高。
- **`services/` 之间禁止直接依赖**——服务间只通过 `api/*-api` 契约走 Dubbo；网关只直连管理后台/聚合服务。
- starter 的契约：**引入即生效、配置可拔**（`scaffold.<域>.enabled` 总开关，`matchIfMissing` 明确）；自动装配条目写进 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`。
- 拿不准放哪时：读 README §1 + `docs/new-module-guide.md`，**还拿不准就问，不要猜**。

## 2. 硬约束（违反 = CI 红 / MR 打回）

1. **依赖版本一律走 [bom/build.gradle](bom/build.gradle)**（java-platform 平台）：模块 build.gradle 禁止出现硬编码版本（`version=1.0-SNAPSHOT` 例外）；引入新三方件，先在 BOM 的 `constraints { }` 块内加 `api("group:artifact:${xxxVersion}")` 并在 `gradle.properties` 补版本属性，说明用途后再使用。**约束禁止写在 constraints 块外**——java-platform 下裸 `api("g:a:v")` 是真实依赖，会被所有模块整包继承（曾导致无库服务被塞进 flyway/warm-flow/druid 而启动失败）。运行时桥接类依赖（如 Dubbo Triple REST 需要的 gson/jackson-jsr310）统一声明在 `buildSrc/scaffold.java-conventions.gradle`。框架平台 `Boot → Cloud → Alibaba → dubbo-bom` 垫底，不要调整。
2. **返回体与异常**：Web 层新代码统一用 `R`（`com.scaffold.common.core.domain.R`；`AjaxResult` 仅为存量兼容保留，不要再扩大使用）；业务异常抛 `ServiceException`，不要自造异常体系。
3. **端点必挂权限**：每个带 `@*Mapping` 的端点方法必须挂 `@RequiresPermissions`（免鉴权的只读/回调端点登记进 ArchTest 白名单）。每个含 `api` 包的服务提供一个 `ArchTestSupport` 子类守住本条与下一条（写法见 [starters/test/.../ArchTestSupport.java](starters/test/src/main/java/com/scaffold/common/test/arch/ArchTestSupport.java) javadoc）。
4. **SQL 注入面**：ORM 查询一律走参数绑定（JpaSpecs 条件或 `@Query` 的 `:param`），禁止字符串拼接 SQL；原生 `nativeQuery` 出现方言专有构造会被 `ArchTest` 门禁直接红。
5. **前缀即命名空间**：Redis key 用 `scaffold:` 前缀、Spring 配置用 `scaffold.` 前缀、trace attachment key 用 `scaffold_trace_id`。新项目由脚本统一替换，不要手工发明前缀。
6. **Flyway 只追加**：已合并的 `V*.sql` 永不修改（改历史迁移 = 破坏所有已部署环境）；新迁移递增版本号；每个方言家族一份迁移（`db/migration/<家族>/`）与 h2 基线（`db/migration/h2/`）**成对维护**，本地默认 H2 零安装可跑；家族基线由 `scripts/gen_dialect_migrations.py` 从 h2 基线生成（生成文件带 GENERATED-BY 标记勿手改，h2 演进后 `--force` 重新生成）；家族归并与信创库借道策略见 [docs/multi-database-guide.md](docs/multi-database-guide.md)。
7. **改名走脚本**：新项目 / 全局改名用 `scripts/new-project.ps1`（或 `.sh`），占位符规则见 README §2；禁止手工 `com.scaffold` → 别的名字。
8. **工具类先查再写**：`common/core` 已有雪花 ID、TTL 线程池、SM3 签名、JwtUtils 等，`common/redis` 有 RedisService 门面，starter 有两级缓存+分布式锁（cache）、幂等（idempotent）、读写分离（rw）、区划/IP 归属地（area）。**写任何工具类 / 中间件胶水前，先对着 README §1 的模块清单找一遍**，重复造轮子 MR 会被打回。
9. **日志**：traceId 全链路贯通（MDC，logback 模板已配好，别改 pattern）；操作/追踪日志用 `@Log` / `@TraceLog`；禁止 `System.out` / `e.printStackTrace()`。
10. **中间件胶水用 starter**：分布式锁、缓存、幂等、读写分离一致性一律用对应 starter，不要手写 SETNX / 手动开窗。
11. **持久层为全工程统一的 Spring Data JPA**：数据访问接口放 `repository/` 包并继承 `ScaffoldRepository<T, ID>`（CRUD + Specification 动态条件，参照 `SysConfigRepository`）；service 继承 `ScaffoldServiceImpl`（requireById/page/dataScope/乐观锁翻译由基类统一）；动态条件用 `JpaSpecs` 组装（eqIf/eqIfNotBlank/likeIf/inIf/dateRangeIf/betweenIf 等，大小写不敏感 like 与转义已对齐原 mapper XML 语义）；实体审计列继承 `BaseEntity`/带版本表继承 `VersionedEntity`（`@EnableJpaAuditing` 由 common/core 的 `JpaAuditingAutoConfiguration` 自动装配）。MapStruct 转换器一律放 `convert` 包且**写手写实现类**（`@Component` 或静态工具类，参照 `SysUserConvert`）——本仓库未接入 `mapstruct-processor`，`org.mapstruct.Mapper` 接口不会有实现类。
12. **多数据库（db2/dm/highgo/kingbase/mysql/opengauss/oracle/postgresql/sqlserver）**：9 库按方言家族归并为 6 个方言（oracle 含达梦、postgresql 含 openGauss/瀚高/金仓 PG 模式），完整映射、驱动与 Flyway 模块对应关系见 **[docs/multi-database-guide.md](docs/multi-database-guide.md)**。要点：① 业务查询由 Hibernate 方言接管跨库（DDL 照旧走 Flyway 多家族基线）；`@Query(nativeQuery=true)` 的原生 SQL 出现方言专有构造（SYSDATE/TO_CHAR/||…）会被 `ArchTestSupport` 门禁直接红，确需方言差异的目录/元数据查询走"JdbcTemplate/JDBC 元数据 + `DatabaseDialects.resolve(DataSource)` 服务层分发 + 方言分支单测"模式（参照 `GenCatalogDao`，方言识别工具在 `com.scaffold.common.core.jpa.DatabaseDialects`，H2/未识别库走 JDBC 元数据兜底）；② top-N 用 Spring Data `Pageable`（方言 LIMIT 由 Hibernate 渲染）；③ 驱动与 `flyway-database-*` 模块版本走 BOM，服务按目标库以 runtime scope 引入。

## 3. 测试要求

- 新服务/模块必须有：Flyway 基线 + H2 repository 切片测试（`starters/test` 的 `@H2JpaTest`，写法见其 javadoc：`ddl` + `entityPackages`，ddl 可直接复用 `db/migration/h2` 基线）。
- 测试不依赖本机真实中间件：Dubbo 用 starter-test 的直连运行时/字段 mock，数据库用 H2（Oracle 模式）。
- **测试要"杀得掉变异体"**：合并前对改动包跑一次 PIT（命令见下）；存活变异体集中的类，要么补断言，要么在 MR 里说明为什么不需要。AI 生成的测试尤其要过这一关——"跑过"不等于"测了"。
- 修复 bug 先写复现测试再修；不要为过编译注释/`@Ignore` 掉测试，修不动就留 TODO + 原因说明。

## 4. 常用命令

```bash
./gradlew :<模块>:test                     # 单模块测试（提交前最低要求；Gradle 依赖图自动带上游）
./gradlew compileJava test                 # 全仓编译+测试（动了 common/starters/bom 必须跑）
./gradlew checkstyleMain pmdMain           # 静态检查报告（Checkstyle + PMD-P3C，规则集在 build-tools/）
python scripts/parse_pit_reports.py        # 汇总存活变异体（PIT 报告需先有；见下条）
scripts/new-project.ps1 -GroupId com.yourco -Name yourproj -Dir E:\workspace\yourproj   # 生成新项目
```

构建系统为 Gradle 9（Groovy DSL，2026-10 由 Maven 迁移）：一律用 `./gradlew`（wrapper 锁定 9.4.1），不要用系统 gradle。构建定义：`settings.gradle`（模块清单）+ `gradle.properties`（版本收口）+ `buildSrc/`（公共约定：Java 17/UTF-8/-parameters、BOM 平台注入、Checkstyle+PMD 报告模式）。PIT 变异测试：Maven 版插件已随迁移移除，Gradle 版（info.solidsoft.pitest）待引入；引入前改动包在本地以旧 Maven 侧回退执行并贴报告。DUBBO 契约生成：api 三模块的 Triple 桩为签入的生成物（`src/generated/java`，dubbo-maven-plugin 无 Gradle 等价物），改 `.proto` 后重生成方式见 scripts/README.md。

## 5. 质量门禁：CI 是唯一裁判

- `.gitlab-ci.yml`：build → test 与 verify(`-Psca`) 并行；**CI 绿是可合并的唯一标准**，本地环境（人肉或 AI）不作为豁免理由。
- 静态检查当前为**报告模式**（出报告不拦截）；转硬门禁的路径：先调规则集与存量代码的冲突（如 Logger 字段名 `log`、默认值魔法数字），设 `maxAllowedViolations` 基线只拦增量，再换 `checkstyle:check` / `pmd:check`。在那之前，verify 报告里**新增**的违例应在 MR 里清零。
- MR 自查清单：① 端点挂权限了吗 ② 新增依赖进 BOM 了吗 ③ Flyway 只追加了吗 ④ 有测试且 PIT 存活可解释吗 ⑤ 改了 `bom/common/starters/build-tools` 底座吗（需要底座 Owner 过目）。

## 6. AI 助手补充纪律

- **先读后写**：动手前先读 README §1、本文件 §1/§2；不要凭训练记忆假设本工程的结构。
- **不引白名外部依赖**：任何 import 的新库先查 BOM；AI 常见幻觉是随手引入未收口的包，CI 会查，但请自查在先。
- **不发明约定**：前缀、目录、命名一律按本文件；示例代码里的风格差异（如网上 RuoYi 老代码）不要照搬。
- **遵循 [.editorconfig](.editorconfig)**（4 空格缩进、LF、文件末尾换行）；格式化交给工具，不要手调。
- **改动最小化**：不顺手重构无关代码、不"顺手升级"依赖版本；发现该修的问题单独提 MR。
- 生成的代码若与存量风格冲突，**以存量为准**（如既有服务的包结构、Resource/Impl 分层）。
