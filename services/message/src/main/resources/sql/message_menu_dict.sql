-- =====================================================================
-- scaffold-message 消息中心：菜单/权限/字典 SQL（运维手工执行，SYSTEM_DB）
-- docs/消息中心模块设计文档.md §5.4 / §8.4
-- 字典主键为显式 ID（dict_code 非自增），取 9000+ 段避免与 scaffold-system 种子冲突；
-- 若段位已被占用请整体平移。菜单 @parentId 用 SELECT 回填。
-- =====================================================================

-- ======== 一、字典（sys_dict_type / sys_dict_data）========

-- 短信渠道类型
INSERT INTO sys_dict_type (dict_id, dict_name, dict_type, status, create_by, remark)
VALUES (9001, '短信渠道类型', 'message_sms_channel_type', '0', 'admin', '消息中心：短信渠道厂商类型');
INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, status, create_by) VALUES (900101, 1, '阿里云', 'ALIYUN',   'message_sms_channel_type', '0', 'admin');
INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, status, create_by) VALUES (900102, 2, '腾讯云', 'TENCENT',  'message_sms_channel_type', '0', 'admin');
INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, status, create_by) VALUES (900103, 3, '华为云', 'HUAWEI',   'message_sms_channel_type', '0', 'admin');
INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, status, create_by) VALUES (900104, 4, '天翼云', 'CTYUN',    'message_sms_channel_type', '0', 'admin');
INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, status, create_by) VALUES (900105, 5, '模拟',   'MOCK',     'message_sms_channel_type', '0', 'admin');

-- 短信用途分类
INSERT INTO sys_dict_type (dict_id, dict_name, dict_type, status, create_by, remark)
VALUES (9002, '短信用途分类', 'message_sms_category', '0', 'admin', '消息中心：短信模板用途分类');
INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, status, create_by) VALUES (900201, 1, '验证码', 'VERIFY',    'message_sms_category', '0', 'admin');
INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, status, create_by) VALUES (900202, 2, '通知',   'NOTIFY',    'message_sms_category', '0', 'admin');
INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, status, create_by) VALUES (900203, 3, '营销',   'MARKETING', 'message_sms_category', '0', 'admin');

-- 消息发送状态
INSERT INTO sys_dict_type (dict_id, dict_name, dict_type, status, create_by, remark)
VALUES (9003, '消息发送状态', 'message_send_status', '0', 'admin', '消息中心：发送日志状态机');
INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, status, create_by) VALUES (900301, 1, '待发送', 'PENDING', 'message_send_status', '0', 'admin');
INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, status, create_by) VALUES (900302, 2, '发送中', 'SENDING', 'message_send_status', '0', 'admin');
INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, status, create_by) VALUES (900303, 3, '成功',   'SUCCESS', 'message_send_status', '0', 'admin');
INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, status, create_by) VALUES (900304, 4, '失败',   'FAILED',  'message_send_status', '0', 'admin');

-- 邮件用途分类
INSERT INTO sys_dict_type (dict_id, dict_name, dict_type, status, create_by, remark)
VALUES (9004, '邮件用途分类', 'message_mail_category', '0', 'admin', '消息中心：邮件模板用途分类');
INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, status, create_by) VALUES (900401, 1, '通知', 'NOTIFY',         'message_mail_category', '0', 'admin');
INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, status, create_by) VALUES (900402, 2, '报表', 'REPORT',         'message_mail_category', '0', 'admin');
INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, status, create_by) VALUES (900403, 3, '公函', 'NOTIFY_OFFICIAL','message_mail_category', '0', 'admin');

-- 站内信用途分类
INSERT INTO sys_dict_type (dict_id, dict_name, dict_type, status, create_by, remark)
VALUES (9005, '站内信用途分类', 'message_inner_category', '0', 'admin', '消息中心：站内信模板用途分类');
INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, status, create_by) VALUES (900501, 1, '系统', 'SYSTEM', 'message_inner_category', '0', 'admin');
INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, status, create_by) VALUES (900502, 2, '预警', 'ALERT',  'message_inner_category', '0', 'admin');
INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, status, create_by) VALUES (900503, 3, '业务', 'BIZ',    'message_inner_category', '0', 'admin');

-- 站内信已读状态
INSERT INTO sys_dict_type (dict_id, dict_name, dict_type, status, create_by, remark)
VALUES (9006, '站内信已读状态', 'message_read_flag', '0', 'admin', '消息中心：站内信已读标记');
INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, status, create_by) VALUES (900601, 1, '未读', '0', 'message_read_flag', '0', 'admin');
INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, status, create_by) VALUES (900602, 2, '已读', '1', 'message_read_flag', '0', 'admin');

-- ======== 二、菜单与权限（sys_menu）========

-- 一级目录：消息中心
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by)
VALUES ('消息中心', 0, 5, 'message', NULL, 'M', '0', '0', '', 'message', 'admin');

