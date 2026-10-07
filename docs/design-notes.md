# 脚手架抽取决策记录（ADR）

记录从 ch_tourist 抽取本脚手架时的取舍与理由，避免后来者重新考古。

## 1. 搬了什么（按层）

| 层 | 内容 | 备注 |
|---|---|---|
| BOM | ct-bom → scaffold-bom，含"项目 BOM 最前 + dubbo-bom 垫底 + exclusion 收口"分层模式 | 剔除业务模块条目与 Drools |
| common | 10 个通用库全量 | 见 §2 改造点 |
| starters | 7 个 starter 全量 | flow 见 §2 |
| services | auth/gateway/system/file/job/gen 六件套 | system 只保留 RBAC 平台能力 |
| 契约 | system-api / file-api / job-api | audit-api 不搬，见 §2 |
| 工程实践 | Flyway 按模块迁移、DBA 引导四步法、docs 文档纪律、单测四层方案（L1 Mockito/L2 H2/L3 真实库/L4 Testcontainers） | 方法论随文档沉淀 |

## 2. 关键改造点（与原型的差异）

1. **审计落库 SPI 化**：原型的 `@Log/@TraceLog` 与登录日志经 Dubbo 写独立的 ct-audit 服务。脚手架不带审计服务，改为 `common-log` 内 `LogPersister` SPI + SLF4J 默认实现；接入方注册自己的 `LogPersister` Bean 即恢复持久化（可实现为 Dubbo/HTTP 投递到自有审计服务）。`TraceLogEntry` 模型随之迁入 common-log。
2. **JWT 密钥外置**：原型硬编码于 `TokenConstants.SECRET`（已在原型中修复），脚手架沿用 `jwt.secret` 系统属性 / `JWT_SECRET` 环境变量方案。
3. **等保三员双因子配置化**：原型硬编码 `admin` + `aud_/sec_/sys_` 前缀强制 OTP；脚手架改为 `scaffold.auth.two-factor.*` 配置（默认 enabled=false，required-roles=[admin]），等保项目按需打开。
4. **国密信封门控**：登录密码 SM2 传输、SM2+SM4 信封过滤器依赖自有国密实现（common/core crypto 包，BouncyCastle 底座，2026-10 由 zdhr-crypto 3.0.3 迁出）与 BCFKS keystore（`GmIdentity`，starters/gm 自动装配，`gm.keystore.format` 默认 BCFKS）；信封过滤器默认关闭（`scaffold.auth.gm-envelope.enabled`）。注意登录链路的 SM2 解密仍随 auth 服务，不需要国密的项目不引入 scaffold-spring-boot-starter-gm 即可。
5. **工作流定义外置**：flow starter 不再内置任何业务流程，改为 `WorkflowDefinitionContributor` SPI 由接入方注册；契约测试（native/Warm-Flow 双引擎同套用例）保留。
6. **消息 Topics 剥离**：业务 topic 常量类不搬，`common-mq` 只保留信封/重试/DLQ/幂等框架。
7. **system 只留平台**：V2 以后业务菜单迁移全部剔除；审计中心菜单（2000 段）与三员角色作为等保可选能力保留在 V1（前端不接审计时菜单不可点，无功能影响）。
8. **job 剥离业务任务**：客流密度任务、日志归档任务及其归档双数据源配置删除；保留 Quartz 管理骨架 + RyTask 作为 demo 模板。
9. **Drools 剔除**：规则引擎属原项目情指行域，BOM 条目一并移除（samples 的 brms 方法论可按需参考原型仓库）。
10. **monitor/idempotent/gateway-sign 不再隐式传递**：原 common-security pom 传递引入三个 starter（过渡兼容），脚手架改为按服务显式引入，依赖更干净。
11. **配置键全量更名**：`ct.*` → `scaffold.*`、`ct:` → `scaffold:`、`ct_trace_id` → `scaffold_trace_id`；从原型 Nacos 迁移配置时注意同步改键。
12. **审计只读数据源（等保三员分立）**：`AuditReadOnlyFilter` 保留但配置门控（`scaffold.audit.readonly-ds` 未配置即整体关闭）。

## 3. 明确不搬的

