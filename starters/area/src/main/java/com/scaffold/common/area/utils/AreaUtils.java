package com.scaffold.common.area.utils;

import com.scaffold.common.area.Area;
import com.scaffold.common.area.enums.AreaTypeEnum;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 行政区划工具（启动时从 classpath:area.csv 加载区划树到内存）
 *
 * @author ct
 */
public class AreaUtils {

  private static final Logger log = LoggerFactory.getLogger(AreaUtils.class);

  /** Area 内存缓存，提升访问速度 */
  private static final Map<Integer, Area> AREAS = new HashMap<>();

  static {
    init();
  }

  private AreaUtils() {}

  /** 初始化：解析 area.csv（表头 id,name,type,parentId）并构建父子树 */
  private static void init() {
    long now = System.currentTimeMillis();
    AREAS.put(Area.ID_GLOBAL, newArea(Area.ID_GLOBAL, "全球", 0, null));
    try (CSVParser parser =
        CSVParser.parse(
            ResourceReader.utf8("area.csv"),
            CSVFormat.DEFAULT
                .builder()
                .setHeader("id", "name", "type", "parentId")
                .setSkipHeaderRecord(true)
                .get())) {
      for (CSVRecord record : parser) {
        Area area =
            newArea(
                Integer.valueOf(record.get("id")),
                record.get("name"),
                Integer.valueOf(record.get("type")),
                null);
        AREAS.put(area.getId(), area);
      }
      // 第二遍：构建父子关系（parentId 指向的节点此时已全部在缓存）
      try (CSVParser parser2 =
          CSVParser.parse(
              ResourceReader.utf8("area.csv"),
              CSVFormat.DEFAULT
                  .builder()
                  .setHeader("id", "name", "type", "parentId")
                  .setSkipHeaderRecord(true)
                  .get())) {
        for (CSVRecord record : parser2) {
          Area area = AREAS.get(Integer.valueOf(record.get("id")));
          Area parent = AREAS.get(Integer.valueOf(record.get("parentId")));
          if (area == null || parent == null || area == parent) {
            continue;
          }
          area.setParent(parent);
          parent.getChildren().add(area);
        }
      }
      log.info("启动加载 AreaUtils 成功，耗时 {} 毫秒", System.currentTimeMillis() - now);
    } catch (Exception e) {
      throw new IllegalStateException("AreaUtils 初始化失败", e);
    }
  }

  private static Area newArea(Integer id, String name, Integer type, Area parent) {
    Area area = new Area();
    area.setId(id);
    area.setName(name);
    area.setType(type);
    area.setParent(parent);
    area.setChildren(new ArrayList<>());
    return area;
  }

  /** 获得指定编号对应的区域 */
  public static Area getArea(Integer id) {
    return AREAS.get(id);
  }

  /** 按路径解析区域，例如说：河南省/石家庄市/新华区 */
  public static Area parseArea(String pathStr) {
    Area area = null;
    for (String path : pathStr.split("/")) {
      List<Area> scope = area == null ? new ArrayList<>(AREAS.values()) : area.getChildren();
      area = findFirst(scope, item -> item.getName().equals(path));
      if (area == null) {
        return null;
      }
    }
    return area;
  }

  /** 获取所有节点的全路径名称（祖先/父级/子级） */
  public static List<String> getAreaNodePathList(List<Area> areas) {
    List<String> paths = new ArrayList<>();
    areas.forEach(area -> getAreaNodePathList(area, "", paths));
    return paths;
  }

  private static void getAreaNodePathList(Area node, String path, List<String> paths) {
    if (node == null) {
      return;
    }
    String currentPath = path.isEmpty() ? node.getName() : path + "/" + node.getName();
    paths.add(currentPath);
    for (Area child : node.getChildren()) {
      getAreaNodePathList(child, currentPath, paths);
    }
  }

  /** 格式化区域（空格分隔） */
  public static String format(Integer id) {
    return format(id, " ");
  }

  /** 格式化区域：中国境内默认跳过国家节点，例如 上海 上海市 静安区 */
  public static String format(Integer id, String separator) {
    Area area = AREAS.get(id);
    if (area == null) {
      return null;
    }
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < AreaTypeEnum.values().length; i++) { // 避免死循环
      sb.insert(0, area.getName());
      area = area.getParent();
      if (area == null || equalsAny(area.getId(), Area.ID_GLOBAL, Area.ID_CHINA)) {
        break;
      }
      sb.insert(0, separator);
    }
    return sb.toString();
  }

  /** 获取指定类型的区域列表 */
  public static <T> List<T> getByType(AreaTypeEnum type, Function<Area, T> func) {
    return AREAS.values().stream()
        .filter(area -> type.getType().equals(area.getType()))
        .map(func)
        .collect(Collectors.toList());
  }

  /** 根据区域编号、上级区域类型，获取上级区域编号 */
  public static Integer getParentIdByType(Integer id, AreaTypeEnum type) {
    Objects.requireNonNull(type, "type 不能为空");
    for (int i = 0; i < Byte.MAX_VALUE; i++) {
      Area area = AREAS.get(id);
      if (area == null) {
        return null;
      }
      if (type.getType().equals(area.getType())) {
        return area.getId();
      }
      if (area.getParent() == null || area.getParent().getId() == null) {
        return null;
      }
      id = area.getParent().getId();
    }
    return null;
  }

  private static boolean equalsAny(Integer value, Integer a, Integer b) {
    return value.equals(a) || value.equals(b);
  }

  private static Area findFirst(List<Area> areas, java.util.function.Predicate<Area> predicate) {
    for (Area area : areas) {
      if (predicate.test(area)) {
        return area;
      }
    }
    return null;
  }
}
