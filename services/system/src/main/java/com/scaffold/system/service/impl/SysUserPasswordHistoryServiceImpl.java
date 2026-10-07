package com.scaffold.system.service.impl;

import java.util.Collections;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.scaffold.common.security.utils.SecurityUtils;
import com.scaffold.system.domain.SysUserPasswordHistory;
import com.scaffold.system.repository.SysUserPasswordHistoryRepository;
import com.scaffold.system.service.ISysUserPasswordHistoryService;

/**
 * 用户密码历史记录 服务层实现（JPA：Repository + Pageable 限条数）
 *
 * @author ct
 */
@Service
public class SysUserPasswordHistoryServiceImpl implements ISysUserPasswordHistoryService
{
    private final SysUserPasswordHistoryRepository passwordHistoryRepository;

    public SysUserPasswordHistoryServiceImpl(SysUserPasswordHistoryRepository passwordHistoryRepository)
    {
        this.passwordHistoryRepository = passwordHistoryRepository;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertPasswordHistory(Long userId, String encryptedPassword)
    {
        SysUserPasswordHistory history = new SysUserPasswordHistory();
        history.setUserId(userId);
        history.setPassword(encryptedPassword);
        history.setCreateTime(new java.util.Date());
        passwordHistoryRepository.save(history);
        return 1;
    }

    @Override
    public List<String> selectRecentPasswords(Long userId, int limit)
    {
        if (limit <= 0)
        {
            return Collections.emptyList();
        }
        return passwordHistoryRepository.findRecentPasswords(userId, PageRequest.of(0, limit));
    }

    @Override
    public boolean matchesRecentHistory(String rawPassword, Long userId, int historyCount)
    {
        if (historyCount <= 0 || rawPassword == null || rawPassword.isEmpty())
        {
            return false;
        }
        List<String> historyPasswords = selectRecentPasswords(userId, historyCount);
        for (String historyPassword : historyPasswords)
        {
            try
            {
                if (SecurityUtils.matchesPassword(rawPassword, historyPassword))
                {
                    return true;
                }
            }
            catch (IllegalArgumentException e)
            {
                // 历史数据中可能存在异常密文（如明文或旧格式），跳过该条
            }
        }
        return false;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int cleanupHistory(Long userId, int keepCount)
    {
        // 钳制非正值：无需保留任何历史时直接清理 0 条（等价原 deleteHistoryExceeding(keep<=0) 语义）
        if (keepCount <= 0)
        {
            return 0;
        }
        List<Long> keepIds = passwordHistoryRepository
                .findRecentIds(userId, PageRequest.of(0, keepCount));
        if (keepIds.isEmpty())
        {
            return 0;
        }
        return passwordHistoryRepository.deleteByUserIdAndIdNotIn(userId, keepIds);
    }
}
