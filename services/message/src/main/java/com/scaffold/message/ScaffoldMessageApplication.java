package com.scaffold.message;

import com.scaffold.common.security.annotation.EnableCustomConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 消息中心服务（docs/消息中心模块设计文档.md）： 短信管理 + 邮箱管理 + 站内信管理（点对点已读闭环）+ 统一发送契约 RemoteMessageService。 通知公告复用
 * scaffold-system（D1 不迁移），仅菜单归组。
 *
 * @author ct
 */
@EnableCustomConfig
// 国密能力（GmFieldCrypto/FileSymmetricKeyProvider/GmIdentity）由 scaffold-spring-boot-starter-gm
// 自动装配，短信通道密钥/邮箱授权码的 SM4 加解密（Sm4FieldCrypto 静态门面）依赖其启动注入
@SpringBootApplication
public class ScaffoldMessageApplication {
  public static void main(String[] args) {
    SpringApplication.run(ScaffoldMessageApplication.class, args);
    System.out.println("消息中心服务启动成功");
  }
}
