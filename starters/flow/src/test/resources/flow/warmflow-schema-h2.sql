-- ============================================================
-- Warm-Flow 1.8.9 官方表（H2 MODE=MySQL，供契约测试初始化引擎表结构）
-- ============================================================
-- ============================================================
-- Warm-Flow 1.3.3 官方 7 张表（MySQL 版列，放宽约束以适配 H2）
-- ============================================================
CREATE TABLE IF NOT EXISTS flow_definition (
    id BIGINT PRIMARY KEY,
    flow_code VARCHAR(40),
    flow_name VARCHAR(100),
    model_value VARCHAR(40) DEFAULT 'CLASSICS',
    category VARCHAR(100),
    version VARCHAR(20),
    is_publish TINYINT,
    form_custom CHAR(1),
    form_path VARCHAR(100),
    activity_status TINYINT,
    listener_type VARCHAR(100),
    listener_path VARCHAR(400),
    ext VARCHAR(500),
    create_time TIMESTAMP,
    create_by VARCHAR(64),
    update_time TIMESTAMP,
    update_by VARCHAR(64),
    del_flag CHAR(1) DEFAULT '0',
    tenant_id VARCHAR(40)
);

CREATE TABLE IF NOT EXISTS flow_node (
    id BIGINT PRIMARY KEY,
    node_type TINYINT,
    definition_id BIGINT,
    node_code VARCHAR(100),
    node_name VARCHAR(100),
    permission_flag VARCHAR(200),
    node_ratio VARCHAR(200),
    coordinate VARCHAR(100),
    any_node_skip VARCHAR(100),
    listener_type VARCHAR(100),
    listener_path VARCHAR(400),
    handler_type VARCHAR(100),
    handler_path VARCHAR(400),
    form_custom CHAR(1),
    form_path VARCHAR(100),
    version VARCHAR(20),
    create_time TIMESTAMP,
    create_by VARCHAR(64),
    update_time TIMESTAMP,
    update_by VARCHAR(64),
    ext TEXT,
    del_flag CHAR(1) DEFAULT '0',
    tenant_id VARCHAR(40)
);

CREATE TABLE IF NOT EXISTS flow_skip (
    id BIGINT PRIMARY KEY,
    definition_id BIGINT,
    node_id BIGINT,
    now_node_code VARCHAR(100),
    now_node_type TINYINT,
    next_node_code VARCHAR(100),
    next_node_type TINYINT,
    skip_name VARCHAR(100),
    skip_type VARCHAR(40),
    skip_condition VARCHAR(200),
    coordinate VARCHAR(100),
    create_time TIMESTAMP,
    create_by VARCHAR(64),
    update_time TIMESTAMP,
    update_by VARCHAR(64),
    del_flag CHAR(1) DEFAULT '0',
    tenant_id VARCHAR(40)
);

CREATE TABLE IF NOT EXISTS flow_instance (
    id BIGINT PRIMARY KEY,
    definition_id BIGINT,
    business_id VARCHAR(40),
    node_type TINYINT,
    node_code VARCHAR(40),
    node_name VARCHAR(100),
    variable TEXT,
    flow_status VARCHAR(20),
    activity_status TINYINT,
    def_json TEXT,
    create_time TIMESTAMP,
    create_by VARCHAR(64),
    update_time TIMESTAMP,
    update_by VARCHAR(64),
    ext VARCHAR(500),
    del_flag CHAR(1) DEFAULT '0',
    tenant_id VARCHAR(40)
);

CREATE TABLE IF NOT EXISTS flow_task (
    id BIGINT PRIMARY KEY,
    definition_id BIGINT,
    instance_id BIGINT,
    node_code VARCHAR(100),
    node_name VARCHAR(100),
    node_type TINYINT,
    flow_status VARCHAR(20),
    form_custom CHAR(1),
    form_path VARCHAR(100),
    create_time TIMESTAMP,
    create_by VARCHAR(64),
    update_time TIMESTAMP,
    update_by VARCHAR(64),
    del_flag CHAR(1) DEFAULT '0',
    tenant_id VARCHAR(40)
);

CREATE TABLE IF NOT EXISTS flow_his_task (
    id BIGINT PRIMARY KEY,
    definition_id BIGINT,
    instance_id BIGINT,
    task_id BIGINT,
    flow_name VARCHAR(100),
    business_id VARCHAR(40),
    node_code VARCHAR(100),
    node_name VARCHAR(100),
    node_type TINYINT,
    target_node_code VARCHAR(200),
    target_node_name VARCHAR(200),
    approver VARCHAR(40),
    cooperate_type TINYINT,
    collaborator VARCHAR(500),
    skip_type VARCHAR(10),
    flow_status VARCHAR(20),
    form_custom CHAR(1),
    form_path VARCHAR(100),
    message VARCHAR(500),
    variable TEXT,
    ext TEXT,
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    del_flag CHAR(1) DEFAULT '0',
    tenant_id VARCHAR(40)
);

CREATE TABLE IF NOT EXISTS flow_user (
    id BIGINT PRIMARY KEY,
    type CHAR(1),
    processed_by VARCHAR(80),
    associated BIGINT,
    create_time TIMESTAMP,
    create_by VARCHAR(80),
    update_time TIMESTAMP,
    update_by VARCHAR(64),
    del_flag CHAR(1) DEFAULT '0',
    tenant_id VARCHAR(40)
);
CREATE TABLE IF NOT EXISTS flow_form (
    id BIGINT PRIMARY KEY,
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    create_by VARCHAR(64),
    update_by VARCHAR(64),
    del_flag CHAR(1) DEFAULT '0',
    tenant_id VARCHAR(40),
    form_code VARCHAR(100),
    form_name VARCHAR(100),
    version VARCHAR(20),
    is_publish TINYINT,
    form_type TINYINT,
    form_path VARCHAR(100),
    form_content TEXT,
    ext TEXT
);

CREATE INDEX IF NOT EXISTS idx_wf_user_processed ON flow_user (processed_by, type);
CREATE INDEX IF NOT EXISTS idx_wf_user_associated ON flow_user (associated);
CREATE INDEX IF NOT EXISTS idx_wf_task_instance ON flow_task (instance_id);

-- ============================================================
