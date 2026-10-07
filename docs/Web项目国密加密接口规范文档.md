# Web项目国密加密接口规范文档

**文档版本**：V1.2
**适用对象**：前端开发组、后端开发组、测试组、安全组
**生效日期**：2026-08-31

## 修订记录

| 版本 | 日期 | 修订内容 |
|------|------|----------|
| V1.0 | 2026-08-29 | 初版：固定 SM4 密钥 + 敏感字段白名单加密 + SM2 签名 |
| V1.1 | 2026-08-31 | 改为 **SM2+SM4 信封加密**：每请求随机 SM4 密钥加密业务数据，SM2 公钥加密该密钥；废除固定 SM4 密钥下发；报文、签名、错误码、密钥管理与实现对齐（ct-ui / ct-auth 已落地） |
| V1.2 | 2026-08-31 | **签名密钥改为会话签发**：废除前端构建期注入静态签名私钥（gm-keygen.cjs / `VITE_GM_SIGN_*` / `frontend-public-key-hex` 全部废弃）；登录后由后端按会话生成 SM2 签名密钥对，私钥经 TLS 下发（`GET /auth/gm-sign-key`），公钥绑 token 存 Redis 验签，登出/过期即销毁；带 token 的请求强制验签，新增错误码 40006 |

---

## 一、概述

### 1.1 文档目的

本文档旨在规范Web项目在应用层实施国密算法加密与接口签名校验的技术标准，确保前后端交互过程中的数据机密性、完整性及抗重放能力，为开发团队提供统一的落地实施依据。

### 1.2 适用范围

本规范适用于项目内所有HTTP接口，包括但不限于 GET、POST、PUT、DELETE 方法。

以下流量**暂不走信封**，需按各自方式保护或另行评估：

- 文件上传（`multipart/form-data`）
- 文件下载（`responseType: blob`、`application/x-www-form-urlencoded` 表单提交）
- 标注 `secure: false` 的请求（如密钥下发接口本身）
- 网关健康检查、接口文档（`/v3/api-docs/**`、`/actuator/**`）

### 1.3 设计原则

- **信封加密（Envelope Encryption）**：前端每次请求随机生成临时 SM4 密钥加密业务数据，再用服务端 SM2 公钥加密该临时密钥，一并提交；服务端解开信封后业务 Controller 无感。
- **一次性数据密钥**：SM4 密钥每请求随机产生、密文传输，服务端不留存、不下发固定密钥，避免 V1.0 固定密钥模型的泄露面。
- **全量请求签名 + 防重放**：已登录会话的所有请求必须携带 SM2 签名（对密文签名，完整性覆盖加密后的报文），并校验 timestamp 窗口与 nonce 唯一性；签名密钥对由后端**按会话签发**（V1.2），登录前流量（无 token）仅信封+防重放。
- **统一过滤器处理**：加解密与验签逻辑对业务 Controller 完全透明，由服务端全局过滤器统一处理。

---

## 二、密码算法说明

本项目采用国家密码管理局发布的国密算法体系，具体分工如下：

| 算法 | 类型 | 用途 | 本项目约定 |
|------|------|------|-----------|
| **SM2** | 非对称加密/签名 | ① 加密临时 SM4 密钥（C1C3C2，`04`前缀 hex）；② 请求签名（SM3withSM2，DER） | 签名密钥对由后端**按会话签发**（私钥经 TLS 下发给已登录会话，公钥绑 token 存 Redis）；服务端持有**加密密钥对**私钥（keystore） |
| **SM3** | 密码杂凑 | SM3withSM2 签名内部摘要（含 ZA 预处理） | 由签名库内部完成，前端勿手动拼接 ZA |
| **SM4** | 对称加密 | 业务数据加密（ECB + PKCS7Padding，hex） | 密钥每请求随机生成 16 字节；因密钥一次性，ECB 模式风险可接受 |

**协作关系**：随机 SM4 加密业务数据 → SM2 公钥加密 SM4 密钥 → SM3withSM2 对密文报文签名。

**库约定（两端已实测互验）**：

