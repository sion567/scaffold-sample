-- H2 建表 + 种子脚本：sys_dict_data
-- 注意：DICT_CODE 非自增，使用显式 ID
CREATE TABLE sys_dict_data (
    dict_code   BIGINT PRIMARY KEY,
    dict_sort   INT     DEFAULT 0,
    dict_label  VARCHAR(100),
    dict_value  VARCHAR(100),
    dict_type   VARCHAR(100),
    css_class  VARCHAR(100),
    list_class VARCHAR(100),
    is_default CHAR(1) DEFAULT 'N',
    status     CHAR(1) DEFAULT '0',
    create_by  VARCHAR(64),
    create_time TIMESTAMP,
    update_by  VARCHAR(64),
    update_time TIMESTAMP,
    remark     VARCHAR(500),
    version    BIGINT DEFAULT 0
);

INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, status, remark)
VALUES (1, 1, '男', '0', 'sys_user_sex', '0', '男性');

INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, status, remark)
VALUES (2, 2, '女', '1', 'sys_user_sex', '0', '女性');

INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, status, remark)
VALUES (3, 3, '未知', '2', 'sys_user_sex', '1', '未知性别');

INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, status, remark)
VALUES (4, 1, '显示', '0', 'sys_show_hide', '0', '显示');

ALTER TABLE sys_dict_data ALTER COLUMN dict_code RESTART WITH 100;
