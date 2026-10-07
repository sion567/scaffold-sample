package com.scaffold.message.repository;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.scaffold.common.core.jpa.ScaffoldRepository;
import com.scaffold.message.domain.MsgMailAccount;

/**
 * 邮件账号数据访问（msg_mail_account）。
 *
 * @author ct
 */
@Repository
public interface MsgMailAccountRepository extends ScaffoldRepository<MsgMailAccount, Long>
{
    /** 邮箱地址唯一查询（未删除） */
    MsgMailAccount findByEmailAddrAndDelFlag(String emailAddr, String delFlag);

    /** 默认发信账号（状态正常、未删除，按 ID 升序取首个） */
    List<MsgMailAccount> findByStatusAndDelFlagOrderByIdAsc(String status, String delFlag);
}