| 端 | 库 | 关键参数 |
|----|----|----------|
| 前端 | `sm-crypto` | `sm2.doEncrypt(msg, pub, 1)`；`sm2.doSignature(canonical, priv, { hash: true, publicKey, userId: '1234567812345678', der: true })`；`sm4.encrypt(data, key, { padding: 'pkcs#7' })` |
| 后端 | 本仓 `common/core` crypto 包（BouncyCastle 底座，2026-10 迁出 zdhr-crypto） | `Sm2Engine.verify(data, sigHex)`（SM3withSM2，DER）；`Sm2Engine.decryptStr`（C1C3C2，GmIdentity 层自动补 `04`）；`Cipher("SM4/ECB/PKCS7Padding", "BC")` |

> ⚠️ 签名必须 `der: true` 且传入 `publicKey`（参与 ZA 计算），否则与后端验签必然失败。此契约已经 Java↔JS 双端实测验证。

---

## 三、整体架构与职责划分

### 3.1 前端职责（ct-ui）

1. **密钥获取**：首次请求前调用 `GET /auth/gm-key` 获取服务端 SM2 公钥（内存缓存约 4 分钟，跟随后端 `Cache-Control: max-age=300`）。
2. **签名会话初始化**：登录后（含页面刷新恢复登录态时，路由守卫触发）调用 `GET /auth/gm-sign-key` 获取会话签名密钥对，仅存本页内存，登出即清除；收到 40006/40003 时自动重签并用原始报文重放一次。
3. **信封构造**（每次请求）：随机 SM4 密钥 → SM4 加密业务 JSON（`encData`）→ SM2 公钥加密 SM4 密钥（`encKey`）。
4. **签名**：对 `{encKey, encData, timestamp, nonce}` 按 ASCII 升序拼装后用会话签名私钥 SM2 签名（`signature`）。
5. **报文组装**：POST/PUT 替换整个请求体；GET/DELETE 业务参数全部进信封、密文参数拼入 Query。

实现位置：`utils/gmEnvelope.js`（信封构造）、`utils/request.js`（统一拦截器接入）、`utils/crypto.js`（算法封装）、`utils/sm2.js`（登录密码 SM2 加密）。**业务代码禁止单独调用加密函数。**

### 3.2 后端职责（ct-auth，业务服务按附录 A 接入）

1. **全局拦截**：`GmEnvelopeFilter`（`OncePerRequestFilter`）按 `include/exclude-patterns` 拦截。
2. **防重放校验**：timestamp 窗口（默认 5 分钟）+ nonce Redis SETNX 唯一性（TTL 与窗口一致）。
3. **签名验证**：按请求 token 从 Redis 取会话验签公钥（`gm:sign:pub:{token}`），`Sm2Engine.verify(canonical, signature)`，失败直接阻断；未签发/过期返回 40006 引导前端重签。
4. **信封解密**：SM2 私钥解 `encKey` 还原 SM4 密钥 → SM4 解 `encData` → 明文回写请求体/Query。
5. **业务放行**：Controller 接收到的即为解密后的标准明文请求，无需任何加解密代码。

### 3.3 Controller职责

- **完全无感知**：接收到的参数即为解密后的明文 JSON，专注业务逻辑。

---

## 四、加密范围

### 4.1 整体信封（V1.1 起）

V1.0 的"敏感字段白名单"方案已由**整体信封**取代：业务报文整体加密，不再逐一区分字段，白名单维护成本与漏配风险同步消除。

### 4.2 路径维度控制

加密按**路径**控制（替代字段白名单）：

| 配置 | 说明 | 默认值 |
|------|------|--------|
| `sm.envelope.include-patterns` | 强制信封的路径（Ant 风格） | `/**` |
| `sm.envelope.exclude-patterns` | 放行路径白名单 | `/gm-key`、`/v3/api-docs/**`、`/actuator/**` |

前端侧豁免见 1.2；V1.0 的敏感字段清单（身份证、手机号、银行卡、密码、薪资、邮箱）保留作为**响应脱敏与数据库字段加密**（原 zdhr-crypto-mybatis 的 SM4 TypeHandler 形态）的参考基线。

---

## 五、前端实现规范

### 5.1 请求拦截器封装

