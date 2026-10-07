package com.scaffold.common.area;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.scaffold.common.area.enums.AreaTypeEnum;
import com.scaffold.common.area.utils.AreaUtils;
import com.scaffold.common.area.utils.IPUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 冒烟测试：真实 area.csv / ip2region.xdb 数据链路（static 初始化在类加载时执行）。
 *
 * @author ct
 */
class AreaSmokeTest {
  @Test
  @DisplayName("区划树：根节点中国 + 按类型取省级列表")
  void areaTreeLoaded() {
    Area china = AreaUtils.getArea(Area.ID_CHINA);
    assertNotNull(china);
    assertEquals("中国", china.getName());
    assertDoesNotThrow(() -> AreaUtils.getByType(AreaTypeEnum.PROVINCE, Area::getName));
  }

  @Test
  @DisplayName("IP 离线查询：不抛异常即通过（数据覆盖范围以 xdb 为准）")
  void ipSearch() {
    assertDoesNotThrow(() -> IPUtils.getAreaId("220.181.38.148"));
  }
}
