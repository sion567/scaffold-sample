-- services/audit sqlserver 方言家族基线（由 scripts/gen_dialect_migrations.py 从 h2 基线生成，勿手工编辑；
-- GENERATED-BY gen_dialect_migrations.py
-- 修正应改脚本映射表后 --force 重新生成；真实库接入回归见 docs/multi-database-guide.md §6）

-- 本地 H2 基线（复制自测试 DDL，已被模块测试验证）
-- 注意：非完整生产 schema；菜单/字典等种子缺口见 docs/合规对照与落地行动方案.md

-- H2 建表 + 种子脚本：scaffold-audit 全表
-- 主键均由应用层雪花 ID 生成，与 V1__init_audit.sql 保持一致（无自增/序列）
-- audit_oper_log（操作日志，scaffold-audit 自有审计表，仅追加；扩展列用于等保 8.1.4 审计）

CREATE TABLE audit_oper_log (
    oper_id BIGINT NOT NULL,
    PRIMARY KEY (oper_id),
    title NVARCHAR(500),
    business_type INT,
    method NVARCHAR(200),
    request_method NVARCHAR(10),
    operator_type INT,
    oper_name NVARCHAR(50),
    dept_name NVARCHAR(200),
    oper_url NVARCHAR(255),
    oper_ip NVARCHAR(128),
    oper_param NVARCHAR(2000),
    json_result NVARCHAR(2000),
    status INT,
    error_msg NVARCHAR(2000),
    oper_time DATETIME2,
    cost_time BIGINT,
    user_id BIGINT,
    session_id NVARCHAR(100),
    biz_key NVARCHAR(100),
    biz_type NVARCHAR(50),
    event_type NVARCHAR(50),
    result_code NVARCHAR(20),
    request_ip NVARCHAR(128),
    request_id NVARCHAR(64),
    before_value NVARCHAR(2000),
    after_value NVARCHAR(2000)
);
EXEC sp_addextendedproperty @name=N'MS_Description', @value=N'请求贯穿ID（与 query_audit_log.request_id 同源）', @level0type=N'SCHEMA', @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'audit_oper_log', @level2type=N'COLUMN', @level2name=N'request_id';


-- sys_role（system 域只读授权表，支持 del_flag + status，供 AuditLogMapper.selectUserRoles 使用）

CREATE TABLE sys_role (
    role_id BIGINT IDENTITY(1,1),
    PRIMARY KEY (role_id),
    role_name NVARCHAR(50),
    role_key NVARCHAR(100),
    del_flag NCHAR(1) DEFAULT '0',
    status NCHAR(1) DEFAULT '0'
);

-- sys_user_role（system 域只读授权表，关联表）

CREATE TABLE sys_user_role (
    user_id BIGINT,
    role_id BIGINT
);

-- collector_audit_log

CREATE TABLE collector_audit_log (
    id BIGINT NOT NULL,
    PRIMARY KEY (id),
    domain NVARCHAR(100),
    source NVARCHAR(100),
    channel_type NVARCHAR(50),
    status NVARCHAR(20),
    record_count BIGINT,
    watermark_before NVARCHAR(100),
    watermark_after NVARCHAR(100),
    error_msg NVARCHAR(1000),
    cost_ms BIGINT,
    start_time DATETIME2,
    create_time DATETIME2
);

-- query_audit_log

CREATE TABLE query_audit_log (
    id BIGINT NOT NULL,
    PRIMARY KEY (id),
    caller_system NVARCHAR(100),
    operator_name NVARCHAR(100),
    subject_uid NVARCHAR(100),
    subject_masked NVARCHAR(100),
    query_type NVARCHAR(50),
    purpose NVARCHAR(200),
    request_id NVARCHAR(100),
    result_count BIGINT,
    result_summary NVARCHAR(500),
    query_time DATETIME2
);

-- sys_config（system 域只读授权表，供 AuditLogMapper.selectSecurityConfig 使用）

CREATE TABLE sys_config (
    config_id BIGINT IDENTITY(1,1),
    PRIMARY KEY (config_id),
    config_name NVARCHAR(100),
    config_key NVARCHAR(100),
    config_value NVARCHAR(500),
    config_type NCHAR(1) DEFAULT 'N'
);

-- audit_logininfor（登录日志，scaffold-audit 自有审计表，仅追加；供 selectLogininforList / selectLoginFailTop10 使用）

CREATE TABLE audit_logininfor (
    info_id BIGINT NOT NULL,
    PRIMARY KEY (info_id),
    user_name NVARCHAR(100),
    ipaddr NVARCHAR(128),
    status NCHAR(1),
    msg NVARCHAR(500),
    access_time DATETIME2,
    user_id BIGINT,
    device_info NVARCHAR(255),
    session_id NVARCHAR(64)
);
EXEC sp_addextendedproperty @name=N'MS_Description', @value=N'用户ID（登录成功后回填；失败尝试可为空）', @level0type=N'SCHEMA', @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'audit_logininfor', @level2type=N'COLUMN', @level2name=N'user_id';
EXEC sp_addextendedproperty @name=N'MS_Description', @value=N'设备信息（浏览器/OS/User-Agent）', @level0type=N'SCHEMA', @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'audit_logininfor', @level2type=N'COLUMN', @level2name=N'device_info';
EXEC sp_addextendedproperty @name=N'MS_Description', @value=N'会话ID（与 Redis 会话键关联）', @level0type=N'SCHEMA', @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'audit_logininfor', @level2type=N'COLUMN', @level2name=N'session_id';


