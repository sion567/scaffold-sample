package com.scaffold.common.area.utils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * classpath 资源读取小工具（starter 内无 Spring 依赖时的兜底）
 *
 * @author ct
 */
final class ResourceReader {
  private ResourceReader() {}

  static BufferedReader utf8(String name) {
    InputStream in = ResourceReader.class.getResourceAsStream("/" + name);
    if (in == null) {
      throw new IllegalStateException("classpath 资源不存在: " + name);
    }
    return new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
  }

  static byte[] bytes(String name) {
    InputStream in = ResourceReader.class.getResourceAsStream("/" + name);
    if (in == null) {
      throw new IllegalStateException("classpath 资源不存在: " + name);
    }
    try (InputStream in2 = in) {
      return in2.readAllBytes();
    } catch (Exception e) {
      throw new IllegalStateException("classpath 资源读取失败: " + name, e);
    }
  }
}
