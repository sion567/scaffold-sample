package com.scaffold.audit.repository;

import java.util.Date;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.scaffold.common.core.jpa.ScaffoldRepository;
import com.scaffold.system.api.domain.SysLogininfor;

/**
 * 登录日志数据访问（audit_logininfor，仅追加）。
 *
 * @author ct
 */
@Repository
public interface LogininforRepository extends ScaffoldRepository<SysLogininfor, Long>
{
    /**
     * 登录失败 TOP N（按 IP+用户聚合；top-N 的方言 LIMIT 由 Hibernate 按 Pageable 渲染）。
     */
    @Query("""
            select l.ipaddr as ip, l.userName as username, count(l) as failCount,
                   max(l.accessTime) as lastFailTime
            from SysLogininfor l
            where l.status = '1' and l.accessTime >= :before
            group by l.ipaddr, l.userName
            order by count(l) desc
            """)
    List<LoginFailStat> selectLoginFailTop10(@Param("before") Date before, Pageable topN);

    /**
     * 登录失败聚合行投影（字段名即对外 JSON 键，保持原 Map 输出形态）。
     */
    interface LoginFailStat
    {
        String getIp();

        String getUsername();

        Long getFailCount();

        Date getLastFailTime();
    }
}