SET IDENTITY_INSERT sys_config ON;
INSERT INTO sys_config (config_name, config_key, config_value, config_type) VALUES ('密码策略-最小长度', 'sys.account.password.minLength', '8', 'Y');
INSERT INTO sys_config (config_name, config_key, config_value, config_type) VALUES ('密码策略-最大长度', 'sys.account.password.maxLength', '20', 'Y');

-- audit_logininfor 种子：id=1 失败（供 selectLoginFailTop10），id=2 成功
SET IDENTITY_INSERT sys_config OFF;
INSERT INTO audit_logininfor (info_id, user_name, ipaddr, status, msg, access_time) VALUES (1, 'admin', '127.0.0.1', '1', '密码错误', CURRENT_TIMESTAMP);
INSERT INTO audit_logininfor (info_id, user_name, ipaddr, status, msg, access_time) VALUES (2, 'admin', '10.0.0.1', '0', '登录成功', CURRENT_TIMESTAMP);

-- 种子数据
INSERT INTO audit_oper_log (oper_id, title, business_type, oper_name, status, event_type, biz_key, user_id, oper_time, cost_time) VALUES (1, '用户登录', 1, 'admin', 0, 'LOGIN', 'user:1', 1, CURRENT_TIMESTAMP, 150);

INSERT INTO audit_oper_log (oper_id, title, business_type, oper_name, status, event_type, biz_key, user_id, oper_time, cost_time) VALUES (2, '角色授权', 2, 'admin', 0, 'ROLE_ASSIGN', 'user:1', 1, CURRENT_TIMESTAMP, 80);

INSERT INTO audit_oper_log (oper_id, title, business_type, oper_name, status, event_type, biz_key, user_id, oper_time, cost_time) VALUES (3, '权限变更', 3, 'admin', 0, 'PERM_CHANGE', 'user:2', 1, CURRENT_TIMESTAMP, 60);

-- biz_key 不是 "user:" 开头，不被 filterByBizKey 命中
INSERT INTO audit_oper_log (oper_id, title, business_type, oper_name, status, event_type, biz_key, user_id, oper_time, cost_time) VALUES (4, '系统配置', 6, 'admin', 0, 'CONFIG_CHANGE', 'config:sys', 1, CURRENT_TIMESTAMP, 30);

SET IDENTITY_INSERT sys_role ON;
INSERT INTO sys_role (role_id, role_name, role_key, del_flag, status) VALUES (1, '管理员', 'admin', '0', '0');
INSERT INTO sys_role (role_id, role_name, role_key, del_flag, status) VALUES (2, '普通用户', 'common', '0', '0');
INSERT INTO sys_role (role_id, role_name, role_key, del_flag, status) VALUES (3, '已删除角色', 'deleted', '2', '0');

SET IDENTITY_INSERT sys_role OFF;
INSERT INTO sys_user_role (user_id, role_id) VALUES (1, 1);
INSERT INTO sys_user_role (user_id, role_id) VALUES (1, 2);

INSERT INTO collector_audit_log (id, domain, source, channel_type, status, record_count, create_time) VALUES (100, 'tourist', 'file', 'csv', 'success', 1000, CURRENT_TIMESTAMP);

INSERT INTO query_audit_log (id, caller_system, operator_name, subject_uid, query_type, request_id, result_count) VALUES (100, 'ct-web', 'admin', 'uid-001', 'person_query', 'req-123', 50);


DBCC CHECKIDENT ('sys_role', RESEED, 99);

DBCC CHECKIDENT ('sys_config', RESEED, 99);

-- audit_trace（V2：全链路业务留痕，主键应用侧雪花）

CREATE TABLE audit_trace (
    trace_id BIGINT NOT NULL,
    scene NVARCHAR(64) NOT NULL,
    biz_type NVARCHAR(64),
    biz_id NVARCHAR(64),
    operator NVARCHAR(64),
    operate_ip NVARCHAR(64),
    terminal NVARCHAR(32),
    method_name NVARCHAR(256),
    snapshot_json NVARCHAR(MAX),
    result_json NVARCHAR(MAX),
    error_msg NVARCHAR(1000),
    cost_ms BIGINT,
    key_action NCHAR(1) DEFAULT '0',
    prev_digest NVARCHAR(64),
    curr_digest NVARCHAR(64) NOT NULL,
    sign_value NVARCHAR(512),
    op_time DATETIME2 NOT NULL,
    create_time DATETIME2 DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_audit_trace PRIMARY KEY (trace_id)
);
