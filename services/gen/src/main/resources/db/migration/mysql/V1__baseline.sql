-- services/gen mysql 方言家族基线（由 scripts/gen_dialect_migrations.py 从 h2 基线生成，勿手工编辑；
-- GENERATED-BY gen_dialect_migrations.py
-- 修正应改脚本映射表后 --force 重新生成；真实库接入回归见 docs/multi-database-guide.md §6）


CREATE TABLE gen_table (
    table_id BIGINT AUTO_INCREMENT COMMENT '编号',
    table_name VARCHAR(200) DEFAULT '' COMMENT '表名称',
    table_comment VARCHAR(500) DEFAULT '' COMMENT '表描述',
    sub_table_name VARCHAR(64) COMMENT '关联子表的表名',
    sub_table_fk_name VARCHAR(64) COMMENT '子表关联的外键名',
    class_name VARCHAR(100) DEFAULT '' COMMENT '实体类名称',
    tpl_category VARCHAR(200) DEFAULT 'crud' COMMENT '使用的模板（crud单表操作 tree树表操作）',
    tpl_web_type VARCHAR(30) DEFAULT '' COMMENT '前端模板类型（element-ui模版 element-plus模版）',
    package_name VARCHAR(100) COMMENT '生成包路径',
    module_name VARCHAR(30) COMMENT '生成模块名',
    business_name VARCHAR(30) COMMENT '生成业务名',
    function_name VARCHAR(50) COMMENT '生成功能名',
    function_author VARCHAR(50) COMMENT '生成功能作者',
    form_col_num INT DEFAULT 1 COMMENT '表单布局（单列 双列 三列）',
    gen_type CHAR(1) DEFAULT '0' COMMENT '生成代码方式（0zip压缩包 1自定义路径）',
    gen_path VARCHAR(200) DEFAULT '/' COMMENT '生成路径（不填默认项目路径）',
    options VARCHAR(1000) COMMENT '其它生成选项',
    create_by VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME COMMENT '创建时间',
    update_by VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME COMMENT '更新时间',
    remark VARCHAR(500) COMMENT '备注',
    CONSTRAINT pk_gen_table PRIMARY KEY (table_id)
) ENGINE=InnoDB AUTO_INCREMENT=100;



CREATE TABLE gen_table_column (
    column_id BIGINT AUTO_INCREMENT COMMENT '编号',
    table_id BIGINT COMMENT '归属表编号',
    column_name VARCHAR(200) COMMENT '列名称',
    column_comment VARCHAR(500) COMMENT '列描述',
    column_type VARCHAR(100) COMMENT '列类型',
    java_type VARCHAR(500) COMMENT 'JAVA类型',
    java_field VARCHAR(200) COMMENT 'JAVA字段名',
    is_pk CHAR(1) COMMENT '是否主键（1是）',
    is_increment CHAR(1) COMMENT '是否自增（1是）',
    is_required CHAR(1) COMMENT '是否必填（1是）',
    is_insert CHAR(1) COMMENT '是否为插入字段（1是）',
    is_edit CHAR(1) COMMENT '是否编辑字段（1是）',
    is_list CHAR(1) COMMENT '是否列表字段（1是）',
    is_query CHAR(1) COMMENT '是否查询字段（1是）',
    query_type VARCHAR(200) DEFAULT 'EQ' COMMENT '查询方式（等于、不等于、大于、小于、范围）',
    html_type VARCHAR(200) COMMENT '显示类型（文本框、文本域、下拉框、复选框、单选框、日期控件）',
    dict_type VARCHAR(200) DEFAULT '' COMMENT '字典类型',
    sort BIGINT COMMENT '排序',
    create_by VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME COMMENT '创建时间',
    update_by VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME COMMENT '更新时间',
    CONSTRAINT pk_gen_table_column PRIMARY KEY (column_id)
) ENGINE=InnoDB AUTO_INCREMENT=100;


-- ===================================================================
-- 以下并入原 V2__seed_sample_gen_table.sql（项目未上线，2026-09 合并为单基线）：
-- gen 案例：样例客户表的生成元数据种子，登录后台可直接预览/下载 SAMPLE_CUSTOMER 生成代码
INSERT INTO gen_table(table_id, table_name, table_comment, class_name, tpl_category, tpl_web_type, package_name, module_name, business_name, function_name, function_author, form_col_num, gen_type, gen_path, create_by, create_time, remark) VALUES(5, 'SAMPLE_CUSTOMER', '样例客户表', 'SampleCustomer', 'crud', 'element-plus', 'com.scaffold.sample', 'sample', 'customer', '样例客户', 'scaffold', 1, '0', '/', 'admin', CURRENT_TIMESTAMP, '单表 CRUD 生成案例');