-- @messageId 回填
-- 二级菜单（component 为 views 相对路径）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by)
SELECT '短信渠道', m.menu_id, 1, 'smsChannel', 'message/sms/channel/index', 'C', '0', '0', 'message:smsChannel:list', 'phone', 'admin' FROM sys_menu m WHERE m.path = 'message' AND m.parent_id = 0;
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by)
SELECT '短信模板', m.menu_id, 2, 'smsTemplate', 'message/sms/template/index', 'C', '0', '0', 'message:smsTemplate:list', 'documentation', 'admin' FROM sys_menu m WHERE m.path = 'message' AND m.parent_id = 0;
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by)
SELECT '短信日志', m.menu_id, 3, 'smsLog', 'message/sms/log/index', 'C', '0', '0', 'message:smsLog:list', 'log', 'admin' FROM sys_menu m WHERE m.path = 'message' AND m.parent_id = 0;
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by)
SELECT '邮箱账号', m.menu_id, 4, 'mailAccount', 'message/mail/account/index', 'C', '0', '0', 'message:mailAccount:list', 'email', 'admin' FROM sys_menu m WHERE m.path = 'message' AND m.parent_id = 0;
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by)
SELECT '邮件模板', m.menu_id, 5, 'mailTemplate', 'message/mail/template/index', 'C', '0', '0', 'message:mailTemplate:list', 'edit', 'admin' FROM sys_menu m WHERE m.path = 'message' AND m.parent_id = 0;
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by)
SELECT '邮件记录', m.menu_id, 6, 'mailLog', 'message/mail/log/index', 'C', '0', '0', 'message:mailLog:list', 'log', 'admin' FROM sys_menu m WHERE m.path = 'message' AND m.parent_id = 0;
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by)
SELECT '站内信模板', m.menu_id, 7, 'innerTemplate', 'message/inner/template/index', 'C', '0', '0', 'message:innerTemplate:list', 'message', 'admin' FROM sys_menu m WHERE m.path = 'message' AND m.parent_id = 0;
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by)
SELECT '消息记录', m.menu_id, 8, 'innerMessage', 'message/inner/message/index', 'C', '0', '0', 'message:innerMessage:list', 'list', 'admin' FROM sys_menu m WHERE m.path = 'message' AND m.parent_id = 0;
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by)
SELECT '我的消息', m.menu_id, 9, 'my', 'message/inner/my', 'C', '0', '0', '', 'message', 'admin' FROM sys_menu m WHERE m.path = 'message' AND m.parent_id = 0;

-- 通知公告归组到消息中心（component/权限串/角色分配不变）
UPDATE sys_menu SET parent_id = (SELECT menu_id FROM sys_menu WHERE path = 'message' AND parent_id = 0)
WHERE perms = 'system:notice:list' AND menu_type = 'C';

-- F 型按钮权限（§12.1 清单；parent 取各自菜单 menu_id）
-- 短信渠道
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by)
SELECT '渠道查询', m.menu_id, 1, '', '', 'F', '0', '0', 'message:smsChannel:query', '#', 'admin' FROM sys_menu m WHERE m.perms = 'message:smsChannel:list';
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by)
SELECT '渠道新增', m.menu_id, 2, '', '', 'F', '0', '0', 'message:smsChannel:add', '#', 'admin' FROM sys_menu m WHERE m.perms = 'message:smsChannel:list';
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by)
SELECT '渠道修改', m.menu_id, 3, '', '', 'F', '0', '0', 'message:smsChannel:edit', '#', 'admin' FROM sys_menu m WHERE m.perms = 'message:smsChannel:list';
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by)
SELECT '渠道删除', m.menu_id, 4, '', '', 'F', '0', '0', 'message:smsChannel:remove', '#', 'admin' FROM sys_menu m WHERE m.perms = 'message:smsChannel:list';

-- 短信模板
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by)
SELECT '短信模板查询', m.menu_id, 1, '', '', 'F', '0', '0', 'message:smsTemplate:query', '#', 'admin' FROM sys_menu m WHERE m.perms = 'message:smsTemplate:list';
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by)
SELECT '短信模板新增', m.menu_id, 2, '', '', 'F', '0', '0', 'message:smsTemplate:add', '#', 'admin' FROM sys_menu m WHERE m.perms = 'message:smsTemplate:list';
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by)
SELECT '短信模板修改', m.menu_id, 3, '', '', 'F', '0', '0', 'message:smsTemplate:edit', '#', 'admin' FROM sys_menu m WHERE m.perms = 'message:smsTemplate:list';
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by)
SELECT '短信模板删除', m.menu_id, 4, '', '', 'F', '0', '0', 'message:smsTemplate:remove', '#', 'admin' FROM sys_menu m WHERE m.perms = 'message:smsTemplate:list';