- 业务域 common（data/meta/gov/police/collector）与 20+ 业务服务（指令/预警/情报/大屏/采集器/聚合服务等）
- V2+ 业务菜单/角色迁移、业务 Nacos 种子
- docs 的情指行/P-IRS 设计文档族、samples POC 代码、AI 改造脚本（.zcode/patch_*.py）、若依遗留启动脚本

## 4. 已知遗留事项

1. **本机 Maven 仓库污染**：`D:\jars-repo` 中 `org/springframework/boot/spring-boot-autoconfigure/3.5.16` 的 jar 疑似被 Boot 4 实验构建覆盖（`@AutoConfiguration` 缺失 `proxyBeanMethods` 属性）。脚手架代码已按兼容写法规避，但建议清理该 jar 并重新下载，避免其他工程踩坑。
2. **本地 yml 内网默认值**：Nacos/Sentinel 默认地址仍指向原型环境内网 IP（均可用 `${env:...}` 覆盖）；auth 的 application-local.yml 含 SM2 开发私钥（仅限本地联调），发布模板前请更换。
3. **admin 初始密码**为原型基线 bcrypt，首次登录必须修改；建议各项目生成随机密码重置。
4. **Drools/规则沙箱、双流程引擎共存**等调研成果留在原型 samples/，脚手架的 flow starter 只保留单引擎门面。
5. **等保增强表**（SYS_ANON_LOG、密码历史、第三方身份绑定）随 V1 建表保留，不用即无副作用。

## 5. 运行验证记录（H2 模式，2026-09）

