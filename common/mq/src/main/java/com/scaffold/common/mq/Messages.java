package com.scaffold.common.mq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.scaffold.common.trace.TraceContext;

/**
 * 消息解析工具（消费端）：兼容「信封」与「裸载荷」两种形态的平滑迁移。
 *
 * <p>发送侧迁移到 {@link MessageProducer} 后消息带 {@link MessageEnvelope} 信封，
 * 存量消息仍是裸 DTO JSON。本工具按结构自动识别：是信封则拆出 data 并恢复
 * traceId（跨服务链路串联），否则按裸 DTO 解析——消费端先上、发送端按节奏跟进，
 * 两端不需要同步发版。</p>
 *
 * <p>识别依据：顶层含 specVersion/id/data 三字段（CloudEvents 风格信封的固有结构，
 * 业务 DTO 出现同形字段碰撞时请在信封里挪 data 层规避）。</p>
 *
 * @author ct
 */
public final class Messages
{
    private static final Logger log = LoggerFactory.getLogger(Messages.class);

    private Messages()
    {
    }

    /**
     * 解析消息载荷：信封形态拆出 data 并恢复链路，裸载荷原样解析
     *
     * @param payload  消费端收到的字符串载荷
     * @param dataType 业务 DTO 类型
     * @return 业务对象；载荷为空时返回 null（由调用方决定丢弃策略）
     */
    public static <T> T unwrap(String payload, Class<T> dataType)
    {
        if (payload == null || payload.isEmpty())
        {
            return null;
        }
        JSONObject json = JSON.parseObject(payload);
        if (json == null)
        {
            return null;
        }
        if (json.containsKey("specVersion") && json.containsKey("id") && json.containsKey("data"))
        {
            String traceId = json.getString("traceId");
            if (traceId != null && !traceId.isEmpty())
            {
                TraceContext.bindOrNew(traceId);
            }
            return json.getObject("data", dataType);
        }
        return json.toJavaObject(dataType);
    }

    /**
     * 取信封消息ID（消费幂等键；裸载荷返回 null，调用方退回业务唯一键）
     */
    public static String envelopeId(String payload)
    {
        try
        {
            JSONObject json = JSON.parseObject(payload);
            if (json != null && json.containsKey("specVersion") && json.containsKey("id"))
            {
                return json.getString("id");
            }
        }
        catch (Exception e)
        {
            log.debug("[MQ] 非JSON载荷，无信封ID: {}", e.toString());
        }
        return null;
    }
}
