# 中间件安全部署与合规指南（信创 / 等保 / 密评）

> 适用范围：ch_tourist 项目实际使用到的中间件 —— **Nacos、Oracle（业务库）、MySQL（Nacos 配置库）、Redis、Kafka、MinIO、Sentinel、Nginx、Quartz（内嵌）**。
> 目标：① 介绍各中间件是干什么的、本项目怎么部署的；② 给出生产环境标准部署要求；③ 对照 **信创、等保 2.0（三级）、商用密码应用安全性评估（密评）** 说明还需要补齐哪些安全措施。

> **2026-09-10 更新**：项目业务库已统一为 **Oracle**（规划向**达梦 DM8** 迁移），MySQL 仅保留 **Nacos 配置库**角色，数据库相关章节已按此口径修订；Nacos 鉴权未整改（`nacos.core.auth.enabled=false`、默认 token 密钥）的 ⚠ 现状经复核**仍然成立**，相关警告保留。

---

## 一、中间件总览

| 中间件 | 角色 | 本项目用途 | 传输端口 |
|---|---|---|---|
| Nacos | 注册中心 + 配置中心 | 服务注册发现、配置管理 | 8848 / 9848(gRPC) / 7848(raft) |
| Oracle | 关系型数据库（业务库） | 各业务服务主数据存储，规划迁移达梦 DM8 | 1521 |
| MySQL | 关系型数据库 | Nacos 配置库 | 3306 |
| Redis | 缓存 / 会话 | 登录令牌、验证码、热点缓存 | 6379 |
| Kafka | 消息队列 | 业务异步消息、日志流 | 9092 |
| MinIO | 对象存储 | 文件服务(ct-file)的上传存储 | 9000 / 9001(控制台) |
| Sentinel | 流控熔断 | 网关与服务限流、熔断降级 | 8719（client）/ 控制台按需 |
| Nginx | 反向代理 | 前端静态资源 + 网关入口 | 80/443 |
| Quartz | 定时任务（内嵌于 ct-job） | 定时任务调度，无独立进程 | — |

当前部署形态（见 `docker/docker-compose.yml`）：业务服务容器化，**Nacos/MySQL/Redis/MinIO 运行在宿主机或外部 K8S**，通过 Nacos 配置中的外部地址连接。

---

## 二、各中间件简介与安全部署

### 1. Nacos

**简介**：阿里开源的服务注册发现 + 动态配置中心。所有微服务的地址和配置都从 Nacos 获取，因此它是**整个系统的单点入口资产，泄露配置等于泄露全部数据库口令**，是本项目最需要加固的中间件。

**部署要求（生产）**：
- 至少 3 节点集群 + 外置 MySQL（推荐主从），raft 端口 7848 需互通。
- 仅监听内网；控制台不对互联网暴露。如需远程管理，走 VPN 或堡垒机。
- 镜像建议固定国产化替代可用版本（Nacos 2.x 已通过信创兼容性认证环境大量落地；如需严格信创栈可评估 Nacos + 东方通/TongHSDB 适配或直接使用 K8S 原生方案）。

**安全加固清单（当前仓库存在的问题已标注 ⚠）**：

| 项目 | 要求 | 现状 |
|---|---|---|
| 鉴权开关 | `nacos.core.auth.enabled=true` | ⚠ `docker/nacos/conf/application.properties` 中为 `false`，任何人可读写全部配置 |
| token 密钥 | ≥32 字节随机值，禁止使用官方示例值 | ⚠ 当前为示例 `SecretKey0123...`，属于**已公开的默认密钥**（CVE-2021-29442 类问题），必须更换 |
| server identity | key/value 改为随机字符串 | ⚠ 当前 `serverIdentity/security` 为默认值，可被伪造绕过 |
| 默认口令 | 首次登录即修改 `nacos/nacos`，或初始 SQL 里删除默认用户 | ⚠ `docs/sql/nacos2-config-mysql.sql`、nacos3 脚本含默认用户 |
| ignore.urls | 收敛 `/actuator/**` 等白名单，不要放开健康检查以外路径 | ⚠ 现配置放开过宽 |
| 配置加密 | 敏感配置（DB 口令、AK/SK）使用 Nacos 插件加密或托管到密管系统，避免明文 | 未做 |
| 传输加密 | Nacos 2.x 开启 TLS（gRPC SSL），或至少保证网络层隔离 | 未做 |