本脚手架已在本机完整跑通并验证：
- 平台 7 服务全部启动：gateway(28080) / auth(20880) / system(20881) / file(20884) / job / gen / sample-app(9210)
- 数据库：H2 MODE=Oracle（文件 ./data/h2/*），H2 为唯一数据库
- 登录 E2E：验证码 → SM2 加密密码 → JWT 签发 → 网关鉴权 → sample 接口（@RequiresPermissions 生效）
- 工作流 E2E：订单提交 → 审批驳回（相邻退回）→ 流转历史 5 条
- 定时任务：库存预警（分布式锁防重）按 60s 周期执行

顺带发现并修复了抽取过程中的深层次问题（详见第 2 节），追加两条原型问题：
1. **TokenResource 登录接口缺 @RequestBody**：Triple REST 对未注解 POJO 按 query 参数绑定，
   前端 JSON body 传参会导致密码为空（"SM2 cipher is empty"）——脚手架已补注解。
2. **Dubbo 应用注册名与网关路由名不一致**：服务注册为 `xxx-service`（dubbo.application.name），
   网关路由 `lb://xxx`，无 Web 容器服务没有 SC 注册时路由 404——脚手架统一去掉 `-service` 后缀，
   并为样例类纯 Web 服务关闭 Dubbo 注册（`dubbo.registry.register: false`）避免双实例。
3. **common-core 的 SpringUtils/异步线程池/雪花 ID 未注册自动配置**（imports 文件只有 JwtUtils），
   system 服务启动即 NPE——脚手架已补全 imports。
4. **job 服务缺 SYS_JOB/SYS_JOB_LOG 建表脚本**（DDL 误放于 system 基线 V1）——脚手架以 V3 迁移收正。
5. **cache starter 锁装配时序**：LockAutoConfiguration 需 @AutoConfigureAfter(RedisAutoConfiguration)，
   否则 @ConditionalOnSingleCandidate(StringRedisTemplate) 恒假。
6. **H2 模式下 PageHelper 需显式 `pagehelper.helper-dialect: oracle`**（自动探测误判 mysql/LIMIT，
   H2 MODE=Oracle 不支持 LIMIT 语法）。

## 6. H2 运行模式的边界

- Flyway 迁移脚本统一放在 db/migration/h2（H2 MODE=Oracle 方言）。
- gen「导入表」依赖数据库原生字典（user_tables），H2 演示模式不可用；单表案例以预置元数据
  （V1__init.sql 尾部种子节）提供，后台可直接预览/下载生成代码。
- Nacos 仅作注册中心使用（local profile 关闭配置中心）；元数据上报到共享 Nacos 可能出现
  写超时告警（Dubbo 异步重试），不影响功能。

## 7. 国密底座：zdhr-crypto 3.0.3 → 本仓自有实现（BouncyCastle，2026-10）

> 上一轮（2026-09）曾将底座切到 zdhr-crypto 3.0.3（kona 引擎），2026-10 起
> **zdhr-crypto 依赖已全部移除**，以下 3.0.3 升级记录仅作历史存档，现状以本节为准。

- **实现位置**：`common/core` `com.scaffold.common.core.crypto` 包（Sm2Engine/Sm2Keys/
  Sm3Digester/HmacSm3/Hexs/FieldCryptoHolder，全走 BC 轻量级 API，不注册全局 Provider）；
  口令哈希在 `common/security` Sm3PasswordEncoder；keystore/字段加密在 `starters/gm`
  （GmIdentity/GmFieldCrypto/FileSymmetricKeyProvider，`scaffold.gm.enabled` 开关自动装配）。
- **格式兼容**：密文 raw C1C3C2、签名 SM3withSM2 DER、`$SM4$` 版本化字段密文、
  `salt$hash` 口令格式与 zdhr-crypto 3.0 逐字节兼容，存量数据零迁移。
- **BOM**：删除 zdhr 四条目与版本属性，新增 `bcprov-jdk18on:1.81`；服务模块改引
  `scaffold-spring-boot-starter-gm`。
- **keystore 注意**：PKCS12 的私钥袋封装存在跨 provider 兼容问题（Kona 写出的 PBES2 变体
  BC 解不开，报 pad block corrupted），2026-10 起 keystore 统一 BCFKS（BC 专有格式，
  `gm.keystore.format` 默认 BCFKS）。存量 p12 用 starters/gm 测试源里的
  `GmKeystoreConvertTool` 一次性转换（`java -cp bcprov:tests … <in> <out> <storePass> <keyPass> <alias>`，
  默认输出 BCFKS；密钥材料不变）；dev 密钥库随仓库 `gm/gm.bcfks`（相对各服务工作目录）。
- **历史：3.0 升级（2026-09，已废止）**

底座切换到公司源工程 `E:/workspace/zdhr-crypto-parent`（3.0.x，底层腾讯 KonaSMSuite，
默认发行版无 BouncyCastle）。

- **BOM**：`zdhr-crypto.version` 2.6.1 → 3.0.3；新增 `zdhr-crypto-spring-boot-starter`、
  `zdhr-crypto-engine-kona` 管理条目；移除未使用的 `zdhr-crypto-mybatis`。
- **common-core**：依赖 `zdhr-crypto-core` + `zdhr-crypto-engine-kona(runtime)`——
  HmacSm3/Hexs/Sm2Engine/FieldCryptoHolder 全服务运行时使用（网关签名、JWT 验签），
  3.0 core 不含引擎，ServiceLoader 加载 kona 引擎。
- **auth**：依赖 `zdhr-crypto-spring-boot-starter` + `engine-kona(runtime)`；
  `@EnableSmCrypto`/`GmIdentity` 用法不变（decryptSm2/getActiveAlias/getActivePublicKeyHex）。
- **keystore**：3.0 仅支持 PKCS12（BCFKS 不可读）。dev 密钥对随仓库
  `keystore/gm.p12`（源自 zdhr-crypto-parent/test-keys，alias=202607-h1，
  StorePass123/KeyPass123，仅限本地联调）；生产按密评要求走环境变量/配置中心。
- **代码适配**：`GmEnvelopeFilter` 移除 `GmProviderHolder.ensure()`（3.0 删除，
  Kona provider 自动注册）。
- **本地仓库补充**：engine-kona 未发布到 Nexus，已从源工程 build/libs 手工安装到
  D:/jars-repo（POM 含 kona-crypto/kona-pkix 1.0.24 传递依赖，构件已从 Central 拉取）。
- **回归**：登录 E2E（SM2 密码解密/JWT 签发验签）、样例接口、工作流全链路通过；
  gm-key 公钥由 kona PKCS12 keystore 供给。

注意：gm.p12（test-keys 版）与原型旧 gm.bcfks 并非同一对密钥，公钥已变化；
前端无影响（公钥由 /auth/gm-key 运行时下发），但外部若缓存了旧公钥需更新。
