package com.scaffold.message.repository;

import java.util.Date;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.scaffold.common.core.jpa.ScaffoldRepository;
import com.scaffold.message.domain.MsgInnerMessage;

/**
 * 站内信数据访问（msg_inner_message）。
 *
 * @author ct
 */
@Repository
public interface MsgInnerMessageRepository extends ScaffoldRepository<MsgInnerMessage, Long>
{
    /** 幂等键查询 */
    MsgInnerMessage findByBizTypeAndBizIdAndTemplateCodeAndReceiverIdAndIdempotentKey(
            String bizType, String bizId, String templateCode, Long receiverId, String idempotentKey);

    /** 未读数（铃铛轮询，索引 RECEIVER_ID+READ_FLAG） */
    long countByReceiverIdAndReadFlag(Long receiverId, String readFlag);

    /** 单条已读（校验收件人，未读才更新，返回受影响行数） */
    @Modifying
    @Query("update MsgInnerMessage m set m.readFlag = '1', m.readTime = :now, m.version = m.version + 1"
            + " where m.id = :id and m.receiverId = :receiverId and m.readFlag = '0'")
    int markRead(@Param("id") Long id, @Param("receiverId") Long receiverId, @Param("now") Date now);

    /** 全部已读 */
    @Modifying
    @Query("update MsgInnerMessage m set m.readFlag = '1', m.readTime = :now, m.version = m.version + 1"
            + " where m.receiverId = :receiverId and m.readFlag = '0'")
    int markAllRead(@Param("receiverId") Long receiverId, @Param("now") Date now);
}
