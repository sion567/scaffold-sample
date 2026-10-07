# 新业务模块接入指南（5 步）

以新增业务服务 `yourproj-order` 为例，从零到被网关路由、可被前端访问。

## 第 1 步：建模块骨架

```
services/order/
├── pom.xml                      parent = <groupId>:<name>-services，构件名 <name>-order
└── src/main/
    ├── java/com/<pkg>/order/
    │   ├── OrderApplication.java          @SpringBootApplication + @EnableCustomConfig
    │   ├── api/                           对外 REST（Triple REST 直接挂在 @DubboService 实现上，无需 MVC Controller 层）
    │   ├── api/impl/
    │   ├── domain/ service/ repository/
    │   └── resources/db/migration/h2/V1__init_order.sql   Flyway 建表，命名 V{n}__desc.sql
    └── resources/
        ├── application.yml                spring.application.name: <name>-order + config.import 两条约定
        └── <name>-order-local.yml         本地 DB/Redis 地址（支持 ${env:...} 覆盖）
```

`application.yml` 的配置导入约定（与平台服务一致）：

```yaml
spring:
  config:
    import:
      - optional:nacos:<name>-defaults.yml      # 全平台共享默认值
      - optional:nacos:<name>-order-local.yml   # 本服务 Nacos 侧配置
```

启动类注解说明：`@EnableCustomConfig`（组合 AOP 代理暴露 + `@EnableAsync`；实体不在应用包树下时补 `@EntityScan`）。

## 第 2 步：Nacos 配置

- 为新服务创建 `<name>-order-local.yml`（dataId 与 import 约定一致）。
- 端口：Dubbo 端口在 local yml 的 `dubbo.protocol.port` 指定（各服务错开，参考现有 20880~20884 段往下排）。

## 第 3 步：网关路由（零改动优先）

- **推荐**：服务注册到 Nacos 时在 metadata 声明 `gateway-path: order`，网关动态路由每 30 秒自动生成 `Path=/order/** → lb://<name>-order`（`<name>.gateway.dynamic-routes.enabled=true` 时生效）。
- 兜底：在 `<name>-gateway-local.yml` 加静态路由（参考现有 5 条平台路由写法）。
- 鉴权：新路径默认走 AuthFilter 的 JWT 校验；匿名接口加进 `security.ignore.whites` 白名单。

## 第 4 步：RPC 契约（可选，供其他服务内部调用）

- 在 `api/` 下新建 `<name>-order-api`：`.proto` 定义 + `java_package = com.<pkg>.order.api.proto`，pom 参考 `api/system-api`（dubbo-maven-plugin 生成 Triple 代码）。
- 消费方 `@DubboReference` 直连，**不经过网关**；内部接口加 `@InnerAuth`。

## 第 5 步：菜单与权限 SQL

- 新增 `V{n}__add_order_menus.sql`：`SYS_MENU` 插入目录/菜单/按钮（perms 串建议 `<name>-order:xxx:yyy` 风格）+ 角色关联。
- 前端路由由 `getRouters` 动态下发，无需改前端路由表。

## 自检清单

- [ ] `mvn install` 通过，服务在 Nacos 注册成功
- [ ] Flyway 首次启动自动建表（`flyway_schema_history` 有记录）
- [ ] 经网关访问 `/order/**` 鉴权链路通（无 token 401、有 token 200）
- [ ] `@Log` 注解的操作日志经 LogPersister 落地（默认 SLF4J，接入自有审计时注册 LogPersister Bean 覆盖）
- [ ] 至少一个 `@H2JpaTest` repository 切片测试 + 一个 Dubbo 直连契约测试
