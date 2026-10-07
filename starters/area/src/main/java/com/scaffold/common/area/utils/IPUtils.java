package com.scaffold.common.area.utils;

import com.scaffold.common.area.Area;
import org.lionsoul.ip2region.xdb.LongByteArray;
import org.lionsoul.ip2region.xdb.Searcher;
import org.lionsoul.ip2region.xdb.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * IP 工具类
 *
 * <p>IP 数据源来自 classpath:ip2region.xdb（启动加载到内存）
 *
 * @author wanglhup
 */
public class IPUtils {

  private static final Logger log = LoggerFactory.getLogger(IPUtils.class);

  private static Searcher searcher;

  static {
    init();
  }

  private IPUtils() {}

  /** 初始化：xdb 全量载入内存 */
  private static void init() {
    try {
      long now = System.currentTimeMillis();
      LongByteArray buff = new LongByteArray();
      buff.append(ResourceReader.bytes("ip2region.xdb"));
      searcher = Searcher.newWithBuffer(Version.IPv4, buff);
      log.info("启动加载 IPUtils 成功，耗时 {} 毫秒", System.currentTimeMillis() - now);
    } catch (Exception e) {
      throw new IllegalStateException("IPUtils 初始化失败", e);
    }
  }

  /**
   * 查询 IP 对应的地区编号
   *
   * @param ip IP 地址，格式为 127.0.0.1
   */
  public static Integer getAreaId(String ip) {
    try {
      return Integer.parseInt(searcher.search(ip.trim()));
    } catch (Exception e) {
      throw new IllegalStateException("IP 查询失败: " + ip, e);
    }
  }

  /** 查询 IP 对应的地区编号（IP 的时间戳形式） */
  public static Integer getAreaId(long ip) {
    try {
      byte[] ipBytes =
          new byte[] {
            (byte) ((ip >> 24) & 0xFF), (byte) ((ip >> 16) & 0xFF),
            (byte) ((ip >> 8) & 0xFF), (byte) (ip & 0xFF)
          };
      return Integer.parseInt(searcher.search(ipBytes));
    } catch (Exception e) {
      throw new IllegalStateException("IP 查询失败: " + ip, e);
    }
  }

  /** 查询 IP 对应的地区 */
  public static Area getArea(String ip) {
    return com.scaffold.common.area.utils.AreaUtils.getArea(getAreaId(ip));
  }

  /** 查询 IP 对应的地区（时间戳形式） */
  public static Area getArea(long ip) {
    return AreaUtils.getArea(getAreaId(ip));
  }

  static String long2Ip(long ip) {
    StringBuilder sb = new StringBuilder();
    sb.append((ip >> 24) & 0xFF)
        .append('.')
        .append((ip >> 16) & 0xFF)
        .append('.')
        .append((ip >> 8) & 0xFF)
        .append('.')
        .append(ip & 0xFF);
    return sb.toString();
  }
}
