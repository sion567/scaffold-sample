package com.scaffold.job;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import com.scaffold.common.security.annotation.EnableCustomConfig;

/**
 * 任务调度服务
 *
 * @author scaffold
 */
@EnableCustomConfig
@SpringBootApplication
public class ScaffoldJobApplication
{
    public static void main(String[] args)
    {
        SpringApplication.run(ScaffoldJobApplication.class, args);
        System.out.println("任务调度服务启动成功");
    }
}