- 基于 `utils/request.js` 的 Axios 请求拦截器统一封装，禁止在业务代码中单独调用加密函数。
- 拦截器内部自动完成：信封加密 → 签名 → 报文组装，并附加请求头 `X-Gm-Envelope: 1`。
- **防重复提交判重必须在信封加密前用明文数据执行**（密文中 nonce 每次变化，密文判重永远失效）。

### 5.2 信封构造流程

1. 随机生成 16 字节 SM4 密钥（32 字符 hex）。
2. `encData = SM4-ECB-PKCS7(JSON.stringify(业务参数), 临时SM4密钥)`，输出 hex；GET 无参数时对 `"{}"` 加密，协议格式保持一致。
3. `encKey = '04' + SM2C1C3C2(临时SM4密钥hex, 服务端公钥)`（`utils/sm2.js#encryptBySm2`）。
4. 内存缓存服务端公钥（keyId + publicKey，TTL 4 分钟），密钥轮换时随缓存过期自动更新。

### 5.3 签名计算流程

1. **参数收集**：`{encKey, encData, timestamp, nonce}`（timestamp 毫秒值字符串，nonce 8 字节随机 hex）。
2. **排序**：按参数名 ASCII 码升序排列。
3. **拼接**：`key1=value1&key2=value2...`（空值不参与）。
4. **签名**：使用**会话签名私钥**（`GET /auth/gm-sign-key` 签发，仅内存持有）对拼装串做 SM3withSM2 签名（含 ZA），`der: true` 输出。

> 签名对**密文**进行，完整性覆盖加密后的全部业务数据；签名参数与后端 `EnvelopeCanonical` 严格一致。

### 5.4 请求报文组装

#### POST/PUT 请求体

```json
{
  "encKey": "04ab03c1...",
  "encData": "8f3e7d...（SM4 hex 密文）",
  "timestamp": 1731123456789,
  "nonce": "a1b2c3d4e5f60718",
  "signature": "3081dc0220..."
}
```

业务明文字段不再单独出现于报文。

### 5.5 GET/DELETE 请求

- 业务参数全部进信封，密文参数追加至 URL Query：

```
GET /auth/xxx?encKey=04...&encData=8f...&timestamp=173...&nonce=a1...&signature=30...
```

> 注意：hex 密文会使 URL 长度约翻倍，超长查询（大导出条件等）请改用 POST。

### 5.6 会话签名密钥获取（V1.2）

- 登录成功后（含刷新页面恢复登录态，由路由守卫 `permission.js` 触发）调用 `GET /auth/gm-sign-key`，返回 `{privateKey, publicKey}`（64/130 位 hex，公钥参与 ZA 计算）。
- 私钥**仅存本页内存**，严禁写入 localStorage/sessionStorage/cookie/日志；登出调用 `clearSignKeys()` 清除，后端同步销毁。
- 密钥 TTL（`sm.envelope.signKey-ttl`，默认 30 分钟）过期或服务端不存在时，请求返回 40006；前端自动重签并用原始报文重放一次。
- 前端**零密钥配置**：`.env` 不再有任何 `VITE_GM_SIGN_*` 项，也不存在 gm-keygen 脚本。

---

## 六、后端实现规范

### 6.1 全局过滤器设计

- `com.ct.auth.security.GmEnvelopeFilter`，`FilterRegistrationBean` 注册，Order = `HIGHEST_PRECEDENCE + 100`。
- 总开关 `sm.envelope.enabled`（默认 false，灰度上线用），按 `include/exclude-patterns` 生效。
- 核心流程：拦截 → 缺参校验 → 防重放 → 验签 → 信封解密 → 明文放行。
- 密钥下发接口：`GET /auth/gm-key`（网关 `StripPrefix=1` 后为 ct-auth 根路径 `/gm-key`），匿名白名单，返回 `{keyId, publicKey}`，带 `ETag` 与 `Cache-Control: max-age=300`，**严禁返回任何私钥或 SM4 密钥**。
- 会话签名钥签发接口：`GET /auth/gm-sign-key`（需登录态，信封白名单），返回 `{privateKey, publicKey}`，`Cache-Control: no-store`；公钥以 `gm:sign:pub:{token}` 入 Redis（TTL `sm.envelope.sign-key-ttl`，默认 30 分钟）。

### 6.2 防重放攻击校验