生成随机密钥示例：
```bash
openssl rand -base64 32
```
写入 `nacos.core.auth.plugin.nacos.token.secret.key`（所有节点一致）。

### 2. 数据库（Oracle 业务库 / MySQL Nacos 配置库）

**简介**：业务库当前为 **Oracle**，承载各业务服务的主数据，规划向**达梦 DM8** 迁移；MySQL 现阶段仅作为 **Nacos 配置库**使用。

**Oracle（业务库，现状）部署要求（生产）**：
- 独立服务器/实例，仅对应用网段开放 1521；RAC 或 Data Guard 高可用。
- 信创替换目标为**达梦 DM8**（对 Oracle 语法/PL/SQL 兼容度高，迁移成本相对最低），迁移前先跑全量 SQL 与存储过程回归；规划迁移时将下述加固项同步落入达梦侧。

**Oracle（业务库）安全加固**：
- 锁定/删除无用默认账户（SCOTT 等示例账户），应用账号仅授予业务 schema 最小权限，禁止 `DBA` 角色。
- 禁止 `SYS`/`SYSTEM` 远程日常使用，改为专用管理账号 + 审计。
- 密码策略 ≥ 8 位含大小写数字符号（等保身份鉴别要求），90 天更换（密评/等保要求），配置 `FAILED_LOGIN_ATTEMPTS` 防爆破。
- 开启归档模式（ARCHIVELOG）并 RMAN 异地备份；备份加密存储（等保数据备份恢复要求）。
- 传输加密：开启 Native Network Encryption（`SQLNET.ENCRYPTION_SERVER=REQUIRED`）或 TLS（密评传输链路要求）。
- 敏感字段（手机号、证件号）应用层加密/脱敏存储，或使用 TDE（等保密码应用与个人信息保护要求）。

**MySQL（Nacos 配置库）部署要求（生产）**：
- 独立服务器/容器，仅对 Nacos 集群网段开放 3306；主从或 MGR 高可用。
- 如需信创替换，可选达梦 DM8 / 人大金仓 KingbaseES（提供 MySQL 兼容模式，配置库 SQL 简单、迁移成本低）。若继续用 MySQL，建议 8.0.x 且选用信创云平台承载。

**MySQL（Nacos 配置库）安全加固**：
- 删除匿名账户与 `test` 库；所有账户按最小权限分配（应用账号仅 `SELECT/INSERT/UPDATE/DELETE`，禁止 `SUPER`、`FILE`）。
- 禁止 root 远程登录，root 仅 `localhost`。
- 密码策略 ≥ 8 位含大小写数字符号（等保身份鉴别要求），90 天更换（密评/等保要求）。
- 开启 binlog 并异地备份；备份加密存储（等保数据备份恢复要求）。
- 传输加密：服务端开启 `require_secure_transport`，JDBC 串加 `sslMode=REQUIRED`（密评传输链路要求）。

### 3. Redis

**简介**：缓存与会话存储，持有全部在线用户令牌，**未授权访问 = 全量账户接管**（历史上的 Redis 未授权写 crontab/sshkey 漏洞均源于此）。

**部署要求**：
- 仅监听内网 `bind` 内网 IP，禁止 `0.0.0.0`；容器端口不要映射到公网。
- **必须启用密码**（`requirepass` ≥16 位随机），Spring 配置中同步。
- 危险命令改名或禁用：`FLUSHALL/FLUSHDB/CONFIG/KEYS` 等（`rename-command FLUSHALL ""`）。
- 以非 root、低权限用户运行；开启 `protected-mode yes`。
- 传输加密：Redis 6+ 支持 TLS，或使用 Stunnel/ mesh 加密；密评场景建议 TLS。
- 禁用持久化文件可被任意读取的权限（rdb/aof 文件 chmod 600）。

### 4. Kafka

**简介**：分布式消息队列，承载业务异步解耦。

