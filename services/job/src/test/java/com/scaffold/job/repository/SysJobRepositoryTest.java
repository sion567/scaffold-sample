package com.scaffold.job.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.scaffold.common.core.jpa.JpaSpecs;
import com.scaffold.common.test.H2JpaTest;
import com.scaffold.job.domain.SysJob;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

/**
 * SysJob 数据访问切片测试（JPA + H2）。
 * 覆盖：IDENTITY 主键回填、动态条件筛选、@Version 乐观锁、物理删除。
 *
 * @author ct
 */
@H2JpaTest(
        ddl = "sql/job/sys_job_h2.sql",
        entityPackages = "com.scaffold.job.domain")
class SysJobRepositoryTest
{
    private SysJob job(String name, String group, String invokeTarget, String status)
    {
        SysJob job = new SysJob();
        job.setJobName(name);
        job.setJobGroup(group);
        job.setInvokeTarget(invokeTarget);
        job.setCronExpression("0 0 * * * ?");
        job.setStatus(status);
        return job;
    }

    @Test
    @DisplayName("新增：IDENTITY 主键自动回填")
    void insert(SysJobRepository repository)
    {
        SysJob job = job("测试任务", "DEFAULT", "TestJob", "0");
        assertNull(job.getJobId());

        repository.saveAndFlush(job);

        assertNotNull(job.getJobId());
    }

    @Test
    @DisplayName("按 ID 查询：命中与未命中")
    void selectById(SysJobRepository repository)
    {
        SysJob job = job("查询测试任务", "DEFAULT", "SelectTestJob", "0");
        repository.saveAndFlush(job);

        assertEquals("查询测试任务", repository.findById(job.getJobId()).orElseThrow().getJobName());
        assertTrue(repository.findById(9999L).isEmpty());
    }

    @Test
    @DisplayName("动态筛选：jobName 模糊、jobGroup/status 精确、空串跳过")
    void listFilters(SysJobRepository repository)
    {
        repository.saveAndFlush(job("数据清理测试任务", "DEFAULT", "FilterTest", "0"));
        repository.saveAndFlush(job("系统组任务", "SYSTEM", "GroupFilter", "0"));
        repository.saveAndFlush(job("暂停状态任务", "DEFAULT", "StatusFilter", "1"));

        assertTrue(repository.list(JpaSpecs.likeIf("jobName", "清理"), Sort.unsorted())
                .stream().allMatch(j -> j.getJobName().contains("清理")));
        assertTrue(repository.list(JpaSpecs.eqIfNotBlank("jobGroup", "SYSTEM"), Sort.unsorted())
                .stream().allMatch(j -> "SYSTEM".equals(j.getJobGroup())));
        assertTrue(repository.list(JpaSpecs.eqIfNotBlank("status", "1"), Sort.unsorted())
                .stream().allMatch(j -> "1".equals(j.getStatus())));
        // 空串与 null 等价：全部命中
        assertEquals(repository.count(),
                repository.list(JpaSpecs.likeIf("jobName", null), Sort.unsorted()).size());
    }

    @Test
    @DisplayName("乐观锁：陈旧 version 更新抛冲突")
    void optimisticLock(SysJobRepository repository)
    {
        SysJob job = job("乐观锁任务", "DEFAULT", "LockTest", "0");
        repository.saveAndFlush(job);
        SysJob managed = repository.findById(job.getJobId()).orElseThrow();
        Integer currentVersion = managed.getVersion();

        // 陈旧版本（version+99）更新必须失败
        SysJob stale = new SysJob();
        stale.setJobId(job.getJobId());
        stale.setJobName("冲突");
        stale.setVersion(currentVersion + 99);
        // 切片环境为原生 Hibernate 异常；生产经 Spring 转译为
        // ObjectOptimisticLockingFailureException，服务层两者都兼容
        assertThrows(jakarta.persistence.OptimisticLockException.class,
                () -> repository.saveAndFlush(stale));

        // 正常版本更新成功且 version 自增
        managed.setJobName("修改后的任务");
        repository.saveAndFlush(managed);
        assertEquals("修改后的任务",
                repository.findById(job.getJobId()).orElseThrow().getJobName());
    }

    @Test
    @DisplayName("删除：单条与批量物理删除")
    void delete(SysJobRepository repository)
    {
        SysJob j1 = job("待删除任务", "DEFAULT", "DeleteTest", "0");
        SysJob j2 = job("批量删除1", "DEFAULT", "BatchDel1", "0");
        SysJob j3 = job("批量删除2", "DEFAULT", "BatchDel2", "0");
        repository.saveAllAndFlush(List.of(j1, j2, j3));

        repository.deleteById(j1.getJobId());
        assertTrue(repository.findById(j1.getJobId()).isEmpty());

        repository.deleteAllById(List.of(j2.getJobId(), j3.getJobId()));
        assertFalse(repository.existsById(j2.getJobId()));
        assertFalse(repository.existsById(j3.getJobId()));
    }
}