1. **时间戳校验**：`|当前时间 - timestamp| <= 5分钟`（`sm.envelope.timestamp-window`），超时返回 40001。
2. **Nonce校验**：Redis `SETNX sm:nonce:{scope}:{nonce}`，TTL 与时间窗口一致；已存在返回 40002。
3. Redis 异常时**放行并记录 ERROR 日志**（fail-open，保障认证链路可用性），须配套告警监控。

### 6.3 签名验证流程（V1.2 会话验签）

1. 提取请求 token（`Authorization` 头剥 `Bearer` 前缀）；**无 token（登录前流量）跳过验签**，仅信封+防重放。
2. 从 Redis 取会话验签公钥 `gm:sign:pub:{token}`；不存在 → 返回 40006（前端自动重签并重放一次）。
3. 按前端相同规则（ASCII 升序 + `k=v&`，空值不参与）拼接待签名字符串；缺 `signature` 返回 40005。
4. `Sm2Engine(null, 会话公钥).verify(拼装串, signature)`（JCA `SM3withSM2`，DER 格式，内部含 ZA）。
5. **失败处理**：返回 40003，终止请求。

### 6.4 信封解密流程

1. 验签通过后，`GmIdentity.decryptSm2(encKey)`（C1C3C2，自动补 `04` 前缀，支持多 alias 历史钥解密）还原 SM4 密钥。
2. 校验还原密钥为 32 位 hex；`SM4/ECB/PKCS7Padding`（BC）解密 `encData`。
3. 任何一步失败返回 40004。
4. **明文回写**：POST/PUT 用 `PlainBodyRequestWrapper` 替换请求体；GET/DELETE 用 `PlainQueryRequestWrapper` 将明文参数合并进 ParameterMap 并剥离安全字段。

### 6.5 请求放行

- Controller 接收到的即为标准明文 JSON，无需额外处理。

### 6.6 响应报文加密（可选）

- 暂未实现。若后续需要，建议采用 SM2+SM4 混合：本次请求的临时 SM4 密钥派生响应加密密钥，或 SM2 加密随机 SM4 密钥随响应返回。

---

## 七、密钥管理规范

| 密钥 | 生成/存储 | 分发 | 轮换 |
|------|-----------|------|------|
| **服务端 SM2 加密密钥对** | PKCS12 keystore（`gm.keystore.*`，starters/gm `GmIdentity` 管理，BC 读装） | 公钥经 `/auth/gm-key` 下发；私钥仅存 keystore | 多 alias 共存，`active-alias` 切换，旧钥解历史密文，支持热 reload |
| **会话 SM2 签名密钥对**（V1.2） | 后端按会话生成（`SessionSignKeyStore`，BouncyCastle sm2p256v1）；公钥 `gm:sign:pub:{token}` 存 Redis（TTL 默认 30 分钟），私钥不出服务端日志/存储 | 私钥经 TLS 随 `GET /auth/gm-sign-key` 下发给**已登录会话**（前端仅内存持有） | 每会话独立；签发即轮换，登出/过期即销毁，无全局共享私钥 |
| **临时 SM4 数据密钥** | 前端每请求随机生成 | 密文传输（encKey），明文不出端、不落服务端存储 | 天然一次性，无需轮换 |
| **登录密码 SM2 加密** | 复用服务端加密密钥对 | 公钥同 `/auth/gm-key` | 同服务端密钥对 |

**严禁**：将任何私钥或 SM4 密钥硬编码在 Git 仓库代码中、打印至日志文件、通过无鉴权接口返回给前端（`/auth/gm-sign-key` 是唯一的私钥下发通道，且必须已登录）。

---

## 八、异常处理规范

过滤器返回 HTTP 200 + 标准JSON（`msg` 兼容存量前端拦截器，`message` 符合本规范）：

```json
{ "code": 40003, "msg": "签名验证失败", "message": "签名验证失败", "data": null, "timestamp": 1731123460000 }
```

