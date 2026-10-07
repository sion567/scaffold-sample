-- scaffold-audit Flyway 基线（2026-09-17 由 V1~V2 合并；分类分级三件套已折入建表：无）
-- 团队约定：合并后新增增量从 V2 起步；已执行旧迁移的开发库需重建 schema 或清理 flyway_schema_history

-- ======== 原 V1__init_audit.sql ========
-- ============================================================================
-- scaffold-audit V1：审计域自有表（APP_AUD schema，全部仅追加，等保 8.1.4 e）
-- collector_audit_log 采集留痕（回执/失败告警）
-- query_audit_log      查询审计（谁、何时、查了谁、返回什么）
-- audit_oper_log       操作日志（原 scaffold-system sys_oper_log 职责迁入）
-- audit_logininfor     登录日志（原 scaffold-system sys_logininfor 职责迁入）
-- 主键统一由应用层雪花 ID 生成（SnowflakeIdGenerator），不建自增列/序列；
-- 写入口：RemoteAuditService / RemoteLogService(Dubbo/Triple)，读出口：AuditResource
-- 存量迁移（如需）：INSERT INTO audit_oper_log/audit_logininfor(...)
--   SELECT ... FROM <system-schema>.sys_oper_log/sys_logininfor;
-- ============================================================================

CREATE TABLE collector_audit_log (
    id               BIGINT          NOT NULL,
    domain           VARCHAR(32)     NOT NULL,
    source           VARCHAR(64)     NOT NULL,
    channel_type     VARCHAR(16),
    status           CHAR(1)         NOT NULL,
    record_count     BIGINT,
    watermark_before VARCHAR(128),
    watermark_after  VARCHAR(128),
    error_msg        VARCHAR(1000),
    cost_ms          BIGINT,
    start_time       TIMESTAMP,
    create_time      TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_collector_audit PRIMARY KEY (id)
);

COMMENT ON TABLE collector_audit_log IS '采集留痕日志（成功回执/失败告警，仅追加不修改）';
COMMENT ON COLUMN collector_audit_log.domain IS '领域标识：police/gov/ship/...';
COMMENT ON COLUMN collector_audit_log.source IS '数据源标识：PIRS_API/PIRS_FILE/DWH_CZRK/...';
COMMENT ON COLUMN collector_audit_log.channel_type IS '通道类型：API/FILE/DB/MQ';
COMMENT ON COLUMN collector_audit_log.status IS '结果：0-成功 1-失败';
COMMENT ON COLUMN collector_audit_log.record_count IS '本批记录数（失败为0）';
COMMENT ON COLUMN collector_audit_log.watermark_before IS '采集前水位';
COMMENT ON COLUMN collector_audit_log.watermark_after IS '采集后水位（失败时为空，水位未推进）';
COMMENT ON COLUMN collector_audit_log.error_msg IS '失败原因（截断1000）';
COMMENT ON COLUMN collector_audit_log.cost_ms IS '耗时毫秒';

CREATE INDEX idx_ca_domain_source ON collector_audit_log(domain, source);
CREATE INDEX idx_ca_status_time ON collector_audit_log(status, create_time);
CREATE INDEX idx_ca_create_time ON collector_audit_log(create_time);

CREATE TABLE query_audit_log (
    id              BIGINT          NOT NULL,
    caller_system   VARCHAR(64)     NOT NULL,
    operator_name   VARCHAR(64),
    subject_uid     VARCHAR(64)     NOT NULL,
    subject_masked  VARCHAR(128),
    query_type      VARCHAR(32)     NOT NULL,
    purpose         VARCHAR(255),
    request_id      VARCHAR(64),
    result_count    BIGINT,
    result_summary  VARCHAR(500),
    query_time      TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_query_audit PRIMARY KEY (id)
);

