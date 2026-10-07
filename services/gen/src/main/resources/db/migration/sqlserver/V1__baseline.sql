-- services/gen sqlserver 方言家族基线（由 scripts/gen_dialect_migrations.py 从 h2 基线生成，勿手工编辑；
-- GENERATED-BY gen_dialect_migrations.py
-- 修正应改脚本映射表后 --force 重新生成；真实库接入回归见 docs/multi-database-guide.md §6）


CREATE TABLE gen_table (
    table_id BIGINT IDENTITY(100,1),
    table_name NVARCHAR(200) DEFAULT '',
    table_comment NVARCHAR(500) DEFAULT '',
    sub_table_name NVARCHAR(64),
    sub_table_fk_name NVARCHAR(64),
    class_name NVARCHAR(100) DEFAULT '',
    tpl_category NVARCHAR(200) DEFAULT 'crud',
    tpl_web_type NVARCHAR(30) DEFAULT '',
    package_name NVARCHAR(100),
    module_name NVARCHAR(30),
    business_name NVARCHAR(30),
    function_name NVARCHAR(50),
    function_author NVARCHAR(50),
    form_col_num INT DEFAULT 1,
    gen_type NCHAR(1) DEFAULT '0',
    gen_path NVARCHAR(200) DEFAULT '/',
    options NVARCHAR(1000),
    create_by NVARCHAR(64) DEFAULT '',
    create_time DATETIME2,
    update_by NVARCHAR(64) DEFAULT '',
    update_time DATETIME2,
    remark NVARCHAR(500),
    CONSTRAINT pk_gen_table PRIMARY KEY (table_id)
);
EXEC sp_addextendedproperty @name=N'MS_Description', @value=N'编号', @level0type=N'SCHEMA', @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'gen_table', @level2type=N'COLUMN', @level2name=N'table_id';
EXEC sp_addextendedproperty @name=N'MS_Description', @value=N'表名称', @level0type=N'SCHEMA', @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'gen_table', @level2type=N'COLUMN', @level2name=N'table_name';
EXEC sp_addextendedproperty @name=N'MS_Description', @value=N'表描述', @level0type=N'SCHEMA', @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'gen_table', @level2type=N'COLUMN', @level2name=N'table_comment';
EXEC sp_addextendedproperty @name=N'MS_Description', @value=N'关联子表的表名', @level0type=N'SCHEMA', @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'gen_table', @level2type=N'COLUMN', @level2name=N'sub_table_name';
EXEC sp_addextendedproperty @name=N'MS_Description', @value=N'子表关联的外键名', @level0type=N'SCHEMA', @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'gen_table', @level2type=N'COLUMN', @level2name=N'sub_table_fk_name';
EXEC sp_addextendedproperty @name=N'MS_Description', @value=N'实体类名称', @level0type=N'SCHEMA', @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'gen_table', @level2type=N'COLUMN', @level2name=N'class_name';
EXEC sp_addextendedproperty @name=N'MS_Description', @value=N'使用的模板（crud单表操作 tree树表操作）', @level0type=N'SCHEMA', @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'gen_table', @level2type=N'COLUMN', @level2name=N'tpl_category';
EXEC sp_addextendedproperty @name=N'MS_Description', @value=N'前端模板类型（element-ui模版 element-plus模版）', @level0type=N'SCHEMA', @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'gen_table', @level2type=N'COLUMN', @level2name=N'tpl_web_type';
EXEC sp_addextendedproperty @name=N'MS_Description', @value=N'生成包路径', @level0type=N'SCHEMA', @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'gen_table', @level2type=N'COLUMN', @level2name=N'package_name';
EXEC sp_addextendedproperty @name=N'MS_Description', @value=N'生成模块名', @level0type=N'SCHEMA', @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'gen_table', @level2type=N'COLUMN', @level2name=N'module_name';
EXEC sp_addextendedproperty @name=N'MS_Description', @value=N'生成业务名', @level0type=N'SCHEMA', @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'gen_table', @level2type=N'COLUMN', @level2name=N'business_name';
EXEC sp_addextendedproperty @name=N'MS_Description', @value=N'生成功能名', @level0type=N'SCHEMA', @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'gen_table', @level2type=N'COLUMN', @level2name=N'function_name';
EXEC sp_addextendedproperty @name=N'MS_Description', @value=N'生成功能作者', @level0type=N'SCHEMA', @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'gen_table', @level2type=N'COLUMN', @level2name=N'function_author';
EXEC sp_addextendedproperty @name=N'MS_Description', @value=N'表单布局（单列 双列 三列）', @level0type=N'SCHEMA', @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'gen_table', @level2type=N'COLUMN', @level2name=N'form_col_num';
EXEC sp_addextendedproperty @name=N'MS_Description', @value=N'生成代码方式（0zip压缩包 1自定义路径）', @level0type=N'SCHEMA', @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'gen_table', @level2type=N'COLUMN', @level2name=N'gen_type';
EXEC sp_addextendedproperty @name=N'MS_Description', @value=N'生成路径（不填默认项目路径）', @level0type=N'SCHEMA', @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'gen_table', @level2type=N'COLUMN', @level2name=N'gen_path';
EXEC sp_addextendedproperty @name=N'MS_Description', @value=N'其它生成选项', @level0type=N'SCHEMA', @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'gen_table', @level2type=N'COLUMN', @level2name=N'options';
EXEC sp_addextendedproperty @name=N'MS_Description', @value=N'创建者', @level0type=N'SCHEMA', @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'gen_table', @level2type=N'COLUMN', @level2name=N'create_by';
EXEC sp_addextendedproperty @name=N'MS_Description', @value=N'创建时间', @level0type=N'SCHEMA', @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'gen_table', @level2type=N'COLUMN', @level2name=N'create_time';
EXEC sp_addextendedproperty @name=N'MS_Description', @value=N'更新者', @level0type=N'SCHEMA', @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'gen_table', @level2type=N'COLUMN', @level2name=N'update_by';
EXEC sp_addextendedproperty @name=N'MS_Description', @value=N'更新时间', @level0type=N'SCHEMA', @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'gen_table', @level2type=N'COLUMN', @level2name=N'update_time';
EXEC sp_addextendedproperty @name=N'MS_Description', @value=N'备注', @level0type=N'SCHEMA', @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'gen_table', @level2type=N'COLUMN', @level2name=N'remark';



