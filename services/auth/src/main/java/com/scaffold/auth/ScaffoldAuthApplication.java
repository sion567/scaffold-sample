package com.scaffold.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 认证授权中心
 *
 * @author scaffold
 */
// 国密能力（GmIdentity/GmFieldCrypto）由 scaffold-spring-boot-starter-gm 自动装配，无需组件扫描
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class })
@EnableDiscoveryClient
public class ScaffoldAuthApplication
{
    public static void main(String[] args)
    {
        SpringApplication.run(ScaffoldAuthApplication.class, args);
        System.out.println("认证授权中心启动成功");
    }
}