COMMENT ON TABLE query_audit_log IS '查询审计日志（谁、何时、查了谁、返回什么，仅追加）';
COMMENT ON COLUMN query_audit_log.caller_system IS '调用方系统：UI/DATASCREEN/POLICE_COLLECTOR/...';
COMMENT ON COLUMN query_audit_log.operator_name IS '操作员账号（系统触发为SYSTEM）';
COMMENT ON COLUMN query_audit_log.subject_uid IS '查询对象人员UID';
COMMENT ON COLUMN query_audit_log.subject_masked IS '查询对象摘要（脱敏）';
COMMENT ON COLUMN query_audit_log.query_type IS '查询类型：RISK_CHECK/TRAJECTORY/DETAIL';
COMMENT ON COLUMN query_audit_log.purpose IS '业务用途/场景';
COMMENT ON COLUMN query_audit_log.result_summary IS '返回摘要（脱敏字段清单，不含明文）';

CREATE INDEX idx_qa_subject ON query_audit_log(subject_uid);
CREATE INDEX idx_qa_operator_time ON query_audit_log(operator_name, query_time);
CREATE INDEX idx_qa_time ON query_audit_log(query_time);

CREATE TABLE audit_oper_log (
    oper_id        BIGINT         NOT NULL,
    title          VARCHAR(255),
    business_type  INT,
    method         VARCHAR(200),
    request_method VARCHAR(10),
    operator_type  INT,
    oper_name      VARCHAR(64),
    dept_name      VARCHAR(128),
    oper_url       VARCHAR(255),
    oper_ip        VARCHAR(128),
    oper_param     VARCHAR(2000),
    json_result    VARCHAR(2000),
    status         INT,
    error_msg      VARCHAR(2000),
    oper_time      TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,
    cost_time      BIGINT,
    user_id        BIGINT,
    session_id     VARCHAR(64),
    biz_key        VARCHAR(100),
    biz_type       VARCHAR(50),
    event_type     VARCHAR(50),
    result_code    VARCHAR(20),
    request_ip     VARCHAR(128),
    before_value   VARCHAR(2000),
    after_value    VARCHAR(2000),
    CONSTRAINT pk_audit_oper_log PRIMARY KEY (oper_id)
);

COMMENT ON TABLE audit_oper_log IS '操作日志（审计域权威表，仅追加不修改）';
COMMENT ON COLUMN audit_oper_log.event_type IS '事件类型：LOGIN/DELETE/PWD_CHANGE/ROLE_ASSIGN/EXPORT/CONFIG_CHANGE/...';
COMMENT ON COLUMN audit_oper_log.biz_type IS '业务域：USER/ROLE/MENU/CONFIG/DICT/JOB/AUDIT/...';
COMMENT ON COLUMN audit_oper_log.biz_key IS '业务主键（如 user:1），供权限变更时间线追溯';
COMMENT ON COLUMN audit_oper_log.before_value IS '操作前快照（重要安全事件记录参数）';
COMMENT ON COLUMN audit_oper_log.after_value IS '操作后快照';

CREATE INDEX idx_aol_oper_name ON audit_oper_log(oper_name);
CREATE INDEX idx_aol_oper_time ON audit_oper_log(oper_time);
CREATE INDEX idx_aol_event_type ON audit_oper_log(event_type);
CREATE INDEX idx_aol_biz_key ON audit_oper_log(biz_key);

CREATE TABLE audit_logininfor (
  info_id     BIGINT        NOT NULL,
  user_name   VARCHAR(64),
  ipaddr      VARCHAR(128),
  status      CHAR(1)       DEFAULT '0',
  msg         VARCHAR(255),
  access_time TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT pk_audit_logininfor PRIMARY KEY (info_id)
);

COMMENT ON TABLE audit_logininfor IS '登录日志（审计域权威表，仅追加不修改）';
COMMENT ON COLUMN audit_logininfor.status IS '登录状态：0-成功 1-失败';
COMMENT ON COLUMN audit_logininfor.access_time IS '访问时间';

CREATE INDEX idx_ali_user_name ON audit_logininfor(user_name);
CREATE INDEX idx_ali_status_time ON audit_logininfor(status, access_time);
CREATE INDEX idx_ali_access_time ON audit_logininfor(access_time);

COMMIT;

