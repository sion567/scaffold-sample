package com.scaffold.job.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.scaffold.common.core.jpa.JpaSpecs;
import com.scaffold.common.test.H2JpaTest;
import com.scaffold.job.domain.SysJobLog;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

/**
 * SysJobLog 数据访问切片测试（JPA + H2）。
 * 覆盖：IDENTITY 主键写入、动态条件筛选、单条/批量删除、整表清空。
 *
 * @author ct
 */
@H2JpaTest(
        ddl = "sql/job/sys_job_log_h2.sql",
        entityPackages = "com.scaffold.job.domain")
class SysJobLogRepositoryTest
{
    private SysJobLog log(String name, String group, String invokeTarget, String status)
    {
        SysJobLog log = new SysJobLog();
        log.setJobName(name);
        log.setJobGroup(group);
        log.setInvokeTarget(invokeTarget);
        log.setJobMessage("msg-" + name);
        log.setStatus(status);
        return log;
    }

    @Test
    @DisplayName("新增：IDENTITY 主键自动回填")
    void insert(SysJobLogRepository repository)
    {
        SysJobLog log = log("测试任务", "DEFAULT", "TestJob", "0");
        assertNull(log.getJobLogId());

        repository.saveAndFlush(log);

        assertNotNull(log.getJobLogId());
    }

    @Test
    @DisplayName("按 ID 查询：命中与未命中")
    void selectById(SysJobLogRepository repository)
    {
        SysJobLog log = log("查询测试日志", "DEFAULT", "SelectLog", "0");
        repository.saveAndFlush(log);

        assertEquals("查询测试日志",
                repository.findById(log.getJobLogId()).orElseThrow().getJobName());
        assertTrue(repository.findById(9999L).isEmpty());
    }

    @Test
    @DisplayName("动态筛选：jobName 模糊、jobGroup/status 精确、createTime 区间")
    void listFilters(SysJobLogRepository repository)
    {
        repository.saveAndFlush(log("数据清理日志", "DEFAULT", "FilterJob", "0"));
        repository.saveAndFlush(log("系统日志", "SYSTEM", "SystemJob", "0"));
        repository.saveAndFlush(log("失败日志", "DEFAULT", "FailJob", "1"));

        assertTrue(repository.list(JpaSpecs.likeIf("jobName", "清理"), Sort.unsorted())
                .stream().allMatch(l -> l.getJobName().contains("清理")));
        assertTrue(repository.list(JpaSpecs.eqIfNotBlank("jobGroup", "SYSTEM"), Sort.unsorted())
                .stream().allMatch(l -> "SYSTEM".equals(l.getJobGroup())));
        assertTrue(repository.list(JpaSpecs.eqIfNotBlank("status", "1"), Sort.unsorted())
                .stream().allMatch(l -> "1".equals(l.getStatus())));
        // createTime 区间：全表命中（create_time 由种子/审计填充）
        var all = repository.list(
                JpaSpecs.dateRangeIf("createTime", java.util.Map.of()), Sort.unsorted());
        assertEquals(repository.count(), all.size());
    }

    @Test
    @DisplayName("删除：单条与批量物理删除")
    void delete(SysJobLogRepository repository)
    {
        SysJobLog l1 = log("待删除日志", "DEFAULT", "DelJob", "0");
        SysJobLog l2 = log("批量删除1", "DEFAULT", "BatchDel1", "0");
        SysJobLog l3 = log("批量删除2", "DEFAULT", "BatchDel2", "0");
        repository.saveAllAndFlush(List.of(l1, l2, l3));

        repository.deleteById(l1.getJobLogId());
        assertTrue(repository.findById(l1.getJobLogId()).isEmpty());

        repository.deleteAllById(List.of(l2.getJobLogId(), l3.getJobLogId()));
        assertFalse(repository.existsById(l2.getJobLogId()));
        assertFalse(repository.existsById(l3.getJobLogId()));
    }

    @Test
    @DisplayName("清空：deleteAllInBatch 等价原 truncate")
    void clean(SysJobLogRepository repository)
    {
        repository.saveAndFlush(log("清空日志", "DEFAULT", "CleanJob", "0"));
        assertTrue(repository.count() >= 1);

        repository.deleteAllInBatch();

        assertEquals(0, repository.count());
    }
}
