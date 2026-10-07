package com.scaffold.message.service;

import java.util.List;

/**
 * 配置类 CRUD 通用契约（渠道/模板/账号同款：乐观锁更新 + 引用校验删除 + 状态启停）
 *
 * @param <T> 实体类型
 * @author ct
 */
public interface IMsgCrudService<T> {
  T queryById(Long id);

  List<T> queryList(T query);

  int insert(T entity);

  int update(T entity);

  int deleteByIds(Long[] ids);

  /** 启停（0 启用 1 停用） */
  int changeStatus(Long id, String status);
}
