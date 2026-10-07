# scaffold 微服务脚手架

从 ch_tourist（旅游警务平台）提炼的**业务无关**微服务脚手架，为后续新项目提供开箱即用的技术底座。

> 技术栈：Java 17 · Spring Boot 3.5.x · Spring Cloud 2025.x · Spring Cloud Alibaba（Nacos）· Dubbo 3.3 Triple（protobuf IDL）· Spring Data JPA · Flyway · Redis · Kafka · Vue3 前端另见 ct-ui 同款技术栈（本仓库不含前端）。

## 1. 工程结构

```
scaffold-sample/
├── settings.gradle          模块清单（扁平 include，路径 = 原 artifactId）
├── build.gradle             根脚本（BOM 平台注入）
├── buildSrc/                公共构建约定（Java 17 / BOM / Checkstyle+PMD 报告模式）
├── bom/                     scaffold-bom：内部构件 + 第三方版本收口，冲突 exclusion 集中在此层
├── api/                     RPC 契约（Dubbo Triple protobuf，供消费方以 jar 内部调用，不暴露给网关）
│   ├── system-api           用户/角色/字典/登录模型 + Remote* 契约
│   ├── file-api             文件 RPC 契约（proto）
│   ├── job-api              任务执行日志回写契约（纯 Java 接口）
│   ├── audit-api            审计日志 proto 契约（操作/登录/查询/采集留痕，仅追加防篡改）
│   └── message-api          消息中心发送契约（短信/邮件/站内信，纯 Java 接口）
├── common/                  业务无关公共库
│   ├── core                 统一返回体 R/AjaxResult、异常体系、工具类、雪花 ID、TTL 线程池、SM3 签名、JwtUtils、JPA 支撑（ScaffoldRepository/ScaffoldService/JpaSpecs/审计装配）
│   ├── redis                RedisTemplate 装配 + RedisService 门面
│   ├── sensitive            @Sensitive Jackson 序列化期脱敏
│   ├── trace                traceId 全链路（MDC + Dubbo attachment，key: scaffold_trace_id）
│   ├── mq                   Kafka 消息信封（CloudEvents 风格）+ 退避重试 + DLQ + Redis 幂等
│   ├── log                  @Log/@TraceLog 注解 + LogPersister SPI（默认 SLF4J 落地，接入方可覆盖为远程审计）
│   ├── security             自研 JWT+Redis 会话、@RequiresPermissions/@InnerAuth、HTTP/Dubbo 身份透传
│   ├── datascope            @DataScope 数据权限（SQL 标识符白名单防注入）
│   └── datasource           Druid + dynamic-datasource 多数据源（审计只读为可选等保扩展）
├── starters/                插拔式能力 starter：一个 starter = 一个能力域，引入即生效、配置可拔
│   ├── cache                两级缓存（Caffeine+Redis 广播失效）+ 无 Redisson 分布式锁
│   ├── idempotent           @Idempotent 防重复提交（TOKEN/PARAM/PATH）
│   ├── monitor              Sentinel+Actuator+Prometheus 依赖聚合
│   ├── rw                   读写分离写后读一致性窗口（afterCommit 开窗）
│   ├── test                 Dubbo 直连运行时 / @DubboReference 字段 mock / H2(Oracle 模式) mapper 切片测试 / ArchTestSupport 安全架构自检
│   ├── gateway-sign         网关身份头 HMAC-SM3 验签 + nonce 防重放（下游侧）
│   ├── flow                 工作流门面 SPI（native 内存 / Warm-Flow 可插拔）+ 契约测试基类
│   ├── job                  @ScaffoldJob 定时任务接入：分布式锁防重（多实例单节点执行）+ 执行日志回写任务中心
│   └── area                 行政区划树（area.csv）+ IP 离线归属地（ip2region）
├── build-tools/             构建工具资源包：checkstyle / PMD(P3C) 规则集，供 -Psca 质量检查以 classpath 引用
└── services/                平台服务模板（Flyway 迁移在各服务 resources/db/migration/h2/）
    ├── auth                 认证：登录/登出/刷新/踢人/多端管理、验证码、双因子（配置化、默认关）
    ├── gateway              网关：鉴权+白名单、XSS、TraceId、SM3 签发、验证码、Sentinel、metadata 动态路由
    ├── system               系统管理：用户/角色/菜单/部门/岗位/字典/参数/通知/在线用户/个人中心
    ├── file                 文件：本地 + MinIO 双实现
    ├── job                  任务：Quartz 任务中心 + 执行日志回写 API
    ├── gen                  代码生成：Velocity 模板（Java/Mapper/XML/SQL/Vue2/Vue3+TS）
    ├── audit                审计服务（等保 8.1.4，独立部署）：操作/登录/查询/采集留痕 + 留痕哈希链防篡改巡检
    └── message              消息中心：短信/邮件/站内信（模板/渠道/账号/日志 + 统一发送契约）
```

工程化约定：Gradle Wrapper（`./gradlew`，锁定 9.4.1）、`.editorconfig` 统一格式；静态检查报告用 `./gradlew checkstyleMain pmdMain`
（Checkstyle + PMD-P3C，规则集在 `build-tools/`，日常构建不启用，CI verify 阶段启用，见 `.gitlab-ci.yml`）；
PIT 变异测试：Gradle 版插件待引入（迁移期间在旧 Maven 侧按需执行），配 `scripts/parse_pit_reports.py` 汇总存活变异体。

