-- V2 修复菜单数据引发的前端路由 404（仅数据修正，无结构变更）
--
-- 背景：后端 getRouteName 按 path 段兜底生成前端路由名（SysMenuServiceImpl#getRouteName），
-- 审计中心子菜单与系统管理侧菜单 path 段相同（config/operlog/logininfor），产生同名路由；
-- vue-router addRoute 同名先移除旧记录，admin 可见全部菜单且审计中心（order_num=9）后注册，
-- /system/config、/system/operlog、/system/logininfor 被顶替而落入前端 404 兜底页。
-- 修复：给审计中心子菜单补唯一 ROUTE_NAME（路由名需与视图组件 name 一致以命中 keep-alive，
-- views/audit/stats/index.vue 已为 name="AuditStats"）。
UPDATE SYSTEM_DB.SYS_MENU SET ROUTE_NAME = 'AuditOperlog'    WHERE MENU_ID = 2001;
UPDATE SYSTEM_DB.SYS_MENU SET ROUTE_NAME = 'AuditLogininfor' WHERE MENU_ID = 2002;
UPDATE SYSTEM_DB.SYS_MENU SET ROUTE_NAME = 'AuditStats'      WHERE MENU_ID = 2003;
UPDATE SYSTEM_DB.SYS_MENU SET ROUTE_NAME = 'AuditSnapshot'   WHERE MENU_ID = 2004;
UPDATE SYSTEM_DB.SYS_MENU SET ROUTE_NAME = 'AuditConfig'     WHERE MENU_ID = 2005;

-- 组件路径错位修正：system/operlog|logininfor/index 在前端仓库不存在（原型迁移遗留），
-- 实际页面为 monitor/operlog/index、monitor/logininfor/index，日志管理两个子页此前打不开。
UPDATE SYSTEM_DB.SYS_MENU SET COMPONENT = 'monitor/operlog/index'    WHERE MENU_ID = 500;
UPDATE SYSTEM_DB.SYS_MENU SET COMPONENT = 'monitor/logininfor/index' WHERE MENU_ID = 501;
