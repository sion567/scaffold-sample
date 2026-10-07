package com.scaffold.audit.api.convert;

import java.util.Date;

/**
 * IDL/protobuf 契约转换公共工具。 约定：null <-> proto3 默认值；Date <-> int64 毫秒时间戳（0 表示 null）。
 *
 * @author ct
 */
public final class ProtoConverts {
  private ProtoConverts() {}

  public static long toMillis(Date date) {
    return date == null ? 0L : date.getTime();
  }

  public static Date toDate(long millis) {
    return millis == 0L ? null : new Date(millis);
  }

  public static String emptyToNull(String value) {
    return value == null || value.isEmpty() ? null : value;
  }

  public static Long toLong(long value) {
    return value == 0L ? null : value;
  }

  public static Integer toInteger(int value) {
    return value == 0 ? null : value;
  }
}