**部署要求**：
- 3 broker 起步，KRaft 或 ZooKeeper 集群；内网部署。
- **认证授权**：开启 SASL（SCRAM-SHA-512）+ TLS；生产/消费按 topic 做 ACL，禁止 `ALL topic Allow`（等保访问控制要求）。
- 管理端口与 broker 端口分离，9092 不对公网。
- 消息中的个人信息字段在生产侧加密后再投递（密评/个保法要求）。
- 保留策略按业务定（默认 7 天），涉及敏感数据的 topic 建议缩短并加密磁盘。

### 5. MinIO

**简介**：S3 兼容对象存储，存 ct-file 上传的图片/附件。

**部署要求**：
- 分布式至少 4 块盘（纠删码）；控制台 9001 仅内网/堡垒机访问。
- **AK/SK 必须更换**，禁止使用 `minioadmin/minioadmin`；为应用分配独立的最小权限用户（只允许目标 bucket）。
- 开启桶策略：禁止匿名读（`none` public），下载走预签名 URL 且设置短有效期（如 5 分钟）。
- 传输启用 TLS（等保/密评传输要求）。
- 开启版本控制 + 服务端加密 SSE（密评存储加密），密钥放 KMS（可用信创密管：三未信安、江南天安、卫士通等）。

### 6. Sentinel

**简介**：限流熔断组件。本项目以**客户端依赖**方式运行（8719 端口与控制台通信）。

**安全加固**：
- Sentinel 控制台如部署，必须加登录认证（默认控制台无强认证），且仅内网访问。
- 生产建议不常驻控制台，规则持久化到 Nacos（本项目已如此），8719 端口用防火墙封禁。
- 配置合理限流阈值属于等保"业务连续性/抗 DoS"控制项的落地证据。

### 7. Nginx

**简介**：前端静态资源 + 网关反向代理，系统唯一对外入口。

**安全加固（等保对外边界要求）**：
- 强制 HTTPS：TLS 1.2/1.3，关闭 SSLv3/TLS1.0/1.1、弱套件；证书用国密 SSL 证书（SM2，密评要求，可用 Wotong/CFCA 等国密 CA，Nginx 需换 Tengine 或 nginx + gmcntls 支持国密双证书）。
- 隐藏版本号 `server_tokens off`。
- 添加安全响应头：`X-Frame-Options`、`X-Content-Type-Options`、CSP、HSTS。
- 请求体限制 `client_max_body_size`、超时设置，配合 Sentinel/WAF 抗 DoS。
- 访问日志留存 ≥ 6 个月（等保日志留存要求），并接入集中日志平台。

### 8. Quartz（ct-job 内嵌）

- 无独立端口，随应用部署。注意任务管理的权限控制（已有系统权限）与任务日志中的敏感信息脱敏。

---

## 三、信创 / 等保 / 密评还需要做什么

### 3.1 信创（信息技术应用创新）

信创的核心是**自主可控的软硬件栈**，按"云-基础软件-应用"三层逐项替换：

| 层 | 要求 | 本项目落地点 |
|---|---|---|
| CPU/服务器 | 鲲鹏/飞腾/海光/龙芯 | 部署环境选型，JDK 用 ARM 兼容版本 |
| 操作系统 | 麒麟 V10 / 统信 UOS | 宿主机与容器基础镜像（dockerfile 的 FROM 换成 openEuler/麒麟镜像） |
| 数据库 | openGauss / 达梦 / 人大金仓 / OceanBase | 业务库按 **Oracle → 达梦 DM8** 路径迁移（达梦对 Oracle 兼容度高），先跑全量 SQL/存储过程回归；MySQL（Nacos 配置库）可走达梦/金仓的 MySQL 兼容模式 |
| 中间件 | 东方通 TongWeb 替换 Tomcat（Spring Boot 内嵌可声明自研内核兼容）、消息可用 TongLINK/Q | Nacos/Redis/Kafka 属开源软件，一般可保留，需出兼容性说明 |
| JDK | 毕昇 JDK / 龙井 Dragonwell（OpenJDK 发行版） | 替换 Oracle JDK |
| 密码 | 国密算法 SM2/SM3/SM4 贯穿应用 | 见密评节 |

