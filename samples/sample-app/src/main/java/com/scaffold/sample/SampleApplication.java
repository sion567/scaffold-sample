package com.scaffold.sample;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import com.scaffold.common.security.annotation.EnableCustomConfig;

/**
 * 样例业务服务
 *
 * 演示脚手架的标准业务形态：MVC 业务服务（网关直连类），覆盖
 * 单表 CRUD / 树表 / 主子表 / 工作流审批 / 业务定时任务 / 脱敏 / 操作日志。
 * 代码结构与 gen 代码生成器的产物同构，可直接对照生成结果学习。
 *
 * @author scaffold
 */
@EnableCustomConfig
@EnableScheduling
@SpringBootApplication
public class SampleApplication
{
    public static void main(String[] args)
    {
        SpringApplication.run(SampleApplication.class, args);
        System.out.println("样例服务启动成功");
    }
}