-- ======== 原 V2__create_audit_trace.sql ========
-- ============================================================================
-- scaffold-audit V2：全链路业务留痕表（详细设计 §3.4 audit_trace，情指行差距清单 G2/G3）
-- 与 audit_oper_log（操作日志）互补：本表记业务动作前后快照 + SM3 摘要链防篡改，
-- 关键动作（指令下发/签收/改派/撤回/导出）额外 SM2 签名（sign_value）。
-- 沿用 V1 约定：仅追加不 UPDATE；主键应用侧雪花 ID 生成，不建自增列/序列。
-- 写入口：RemoteTraceLogService(Dubbo)，采集口：scaffold-common-log @TraceLog 切面。
-- ============================================================================

CREATE TABLE audit_trace (
    trace_id      BIGINT          NOT NULL,
    scene         VARCHAR(64)     NOT NULL,
    biz_type      VARCHAR(64),
    biz_id        VARCHAR(64),
    operator      VARCHAR(64),
    operate_ip    VARCHAR(64),
    terminal      VARCHAR(32),
    method_name   VARCHAR(256),
    snapshot_json CLOB,
    result_json   CLOB,
    error_msg     VARCHAR(1000),
    cost_ms       BIGINT,
    key_action    CHAR(1)         DEFAULT '0',
    prev_digest   VARCHAR(64),
    curr_digest   VARCHAR(64)     NOT NULL,
    sign_value    VARCHAR(512),
    op_time       TIMESTAMP       NOT NULL,
    create_time   TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_audit_trace PRIMARY KEY (trace_id)
);

COMMENT ON TABLE audit_trace IS '全链路业务留痕（SM3 摘要链防篡改：curr=SM3(prev+本条规范串)；仅追加不修改）';
COMMENT ON COLUMN audit_trace.scene IS '业务场景：指令下发/签收/改派/撤回/情报查询/规则发布…';
COMMENT ON COLUMN audit_trace.biz_type IS '业务对象类型：instruction/dispatch_order/alert_rule…';
COMMENT ON COLUMN audit_trace.biz_id IS '业务对象 ID（@TraceLog bizIdEl 解析）';
COMMENT ON COLUMN audit_trace.operator IS '操作人账号（系统动作为空）';
COMMENT ON COLUMN audit_trace.terminal IS '终端类型：WEB/警务通…（X-Terminal 头，缺省 WEB）';
COMMENT ON COLUMN audit_trace.snapshot_json IS '入参快照 JSON（截断 2000）';
COMMENT ON COLUMN audit_trace.result_json IS '返回值快照 JSON（截断 2000；异常为空）';
COMMENT ON COLUMN audit_trace.key_action IS '关键动作：0-否 1-是（1 时 sign_value 应有值）';
COMMENT ON COLUMN audit_trace.prev_digest IS '前一条留痕摘要（链头为空串）';
COMMENT ON COLUMN audit_trace.curr_digest IS '本条摘要 = SM3(prev + "|" + 规范化串)';
COMMENT ON COLUMN audit_trace.sign_value IS '关键动作 SM2 签名（对 curr_digest，hex；未配置密钥时为空）';
COMMENT ON COLUMN audit_trace.op_time IS '操作发生时间（切面采集时刻，非落库时间）';

CREATE INDEX idx_at_biz ON audit_trace(biz_type, biz_id);
CREATE INDEX idx_at_op_time ON audit_trace(op_time);

-- ===================================================================
-- 以下并入原 V3__audit_columns.sql（项目未上线，2026-09 合并为单基线）：
-- 审计表列级补齐（A7）：audit_logininfor 补 user_id/device_info/session_id；audit_oper_log 补 request_id
ALTER TABLE audit_logininfor ADD (
    user_id     NUMBER(20),
    device_info VARCHAR(255),
    session_id  VARCHAR(64)
);
COMMENT ON COLUMN audit_logininfor.user_id IS '用户ID（登录成功后回填；失败尝试可为空）';
COMMENT ON COLUMN audit_logininfor.device_info IS '设备信息（浏览器/OS/User-Agent）';
COMMENT ON COLUMN audit_logininfor.session_id IS '会话ID（与 Redis 会话键关联）';

ALTER TABLE audit_oper_log ADD (
    request_id VARCHAR(64)
);
COMMENT ON COLUMN audit_oper_log.request_id IS '请求贯穿ID（与 query_audit_log.request_id 同源）';
