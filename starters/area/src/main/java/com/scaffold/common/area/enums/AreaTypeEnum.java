package com.scaffold.common.area.enums;

import java.util.Arrays;

/**
 * 区域类型枚举
 *
 * @author ct
 */
public enum AreaTypeEnum {
  COUNTRY(1, "国家"),
  PROVINCE(2, "省份"),
  CITY(3, "城市"),
  DISTRICT(4, "地区"), // 县、镇、区等
  ;

  /** 类型 */
  private final Integer type;

  /** 名字 */
  private final String name;

  AreaTypeEnum(Integer type, String name) {
    this.type = type;
    this.name = name;
  }

  public Integer getType() {
    return type;
  }

  public String getName() {
    return name;
  }

  public static AreaTypeEnum valueOfType(Integer type) {
    return Arrays.stream(values()).filter(t -> t.type.equals(type)).findFirst().orElse(null);
  }
}
