# scaffold-ui

脚手架管理前端（Vue 3 + TypeScript + Vite + Element Plus + Pinia）。

## 开发

```bash
npm install
npm run dev        # 代理指向本地网关（见 .env.development / vite.config.ts）
```

## 检查与构建

```bash
npm run typecheck  # vue-tsc 全量类型检查（全前端 TS 化的守门员）
npm run build:prod # vue-tsc + 生产构建
npm run test:run   # Vitest 单测
```

## 说明

- 页面菜单由后端 `getRouters` 动态下发，页面文件按菜单 component 路径放置于 `src/views/**`
- 登录密码 SM2 加密传输（公钥由 `/auth/gm-key` 运行时下发）
- 样例页面见 `src/views/sample/**`（与代码生成器产物同构）
