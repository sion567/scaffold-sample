package com.scaffold.audit.repository;

import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.scaffold.audit.domain.AuditTrace;
import com.scaffold.common.core.jpa.ScaffoldRepository;

/**
 * 全链路业务留痕数据访问（audit_trace，仅追加）。
 *
 * @author ct
 */
@Repository
public interface AuditTraceRepository extends ScaffoldRepository<AuditTrace, Long>
{
    /**
     * 最新一条留痕的摘要（SM3 链尾；空表返回 null，链头 prev 视为空串）。
     */
    @Query("select t.currDigest from AuditTrace t where t.traceId = (select max(t2.traceId) from AuditTrace t2)")
    String selectLastDigest();

    /**
     * 链巡检：按 trace_id 升序取大于游标的一批（分批游标防大表全量加载 OOM）；
     * {@link Slice} 免 count 查询，{@code hasNext()}/{@code nextPageable()} 驱动游标推进。
     */
    Slice<AuditTrace> findByTraceIdGreaterThan(long afterId, Pageable pageable);
}
