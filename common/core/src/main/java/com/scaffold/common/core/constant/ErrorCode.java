package com.scaffold.common.core.constant;

/**
 * 通用错误码分段：
 * 0 成功 / 1xxx 通用（参数、鉴权、状态、限流）/ 9xxx 系统内部。
 *
 * <p>业务模块可自行领取私有号段（如 2xxx 起），携带方式：
 * {@code new ServiceException(message, code)}——统一错误响应结构
 * code/message/traceId 中 code 即此值，message 为人读文案。</p>
 *
 * @author scaffold
 */
public class ErrorCode
{
    // ── 1xxx 通用（参数、鉴权、状态、限流） ──

    /** 通用参数缺失/非法 */
    public static final int COMMON_PARAM = 1001;

    /** 通用：目标对象不存在 */
    public static final int COMMON_NOT_FOUND = 1002;

    /** 通用：状态不允许该操作 */
    public static final int COMMON_STATE = 1003;
}
