# gm/ — 国密 keystore（dev，仅限本地联调）

- `gm.bcfks`：SM2 密钥对（alias `202607-h1`，StorePass123/KeyPass123），BCFKS 格式（BC 专有）。
  auth/message/system 三个服务的 `gm.keystore.path` 默认指向本目录（相对各服务工作目录，
  环境变量 `GM_STORE_PATH` 可覆盖）。
- 存量 `keystore/gm.p12` 的转换命令见 [docs/design-notes.md](../docs/design-notes.md)（GmKeystoreConvertTool）。
- 生产环境密钥不入仓库：path/store-pass/key-pass 全部走环境变量或配置中心。