-- 短信日志
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by)
SELECT '短信日志查询', m.menu_id, 1, '', '', 'F', '0', '0', 'message:smsLog:query', '#', 'admin' FROM sys_menu m WHERE m.perms = 'message:smsLog:list';
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by)
SELECT '短信日志重发', m.menu_id, 2, '', '', 'F', '0', '0', 'message:smsLog:edit', '#', 'admin' FROM sys_menu m WHERE m.perms = 'message:smsLog:list';
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by)
SELECT '短信日志删除', m.menu_id, 3, '', '', 'F', '0', '0', 'message:smsLog:remove', '#', 'admin' FROM sys_menu m WHERE m.perms = 'message:smsLog:list';
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by)
SELECT '短信日志导出', m.menu_id, 4, '', '', 'F', '0', '0', 'message:smsLog:export', '#', 'admin' FROM sys_menu m WHERE m.perms = 'message:smsLog:list';

-- 邮箱账号
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by)
SELECT '账号查询', m.menu_id, 1, '', '', 'F', '0', '0', 'message:mailAccount:query', '#', 'admin' FROM sys_menu m WHERE m.perms = 'message:mailAccount:list';
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by)
SELECT '账号新增', m.menu_id, 2, '', '', 'F', '0', '0', 'message:mailAccount:add', '#', 'admin' FROM sys_menu m WHERE m.perms = 'message:mailAccount:list';
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by)
SELECT '账号修改', m.menu_id, 3, '', '', 'F', '0', '0', 'message:mailAccount:edit', '#', 'admin' FROM sys_menu m WHERE m.perms = 'message:mailAccount:list';
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by)
SELECT '账号删除', m.menu_id, 4, '', '', 'F', '0', '0', 'message:mailAccount:remove', '#', 'admin' FROM sys_menu m WHERE m.perms = 'message:mailAccount:list';

-- 邮件模板
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by)
SELECT '邮件模板查询', m.menu_id, 1, '', '', 'F', '0', '0', 'message:mailTemplate:query', '#', 'admin' FROM sys_menu m WHERE m.perms = 'message:mailTemplate:list';
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by)
SELECT '邮件模板新增', m.menu_id, 2, '', '', 'F', '0', '0', 'message:mailTemplate:add', '#', 'admin' FROM sys_menu m WHERE m.perms = 'message:mailTemplate:list';
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by)
SELECT '邮件模板修改', m.menu_id, 3, '', '', 'F', '0', '0', 'message:mailTemplate:edit', '#', 'admin' FROM sys_menu m WHERE m.perms = 'message:mailTemplate:list';
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by)
SELECT '邮件模板删除', m.menu_id, 4, '', '', 'F', '0', '0', 'message:mailTemplate:remove', '#', 'admin' FROM sys_menu m WHERE m.perms = 'message:mailTemplate:list';

-- 邮件记录
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by)
SELECT '邮件记录查询', m.menu_id, 1, '', '', 'F', '0', '0', 'message:mailLog:query', '#', 'admin' FROM sys_menu m WHERE m.perms = 'message:mailLog:list';
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by)
SELECT '邮件记录重发', m.menu_id, 2, '', '', 'F', '0', '0', 'message:mailLog:edit', '#', 'admin' FROM sys_menu m WHERE m.perms = 'message:mailLog:list';
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by)
SELECT '邮件记录删除', m.menu_id, 3, '', '', 'F', '0', '0', 'message:mailLog:remove', '#', 'admin' FROM sys_menu m WHERE m.perms = 'message:mailLog:list';
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by)
SELECT '邮件记录导出', m.menu_id, 4, '', '', 'F', '0', '0', 'message:mailLog:export', '#', 'admin' FROM sys_menu m WHERE m.perms = 'message:mailLog:list';

-- 站内信模板
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by)
SELECT '站内信模板查询', m.menu_id, 1, '', '', 'F', '0', '0', 'message:innerTemplate:query', '#', 'admin' FROM sys_menu m WHERE m.perms = 'message:innerTemplate:list';
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by)
SELECT '站内信模板新增', m.menu_id, 2, '', '', 'F', '0', '0', 'message:innerTemplate:add', '#', 'admin' FROM sys_menu m WHERE m.perms = 'message:innerTemplate:list';
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by)
SELECT '站内信模板修改', m.menu_id, 3, '', '', 'F', '0', '0', 'message:innerTemplate:edit', '#', 'admin' FROM sys_menu m WHERE m.perms = 'message:innerTemplate:list';
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by)
SELECT '站内信模板删除', m.menu_id, 4, '', '', 'F', '0', '0', 'message:innerTemplate:remove', '#', 'admin' FROM sys_menu m WHERE m.perms = 'message:innerTemplate:list';

-- 消息记录（管理）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by)
SELECT '站内信发送', m.menu_id, 1, '', '', 'F', '0', '0', 'message:innerMessage:send', '#', 'admin' FROM sys_menu m WHERE m.perms = 'message:innerMessage:list';
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by)
SELECT '站内信撤回', m.menu_id, 2, '', '', 'F', '0', '0', 'message:innerMessage:remove', '#', 'admin' FROM sys_menu m WHERE m.perms = 'message:innerMessage:list';
