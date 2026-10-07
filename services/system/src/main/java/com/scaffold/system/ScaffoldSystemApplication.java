package com.scaffold.system;

import com.scaffold.common.core.utils.StringUtils;
import com.scaffold.common.core.utils.crypto.Sm4FieldCrypto;
import com.scaffold.common.security.utils.SecurityUtils;
import com.scaffold.system.api.domain.SysUser;
import com.scaffold.system.repository.SysUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import com.scaffold.common.security.annotation.EnableCustomConfig;

/**
 * 系统管理服务
 *
 * @author scaffold
 */
@EnableCustomConfig
// 国密能力（GmFieldCrypto/FileSymmetricKeyProvider/GmIdentity）由 scaffold-spring-boot-starter-gm
// 自动装配，SM4 字段加密（Sm4FieldCrypto 静态门面）依赖其启动注入
@SpringBootApplication
public class ScaffoldSystemApplication
{
    private static final Logger log = LoggerFactory.getLogger(ScaffoldSystemApplication.class);

    /** 内置管理员默认手机号（明文仅存在于本常量与配置中心，落库一律 SM4 密文） */
    private static final String ADMIN_DEFAULT_PHONE = "15888888888";

    public static void main(String[] args)
    {
        SpringApplication.run(ScaffoldSystemApplication.class, args);
        System.out.println("系统管理服务启动成功");
    }

    /**
     * 启动时初始化内置管理员（admin）的默认密码与手机号：
     * 密码用 SM3 加盐迭代哈希（salt$hash，SecurityUtils.encryptPassword），
     * 手机号用 SM4 版本化密文（$SM4$...，见 Sm4FieldCrypto）。
     * 经 initAdminAccount 按账号一次性补全，仅写为空字段，已有值一律不覆盖，
     * 保证管理员改过的密码/手机号不被重启重置。依赖 gm.* 配置（见 scaffold-system-local.yml）。
     */
    @Bean
    CommandLineRunner initAdminPasswordRunner(
        SysUserRepository userRepository,
        @Value("${scaffold.system.admin.default-password:admin}") String defaultPassword)
    {
        return args ->
        {
            SysUser admin = userRepository.findByUserNameAndDelFlag("admin", "0").orElse(null);
            if (admin == null)
            {
                log.warn("内置管理员账户(admin)不存在，跳过默认密码/手机号初始化");
                return;
            }
            if (StringUtils.isEmpty(admin.getPassword()) || StringUtils.isEmpty(admin.getPhonenumber()))
            {
                var pwd = SecurityUtils.encryptPassword(defaultPassword);
                System.out.println("PWD:" + pwd);
                int result = userRepository.initAdminAccount(admin.getUserName(),
                    pwd, Sm4FieldCrypto.encrypt(ADMIN_DEFAULT_PHONE));
                log.info("[Init({})] 内置管理员账户(admin)默认密码(SM3)/手机号(SM4)已加密并补全到数据库", result);
            }
        };
    }
}
