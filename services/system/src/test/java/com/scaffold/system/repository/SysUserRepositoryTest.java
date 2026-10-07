package com.scaffold.system.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.scaffold.common.test.H2JpaTest;
import com.scaffold.system.api.domain.SysUser;

/**
 * SysUser 数据访问切片测试（JPA + H2 Oracle 模式）。
 * 覆盖：initAdminAccount 补空值语义（coalesce：已有值不覆盖）、@Modifying 修改查询可执行。
 * <p>
 * 背景：initAdminAccount 由启动期 CommandLineRunner 在无事务环境直接调用，
 * @Modifying 查询缺少事务会抛 InvalidDataAccessApiUsageException（Executing an
 * update/delete query）——所有 @Modifying 方法现已统一挂 @Transactional。
 * 注：本切片的 repository 参数由 H2JpaTestExtension 统一包事务，无法在切片内
 * 复现"缺事务"路径本身，事务行为由生产 Spring Data 代理保证。
 *
 * @author scaffold
 */
@H2JpaTest(
        ddl = "sql/system/sys_user_h2.sql",
        entityPackages = {"com.scaffold.system.domain", "com.scaffold.system.api.domain"})
class SysUserRepositoryTest
{
    @Test
    @DisplayName("initAdminAccount：仅补空值（coalesce），已有密码不覆盖，返回受影响行数")
    void initAdminAccountFillsOnlyEmpty(SysUserRepository repository)
    {
        int rows = repository.initAdminAccount("admin", "{sm3}new", "$SM4$1$dev-phone");
        assertEquals(1, rows);
        SysUser admin = repository.findByUserName("admin").orElseThrow();
        assertEquals("{sm3}seed", admin.getPassword(), "已有密码不得覆盖");
        assertEquals("$SM4$1$dev-phone", admin.getPhonenumber(), "空手机号被补全");
        assertNull(repository.findByUserName("tester").orElseThrow().getPhonenumber(),
                "其他账号不受影响");
    }

    @Test
    @DisplayName("initAdminAccount：账号不存在时返回 0")
    void initAdminAccountUnknownUser(SysUserRepository repository)
    {
        assertEquals(0, repository.initAdminAccount("ghost", "{sm3}x", "$SM4$1$y"));
    }

    @Test
    @DisplayName("updateStatus / softDeleteById：修改查询执行并生效")
    void modifyingQueriesApply(SysUserRepository repository)
    {
        assertEquals(1, repository.updateStatus(2L, "1"));
        assertEquals("1", repository.findByUserName("tester").orElseThrow().getStatus());

        assertEquals(1, repository.softDeleteById(2L));
        assertTrue(repository.findByUserNameAndDelFlag("tester", "0").isEmpty(), "软删除后按 delFlag=0 不可查");
    }
}