CREATE TABLE gen_table_column (
    column_id BIGINT IDENTITY(100,1),
    table_id BIGINT,
    column_name NVARCHAR(200),
    column_comment NVARCHAR(500),
    column_type NVARCHAR(100),
    java_type NVARCHAR(500),
    java_field NVARCHAR(200),
    is_pk NCHAR(1),
    is_increment NCHAR(1),
    is_required NCHAR(1),
    is_insert NCHAR(1),
    is_edit NCHAR(1),
    is_list NCHAR(1),
    is_query NCHAR(1),
    query_type NVARCHAR(200) DEFAULT 'EQ',
    html_type NVARCHAR(200),
    dict_type NVARCHAR(200) DEFAULT '',
    sort BIGINT,
    create_by NVARCHAR(64) DEFAULT '',
    create_time DATETIME2,
    update_by NVARCHAR(64) DEFAULT '',
    update_time DATETIME2,
    CONSTRAINT pk_gen_table_column PRIMARY KEY (column_id)
);
EXEC sp_addextendedproperty @name=N'MS_Description', @value=N'编号', @level0type=N'SCHEMA', @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'gen_table_column', @level2type=N'COLUMN', @level2name=N'column_id';
EXEC sp_addextendedproperty @name=N'MS_Description', @value=N'归属表编号', @level0type=N'SCHEMA', @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'gen_table_column', @level2type=N'COLUMN', @level2name=N'table_id';
EXEC sp_addextendedproperty @name=N'MS_Description', @value=N'列名称', @level0type=N'SCHEMA', @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'gen_table_column', @level2type=N'COLUMN', @level2name=N'column_name';
EXEC sp_addextendedproperty @name=N'MS_Description', @value=N'列描述', @level0type=N'SCHEMA', @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'gen_table_column', @level2type=N'COLUMN', @level2name=N'column_comment';
EXEC sp_addextendedproperty @name=N'MS_Description', @value=N'列类型', @level0type=N'SCHEMA', @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'gen_table_column', @level2type=N'COLUMN', @level2name=N'column_type';
EXEC sp_addextendedproperty @name=N'MS_Description', @value=N'JAVA类型', @level0type=N'SCHEMA', @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'gen_table_column', @level2type=N'COLUMN', @level2name=N'java_type';
EXEC sp_addextendedproperty @name=N'MS_Description', @value=N'JAVA字段名', @level0type=N'SCHEMA', @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'gen_table_column', @level2type=N'COLUMN', @level2name=N'java_field';
EXEC sp_addextendedproperty @name=N'MS_Description', @value=N'是否主键（1是）', @level0type=N'SCHEMA', @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'gen_table_column', @level2type=N'COLUMN', @level2name=N'is_pk';
EXEC sp_addextendedproperty @name=N'MS_Description', @value=N'是否自增（1是）', @level0type=N'SCHEMA', @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'gen_table_column', @level2type=N'COLUMN', @level2name=N'is_increment';
EXEC sp_addextendedproperty @name=N'MS_Description', @value=N'是否必填（1是）', @level0type=N'SCHEMA', @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'gen_table_column', @level2type=N'COLUMN', @level2name=N'is_required';
EXEC sp_addextendedproperty @name=N'MS_Description', @value=N'是否为插入字段（1是）', @level0type=N'SCHEMA', @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'gen_table_column', @level2type=N'COLUMN', @level2name=N'is_insert';
EXEC sp_addextendedproperty @name=N'MS_Description', @value=N'是否编辑字段（1是）', @level0type=N'SCHEMA', @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'gen_table_column', @level2type=N'COLUMN', @level2name=N'is_edit';
EXEC sp_addextendedproperty @name=N'MS_Description', @value=N'是否列表字段（1是）', @level0type=N'SCHEMA', @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'gen_table_column', @level2type=N'COLUMN', @level2name=N'is_list';
EXEC sp_addextendedproperty @name=N'MS_Description', @value=N'是否查询字段（1是）', @level0type=N'SCHEMA', @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'gen_table_column', @level2type=N'COLUMN', @level2name=N'is_query';
EXEC sp_addextendedproperty @name=N'MS_Description', @value=N'查询方式（等于、不等于、大于、小于、范围）', @level0type=N'SCHEMA', @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'gen_table_column', @level2type=N'COLUMN', @level2name=N'query_type';
EXEC sp_addextendedproperty @name=N'MS_Description', @value=N'显示类型（文本框、文本域、下拉框、复选框、单选框、日期控件）', @level0type=N'SCHEMA', @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'gen_table_column', @level2type=N'COLUMN', @level2name=N'html_type';
EXEC sp_addextendedproperty @name=N'MS_Description', @value=N'字典类型', @level0type=N'SCHEMA', @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'gen_table_column', @level2type=N'COLUMN', @level2name=N'dict_type';
EXEC sp_addextendedproperty @name=N'MS_Description', @value=N'排序', @level0type=N'SCHEMA', @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'gen_table_column', @level2type=N'COLUMN', @level2name=N'sort';
EXEC sp_addextendedproperty @name=N'MS_Description', @value=N'创建者', @level0type=N'SCHEMA', @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'gen_table_column', @level2type=N'COLUMN', @level2name=N'create_by';
EXEC sp_addextendedproperty @name=N'MS_Description', @value=N'创建时间', @level0type=N'SCHEMA', @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'gen_table_column', @level2type=N'COLUMN', @level2name=N'create_time';
EXEC sp_addextendedproperty @name=N'MS_Description', @value=N'更新者', @level0type=N'SCHEMA', @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'gen_table_column', @level2type=N'COLUMN', @level2name=N'update_by';
EXEC sp_addextendedproperty @name=N'MS_Description', @value=N'更新时间', @level0type=N'SCHEMA', @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'gen_table_column', @level2type=N'COLUMN', @level2name=N'update_time';


-- ===================================================================
-- 以下并入原 V2__seed_sample_gen_table.sql（项目未上线，2026-09 合并为单基线）：
-- gen 案例：样例客户表的生成元数据种子，登录后台可直接预览/下载 SAMPLE_CUSTOMER 生成代码
SET IDENTITY_INSERT gen_table ON;
INSERT INTO gen_table(table_id, table_name, table_comment, class_name, tpl_category, tpl_web_type, package_name, module_name, business_name, function_name, function_author, form_col_num, gen_type, gen_path, create_by, create_time, remark) VALUES(5, 'SAMPLE_CUSTOMER', '样例客户表', 'SampleCustomer', 'crud', 'element-plus', 'com.scaffold.sample', 'sample', 'customer', '样例客户', 'scaffold', 1, '0', '/', 'admin', SYSDATETIME(), '单表 CRUD 生成案例');

SET IDENTITY_INSERT gen_table OFF;
SET IDENTITY_INSERT gen_table_column ON;
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
SET IDENTITY_INSERT gen_table_column OFF;