**建议动作**：联系目标信创环境（政务云等）获取兼容性认证清单，出一份《适配测试报告》。

### 3.2 等保 2.0（按三级测算）

三级等保十个安全类中，与中间件直接相关的控制点及现状差距：

| 等保控制点 | 要求 | 当前差距 / 待办 |
|---|---|---|
| 身份鉴别 | 双因素登录、口令复杂度与定期更换 | ⚠ Nacos/Redis/MinIO/MySQL/Oracle 默认或弱口令待整改；管理后台建议加 OTP/USBKey 第二因素 |
| 访问控制 | 最小权限、默认账户改名或删除 | ⚠ 所有中间件默认账户（nacos、root、minioadmin）整改；Kafka ACL |
| 安全审计 | 日志集中采集留存 ≥6 个月，防篡改 | ⚠ 待建集中日志（ELK/国产：奇安信神探等），中间件日志接入；时间统一 NTP |
| 入侵防范 | 最小安装、关闭不必要端口、漏洞扫描修复 | ⚠ 收敛端口暴露（nacos 8848、redis 6379 等），上线前漏扫（等保测评要求扫描通过） |
| 恶意代码防范 | 主机防病毒/容器镜像扫描 | 待部署主机 EDR、Clair/Trivy 镜像扫描进 CI |
| 通信加密 | 管理面与数据面传输加密 | ⚠ Nacos TLS、MySQL SSL、Oracle 网络加密、Redis TLS、Kafka SASL+TLS、MinIO/Nginx HTTPS 全部待开 |
| 数据保密性 | 敏感数据存储加密、脱敏展示 | ⚠ DB 敏感字段加密（SM4）、前端手机号/证件脱敏 |
| 数据备份 | 本地+异地备份、定期恢复演练 | Oracle/MySQL/MinIO/Nacos 库定时备份演练 |
| 剩余信息保护 / 个人信息保护 | 账号注销数据清理、隐私政策 | 应用层整改 |

**流程建议**：定级（三级）→ 备案 → 差距测评 → 整改（上面清单）→ 测评机构测评拿证。周期约 2~3 个月。

### 3.3 密评（商用密码应用安全性评估，GM/T 0054）

密评看的是**国密算法的正确使用**，普通 RSA/AES 方案不满足。需逐项改造：

| 层面 | 密评要求 | 本项目改造 |
|---|---|---|
| 密码算法 | SM2（非对称/签名）、SM3（摘要）、SM4（对称） | 应用内 RSA/AES/MD5 全部替换：登录口令哈希用 SM3+盐；HTTPS 换国密证书与国密 TLS（TLS 国密 GMTLS 或 TLCP 协议） |
| 密码产品 | 使用**认证的商用密码产品**：密码机、SSL 网关、签名验签服务器（需商密认证证书编号） | 采购云密码机/签名验签服务器（信创密管厂商），应用通过 API 调用，不得本地软实现密钥（密评硬性要求） |
| 密钥管理 | 密钥全生命周期管理：生成、分发、更换、销毁 | 密钥统一放密码机/KMS；Nacos 中的明文口令改为密文托管；定期换钥机制 |
| 身份鉴别 | 登录过程使用密码技术（国密） | 管理后台登录：USBKey/手机盾 SM2 签名认证，或至少 SM3 口令 + OTP |
| 传输安全 | 重要链路国密加密 | Nginx 国密 SSL 网关对外；内网 Nacos/MySQL/Kafka 链路视测评范围，建议 IPSec/国密 VPN 统一承载 |
| 存储安全 | 重要数据 SM4 存储加密 | DB 敏感字段、MinIO 服务端加密走密码机 |
| 完整性 | 日志、重要配置防篡改 | 日志摘要用 SM3，或日志审计设备自带 |

**流程建议**：密评通常与等保同步做——先做密改方案（可请密评机构前置咨询），采购密码设备，改造后由商用密码应用安全性测评机构出报告。

---

## 四、整改优先级（按风险排序）