| 错误码 | 提示信息 | 触发场景 |
|--------|----------|----------|
| 40001 | 请求已过期 | timestamp超出时间窗口 |
| 40002 | 请求重复，请勿重复提交 | nonce已存在Redis中 |
| 40003 | 签名验证失败 | 会话SM2验签不通过（前端自动重签重放一次） |
| 40004 | 数据解析异常 | SM2/SM4解密失败、密钥格式非法 |
| 40005 | 缺少必要安全参数 | 缺失 timestamp/nonce/signature |
| 40006 | 签名会话未初始化 | 携带 token 但会话签名公钥不存在/已过期（前端自动重签并重放一次） |

---

## 九、接口请求/响应格式规范

### 9.1 统一请求格式（POST/PUT）

见 5.4；GET/DELETE 见 5.5。

### 9.2 统一响应格式

```json
{ "code": 200, "message": "success", "data": { ... }, "timestamp": 1731123460000 }
```

### 9.3 错误响应格式

见第八节。

---

## 十、安全注意事项

1. **上线顺序**：先开启后端 `sm.envelope.enabled=true`（前端拦截器天然全量信封）。开启后：无 token 请求必须走信封+防重放（40005 拒绝，fail-closed）；带 token 请求必须通过会话验签（40003/40006），由前端自动完成会话钥签发，无需人工干预。
2. **时间窗口与Nonce缓存**：窗口建议 3-5 分钟，nonce TTL 必须与窗口一致；fail-open 日志需接入告警。
3. **日志脱敏**：禁止打印SM2私钥、SM4密钥、会话签名私钥；信封密文（encData/encKey）可保留，需标注 `[ENCRYPTED]`。会话签名私钥只在 `/auth/gm-sign-key` 响应中出现一次，日志必须脱敏该响应。
4. **会话签名钥安全**：私钥仅内存持有，严禁写入 localStorage/sessionStorage/cookie；泄露面与会话等价（登出/过期即失效），无全局静态钥的历史泄露问题。
5. **算法合规**：统一使用 sm-crypto（前端）与本仓 crypto 包 + BouncyCastle（后端），两端签名/加密契约以本规范第二章为准，任何一侧升级需重跑互验用例。
6. **边界流量**：文件上传/下载暂不走信封，涉及敏感文件的接口需单独评估（如改为信封 JSON 携带 base64、或后端落盘加密）。
7. **WAF配合**：建议前端部署WAF，拦截明显恶意请求，减轻应用层验签压力。
8. **密钥审计**：定期（每季度）审计密钥签发记录与 Redis 会话钥存量（`gm:sign:pub:*` 应与会话数同量级）。
9. **密评衔接**：会话签名模型消除了"私钥静态分发/全局共享"两项硬伤；签名运算目前仍为软实现，最终合规以密码机/签名验签服务器落地为准（见 compliance-roadmap.md 阶段四）。

---

## 附录 A：业务服务接入指引

1. 引入依赖：`scaffold-spring-boot-starter-gm`（自动装配 GmIdentity/GmFieldCrypto，BouncyCastle 随 common-core 传递）。
2. 拷贝 `com.ct.auth.security` 包（Filter/Properties/Canonical/NonceCache/SessionSignKeyStore）至本服务。
3. 配置 `sm.envelope.*`（enabled、patterns、`sign-key-ttl`）与 `gm.keystore.*`；本服务需可访问同一 Redis（会话验签公钥与 nonce 共享）。
4. 前端 `VITE_APP_BASE_API` 对应路径自动生效（拦截器按服务路由前缀全量信封）；会话签名钥仍统一由 ct-auth `/auth/gm-sign-key` 签发（前端零改动）。
5. 联调冒烟：任选一接口核对「验签 40006/40003 → 防重放 40002 → 解密 40004」三条失败链路与正常链路。

## 附录 B：契约互验基线（已实测）

| 用例 | 结果 |
|------|------|
| sm-crypto `doSignature(der:true, hash:true, publicKey, userId)` ↔ `Sm2Engine.verify` | PASS |
| 篡改签名串后验签必须失败 | PASS |
| `encryptBySm2`（`04`+C1C3C2）↔ `GmIdentity.decryptSm2` 还原 SM4 密钥 | PASS |
| sm-crypto `sm4.encrypt(pkcs#7)` ↔ BC `SM4/ECB/PKCS7Padding` 明文还原 | PASS |

任何密钥/库版本变更后，须重跑附录 B 全部用例。