INSERT INTO gen_table_column(table_id, column_name, column_comment, column_type, java_type, java_field, is_pk, is_increment, is_required, is_insert, is_edit, is_list, is_query, query_type, html_type, dict_type, sort) VALUES(5, 'customer_id', '客户ID', 'bigint(20)', 'Long', 'customerId', '1', '1', '1', null, null, null, null, null, null, null, 1);
INSERT INTO gen_table_column(table_id, column_name, column_comment, column_type, java_type, java_field, is_pk, is_increment, is_required, is_insert, is_edit, is_list, is_query, query_type, html_type, dict_type, sort) VALUES(5, 'customer_name', '客户姓名', 'varchar(64)', 'String', 'customerName', '0', '0', '1', '1', '1', '1', '1', 'LIKE', 'input', null, 2);
INSERT INTO gen_table_column(table_id, column_name, column_comment, column_type, java_type, java_field, is_pk, is_increment, is_required, is_insert, is_edit, is_list, is_query, query_type, html_type, dict_type, sort) VALUES(5, 'phone', '手机号', 'varchar(20)', 'String', 'phone', '0', '0', '0', '1', '1', '1', '1', 'LIKE', 'input', null, 3);
INSERT INTO gen_table_column(table_id, column_name, column_comment, column_type, java_type, java_field, is_pk, is_increment, is_required, is_insert, is_edit, is_list, is_query, query_type, html_type, dict_type, sort) VALUES(5, 'email', '邮箱', 'varchar(64)', 'String', 'email', '0', '0', '0', '1', '1', '1', '0', 'EQ', 'input', null, 4);
INSERT INTO gen_table_column(table_id, column_name, column_comment, column_type, java_type, java_field, is_pk, is_increment, is_required, is_insert, is_edit, is_list, is_query, query_type, html_type, dict_type, sort) VALUES(5, 'status', '状态', 'char(1)', 'String', 'status', '0', '0', '0', '1', '1', '1', '1', 'EQ', 'radio', 'sys_normal_disable', 5);
INSERT INTO gen_table_column(table_id, column_name, column_comment, column_type, java_type, java_field, is_pk, is_increment, is_required, is_insert, is_edit, is_list, is_query, query_type, html_type, dict_type, sort) VALUES(5, 'remark', '备注', 'varchar(500)', 'String', 'remark', '0', '0', '0', '1', '1', '1', null, null, 'textarea', null, 6);
INSERT INTO gen_table_column(table_id, column_name, column_comment, column_type, java_type, java_field, is_pk, is_increment, is_required, is_insert, is_edit, is_list, is_query, query_type, html_type, dict_type, sort) VALUES(5, 'create_by', '创建者', 'varchar(64)', 'String', 'createBy', '0', '0', null, '1', null, null, null, null, null, null, 7);
INSERT INTO gen_table_column(table_id, column_name, column_comment, column_type, java_type, java_field, is_pk, is_increment, is_required, is_insert, is_edit, is_list, is_query, query_type, html_type, dict_type, sort) VALUES(5, 'create_time', '创建时间', 'datetime', 'Date', 'createTime', '0', '0', null, '1', null, '1', null, null, 'datetime', null, 8);
INSERT INTO gen_table_column(table_id, column_name, column_comment, column_type, java_type, java_field, is_pk, is_increment, is_required, is_insert, is_edit, is_list, is_query, query_type, html_type, dict_type, sort) VALUES(5, 'update_by', '更新者', 'varchar(64)', 'String', 'updateBy', '0', '0', null, '1', '1', null, null, null, null, null, 9);
INSERT INTO gen_table_column(table_id, column_name, column_comment, column_type, java_type, java_field, is_pk, is_increment, is_required, is_insert, is_edit, is_list, is_query, query_type, html_type, dict_type, sort) VALUES(5, 'update_time', '更新时间', 'datetime', 'Date', 'updateTime', '0', '0', null, '1', '1', null, null, null, null, null, 10);