1. **立即**：开启 Nacos 鉴权 + 更换示例 token 密钥/serverIdentity（当前配置等于配置中心裸奔，可被任意读取数据库口令）。
2. **立即**：更换所有默认口令（Nacos/MySQL/Redis/MinIO），Redis 设 requirepass，MinIO 换 AK/SK。
3. **上线前**：网络收敛——所有中间件端口仅内网可达，管理入口走堡垒机；Nginx 上 HTTPS。
4. **等保整改期内**：集中日志、双因素、备份演练、漏洞扫描、MySQL SSL/Redis TLS。
5. **密改阶段**：国密证书 + 密码机接入 + 应用算法替换 SM2/3/4。

---

## 五、Nacos application.properties 加固版

对照 `docker/nacos/conf/application.properties` 现状的完整加固版，`<...>` 为需要替换的占位符，改动点用 `# [加固]` 注释标出：

```properties
# ==================== 数据源 ====================
spring.datasource.platform=mysql
db.num=1
# [加固] 使用 SSL 加密到 MySQL 的链路（等保/密评-传输加密），库名按实际
db.url.0=jdbc:mysql://ct-mysql:3306/nacos-config?characterEncoding=utf8&connectTimeout=1000&socketTimeout=3000&autoReconnect=true&useUnicode=true&sslMode=REQUIRED&serverTimezone=Asia/Shanghai
# [加固] 禁止 root：建 nacos 专用账号，仅授予 nacos-config 库的 DML/DDL 权限
db.user=nacos_user
# [加固] ≥16 位随机口令，注入方式建议用环境变量（NACOS_DB_PASSWORD），不要明文进 git
db.password=${NACOS_DB_PASSWORD}

# ==================== 服务清理 ====================
nacos.naming.empty-service.auto-clean=true
nacos.naming.empty-service.auto-clean.initial-delay-ms=50000
nacos.naming.empty-service.auto-clean.period-time-ms=30000

# ==================== 监控 ====================
# [加固] actuator 端点最小化，禁止 * 全量暴露；生产如需监控走独立 management 端口+内网
management.endpoints.web.exposure.include=health,metrics
management.server.port=9849

management.metrics.export.elastic.enabled=false
management.metrics.export.influx.enabled=false

# ==================== 访问日志 ====================
server.tomcat.accesslog.enabled=true
server.tomcat.accesslog.pattern=%h %l %u %t "%r" %s %b %D %{User-Agent}i %{Request-Source}i
server.tomcat.basedir=/home/ct/nacos/tomcat/logs

# ==================== 鉴权白名单 ====================
# [加固] 收敛白名单：去掉 /actuator/**（actuator 已限端口），仅保留控制台静态资源与认证/健康检查
nacos.security.ignore.urls=/,/error,/**/*.css,/**/*.js,/**/*.html,/**/*.map,/**/*.svg,/**/*.png,/**/*.ico,/console-ui/public/**,/v1/auth/**,/v1/console/health/**,/v1/console/server/**

# ==================== 核心鉴权 ====================
nacos.core.auth.system.type=nacos
# [加固] 开启鉴权（当前为 false，配置中心裸奔）
nacos.core.auth.enabled=true
# [加固] token 有效期从 5 小时收紧到 1 小时
nacos.core.auth.default.token.expire.seconds=3600
# [加固] 替换官方示例密钥（示例值已被公开利用），所有节点一致，Base64 编码的 ≥32 字节随机值
# 生成：openssl rand -base64 32
nacos.core.auth.default.token.secret.key=<替换为openssl rand -base64 32生成的随机值>
nacos.core.auth.plugin.nacos.token.secret.key=<同上，与上面一致>
nacos.core.auth.caching.enabled=true
nacos.core.auth.enable.userAgentAuthWhite=false
# [加固] server identity 改为随机值（默认 serverIdentity/security 可被伪造绕过鉴权，CVE-2024-22102 利用链一环）
# key/value 均自定义，所有节点一致，仅用于节点间身份校验
nacos.core.auth.server.identity.key=<自定义随机key如x9f2Kd>
nacos.core.auth.server.identity.value=<自定义随机value，openssl rand -hex 16>

nacos.istio.mcp.server.enabled=false

# ==================== 可选：gRPC TLS（Nacos 2.2+，密评建议） ====================
# nacos.remote.server.rpc.tls.enable=true
# nacos.remote.server.rpc.tls.cert.server-file=../conf/tls/server.pem
# nacos.remote.server.rpc.tls.cert.server-key-file=../conf/tls/server.key
# nacos.remote.server.rpc.tls.trust-collection-cert-file=../conf/tls/trust.pem
# nacos.remote.server.rpc.tls.mutual.auth.enable=true
```