协作约定（人 / AI 助手通用）以 **[AGENTS.md](AGENTS.md)** 为唯一事实源：模块归属决策、硬约束（依赖收口/权限注解/Flyway 只追加/前缀命名空间等）、
测试要求与 MR 自查清单都在里面；各 AI 工具的指令文件（`CLAUDE.md`、`.cursor/rules/`、`.github/copilot-instructions.md`）仅指向它。

## 2. 命名占位符（新项目改名规则）

脚手架自身使用中性命名，新项目通过 `scripts/new-project.ps1`（或 `.sh`）一键替换：

| 占位符 | 含义 | 新项目示例 |
|---|---|---|
| `com.scaffold` / `com/scaffold` | Java 包名 / 包路径 | `com.yourco.yourproj` |
| `scaffold-` | Maven 构件名、服务名（scaffold-auth → yourproj-auth） | `yourproj-` |
| `scaffold.` / `scaffold:` | Spring 配置前缀 / Redis key 前缀 | `yourproj.` / `yourproj:` |
| `scaffold_trace_id` | 链路 attachment key | `yourproj_trace_id` |

## 3. 快速开始

前置：JDK 17、Maven 3.9+、Nacos（注册中心）、Redis；**数据库使用 H2 零安装运行**。

```powershell
# 1) 生成新项目（推荐）
scripts/new-project.ps1 -GroupId com.yourco -Name yourproj -Dir E:\workspace\yourproj

# 2) 或直接构建本脚手架
./gradlew build -x test
```

### 本地一键运行（H2 模式，已验证）

```powershell
# 先起本地 Redis（127.0.0.1:6379），再依次：
./gradlew build -x test
# 每个服务默认 local profile 启动（H2 数据库 + Flyway 自动建表，无需外部数据库）：
$env:JWT_SECRET='dev-secret'
java -jar services/system/target/scaffold-system.jar
java -jar services/auth/target/scaffold-auth.jar
java -jar services/gateway/target/scaffold-gateway.jar
java -jar samples/sample-app/target/scaffold-sample-app.jar   # 业务样例（可选）
# file / job / gen 同理（可选）
```

前端：`ui/` 目录（Vue3 + Vite + Element Plus），`npm install && npm run dev`，代理已指向本地网关；
浏览器打开 http://localhost/ → 验证码 + 账号密码登录（SM2 加密传输）。

### 样例模块（samples/sample-app）

覆盖脚手架全部常用能力的标准业务形态（与 gen 生成物同构）：
单表 CRUD（客户/库存）· 树表（商品分类）· 主子表（订单+明细）· 工作流审批（flow starter，
提交→审批→办结/相邻退回）· 业务定时任务（分布式锁防重）· @Sensitive 脱敏 · @Log 操作日志 ·
数据字典 · 网关 metadata 动态路由（`gateway-path: sample`，零网关配置）。
登录后台「样例演示」菜单即可体验。

### gen 代码生成案例

模板支持**单表（crud）/ 树表（tree）/ 主子表（sub）**三类（vm/java、vm/vue/v3、index-tree 等）。
内置案例：SAMPLE_CUSTOMER 单表元数据种子（gen 服务启动自动载入，后台「代码生成」可直接
预览/下载生成代码）；树表/主子表参照 samples/sample-app 同构代码。注意：导入表功能依赖
Oracle 方言（user_tables），H2 演示模式下请使用预置元数据。

- 各服务本地配置在 `services/*/src/main/resources/*-local.yml`（Nacos/Redis/DB 地址均支持 `${env:...}` 覆盖）。
- **JWT 密钥**：`JwtUtils` 从系统属性 `jwt.secret` 或环境变量 `JWT_SECRET` 读取，生产必须注入。
- **默认账号**：system 服务初始 admin 账号密码沿用原型基线（bcrypt 种子见 `V1__init_system.sql`），**首次登录必须修改**。
- **双因子/国密信封/审计只读库**均为可选合规能力，默认关闭，见 `docs/design-notes.md`。

## 4. 构建注意（Windows 中文环境）

- Gradle 构建编码统一 UTF-8（`buildSrc` 约定中 `options.encoding`），与原 `.mvn/jvm.config` 目的相同。
- **工程目录路径请保持纯 ASCII**：目录名含中文或特殊字符（含不可见字符）时，protoc 等原生工具链会因参数编码失败。若已踩坑，可用 `subst X: <项目绝对路径>` 后从 `X:\` 构建。

## 5. 文档

- [docs/new-module-guide.md](docs/new-module-guide.md) —— 新业务模块 5 步接入（骨架 → 配置 → 网关动态路由 → API 契约 → 菜单权限 SQL）
- [docs/design-notes.md](docs/design-notes.md) —— 抽取决策记录：哪些没搬、为什么；可选合规能力开关；遗留事项
- 各服务接口契约：以 `api/` 下 protobuf IDL 为单一事实源

## 6. 核心架构约定（继承自原型）

1. **网关只直连聚合服务 / 管理后台业务服务**；领域基础数据服务只能被业务域服务通过 API 包（jar）内部调用，不经网关。
2. 校验（角色/数据权限/业务规则/审计）收敛在业务域服务一层；基础数据服务只保证读写一致性。
3. 新服务在 Nacos 注册 metadata 里声明 `gateway-path` 即自动生成网关路由，零网关改动（`scaffold.gateway.dynamic-routes.enabled=true`）。
