package com.scaffold.audit;

import com.scaffold.common.security.annotation.EnableCustomConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 审计服务（等保 8.1.4 安全审计）
 *
 * <p>设计要点： 1. 独立部署，不受业务服务重启影响（§附.6 f 审计进程保护） 2. 连接 app_aud 只读账号，物理上无法修改任何数据（§附.5 Layer2） 3.
 * 提供操作日志/登录日志/业务追溯/权限快照/统计分析能力（§附.7 g）
 *
 * <p>实体扫描须覆盖 api 模块（SysOperLog/SysLogininfor/CollectorAuditLog/QueryAuditLog
 * 位于 com.scaffold.system.api.domain，不在应用包树内）。
 *
 * @author ct
 */
@EnableCustomConfig
@EnableScheduling
@SpringBootApplication
@EntityScan(basePackages = {"com.scaffold.audit", "com.scaffold.system.api.domain"})
public class ScaffoldAuditApplication {
  public static void main(String[] args) {
    SpringApplication.run(ScaffoldAuditApplication.class, args);
    System.out.println(
        "(♥◠‿◠)ﾉﾞ  审计服务模块启动成功   ლ(´ڡ`ლ)ﾞ  \n"
            + " .-------.       ____     __        \n"
            + " |  _ _   \\      \\   \\   /  /    \n"
            + " | ( ' )  |       \\  _. /  '       \n"
            + " |(_ o _) /        _( )_ .'         \n"
            + " | (_,_).' __  ___(_ o _)'          \n"
            + " |  |\\ \\  |  ||   |(_,_)'         \n"
            + " |  | \\ `'   /|   `-'  /           \n"
            + " |  |  \\    /  \\      /           \n"
            + " ''-'   `'-'    `-..-'              ");
  }
}