**配套动作（配置改完必须做的）**：

1. 初始 SQL（`docs/sql/nacos2-config-mysql.sql` / nacos3）里的 `nacos/nacos` 默认用户，首次登录后立即改密或删除，按 `roles` 表分配只读/读写分离账号。
2. MySQL 侧执行：
   ```sql
   CREATE USER 'nacos_user'@'%' IDENTIFIED BY '<强口令>';
   GRANT SELECT,INSERT,UPDATE,DELETE,CREATE,DROP,ALTER ON nacos-config.* TO 'nacos_user'@'%';
   -- root 禁止远程
   DELETE FROM mysql.user WHERE user='root' AND host NOT IN ('localhost','127.0.0.1');
   FLUSH PRIVILEGES;
   ```
3. 敏感值（db.password、token.secret.key、identity.value）通过环境变量或启动参数注入，docker-compose 里用 `env_file` 或编排平台的密钥管理，**不进 git 仓库**。
4. 网络层：8848/9848/7848/9849 仅集群与网关网段可达；控制台远程管理走堡垒机。

---

## 六、Docker 部署本身不合规吗？

**不。Docker/容器化与信创、等保、密评都不冲突，等保 2.0 明确覆盖"虚拟化/容器"形态（三级新增了"可用性依赖的计算环境"等要求）。测评对象是**运行的业务系统**，不管底下是物理机、虚机还是容器。真正会成问题的不是 Docker 本身，而是**容器化后容易出现的这些做法**，本项目对照：

| 风险点 | 等保/密评对应要求 | 本项目现状与整改 |
|---|---|---|
| 明文口令/密钥写进 dockerfile、compose、git | 访问控制/密钥管理 | ⚠ `application.properties` 里 root/明文口令已进仓库，应改为环境变量注入 + git 历史清理 |
| 容器以 root 运行 | 入侵防范（最小权限） | dockerfile 加 `USER` 非 root；K8s 下设置 `runAsNonRoot` |
| 镜像来源不明、不扫描 | 恶意代码防范 | 基础镜像固定 digest；CI 中加 Trivy/Clair 扫描；基础镜像可换 openEuler/麒麟（顺带满足信创） |
| 端口全部 `-p` 映射到宿主机 | 访问控制/网络架构 | ⚠ compose 里各服务端口全量映射，生产应仅映射 nginx 80/443，其余走内部网络 `expose` |
| 容器内服务无日志留存 | 安全审计 | 已有 accesslog，但要挂 volume 持久化并外送集中日志（≥6 个月） |
| 容器逃逸面过大（privileged、挂 docker.sock） | 入侵防范 | 检查 compose 中无 privileged/宿主机敏感挂载，保持默认 |
| 镜像里打包明文配置 | 数据保密性 | 用构建参数/运行时注入，镜像只含代码 |

**结论**：保持 Docker 部署没问题，等保测评时如实按"容器化部署"描述架构即可。如果目标环境是政务云/信创环境，容器平台本身可能被要求用国产化平台（如华为CCE、东方通 TKE 等）或至少基础镜像、宿主机 OS 国产化——这是平台选型问题，不影响"容器化"这个形态本身。真正要动手改的是上表里的 4 个 ⚠ 项。

---

## 七、参考文档

- 官方升级/建库 SQL：`docs/sql/nacos2-config-mysql.sql`、`docs/sql/nacos3-config-mysql.sql`
- 等保 2.0：GB/T 22239-2019《网络安全等级保护基本要求》
- 密评：GB/T 39786-2021《信息安全技术 信息系统密码应用基本要求》
- Nacos 安全：官方文档「权限认证」与「集群部署」章节
