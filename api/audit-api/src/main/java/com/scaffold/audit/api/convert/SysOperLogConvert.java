package com.scaffold.audit.api.convert;

import static com.scaffold.audit.api.convert.ProtoConverts.emptyToNull;
import static com.scaffold.audit.api.convert.ProtoConverts.toDate;
import static com.scaffold.audit.api.convert.ProtoConverts.toInteger;
import static com.scaffold.audit.api.convert.ProtoConverts.toLong;
import static com.scaffold.audit.api.convert.ProtoConverts.toMillis;

import com.scaffold.audit.api.proto.SysOperLogProto;
import com.scaffold.system.api.domain.SysOperLog;
import java.util.Arrays;

/**
 * SysOperLog <-> SysOperLogProto 转换。
 *
 * @author ct
 */
public final class SysOperLogConvert {
  private SysOperLogConvert() {}

  public static SysOperLogProto toProto(SysOperLog log) {
    if (log == null) {
      return null;
    }
    SysOperLogProto.Builder builder = SysOperLogProto.newBuilder();
    if (log.getOperId() != null) {
      builder.setOperId(log.getOperId());
    }
    if (log.getTitle() != null) {
      builder.setTitle(log.getTitle());
    }
    if (log.getBusinessType() != null) {
      builder.setBusinessType(log.getBusinessType());
    }
    if (log.getBusinessTypes() != null) {
      builder.addAllBusinessTypes(Arrays.asList(log.getBusinessTypes()));
    }
    if (log.getMethod() != null) {
      builder.setMethod(log.getMethod());
    }
    if (log.getRequestMethod() != null) {
      builder.setRequestMethod(log.getRequestMethod());
    }
    if (log.getOperatorType() != null) {
      builder.setOperatorType(log.getOperatorType());
    }
    if (log.getOperName() != null) {
      builder.setOperName(log.getOperName());
    }
    if (log.getDeptName() != null) {
      builder.setDeptName(log.getDeptName());
    }
    if (log.getOperUrl() != null) {
      builder.setOperUrl(log.getOperUrl());
    }
    if (log.getOperIp() != null) {
      builder.setOperIp(log.getOperIp());
    }
    if (log.getOperParam() != null) {
      builder.setOperParam(log.getOperParam());
    }
    if (log.getJsonResult() != null) {
      builder.setJsonResult(log.getJsonResult());
    }
    if (log.getStatus() != null) {
      builder.setStatus(log.getStatus());
    }
    if (log.getErrorMsg() != null) {
      builder.setErrorMsg(log.getErrorMsg());
    }
    if (log.getOperTime() != null) {
      builder.setOperTime(toMillis(log.getOperTime()));
    }
    if (log.getCostTime() != null) {
      builder.setCostTime(log.getCostTime());
    }
    if (log.getUserId() != null) {
      builder.setUserId(log.getUserId());
    }
    if (log.getSessionId() != null) {
      builder.setSessionId(log.getSessionId());
    }
    if (log.getBizKey() != null) {
      builder.setBizKey(log.getBizKey());
    }
    if (log.getBizType() != null) {
      builder.setBizType(log.getBizType());
    }
    if (log.getEventType() != null) {
      builder.setEventType(log.getEventType());
    }
    if (log.getResultCode() != null) {
      builder.setResultCode(log.getResultCode());
    }
    if (log.getRequestIp() != null) {
      builder.setRequestIp(log.getRequestIp());
    }
    if (log.getBeforeValue() != null) {
      builder.setBeforeValue(log.getBeforeValue());
    }
    if (log.getAfterValue() != null) {
      builder.setAfterValue(log.getAfterValue());
    }
    if (log.getCreateBy() != null) {
      builder.setCreateBy(log.getCreateBy());
    }
    if (log.getCreateTime() != null) {
      builder.setCreateTime(toMillis(log.getCreateTime()));
    }
    if (log.getUpdateBy() != null) {
      builder.setUpdateBy(log.getUpdateBy());
    }
    if (log.getUpdateTime() != null) {
      builder.setUpdateTime(toMillis(log.getUpdateTime()));
    }
    if (log.getRemark() != null) {
      builder.setRemark(log.getRemark());
    }
    if (log.getVersion() != null) {
      builder.setVersion(log.getVersion());
    }
    return builder.build();
  }

  public static SysOperLog toJava(SysOperLogProto proto) {
    if (proto == null) {
      return null;
    }
    SysOperLog log = new SysOperLog();
    log.setOperId(toLong(proto.getOperId()));
    log.setTitle(emptyToNull(proto.getTitle()));
    log.setBusinessType(toInteger(proto.getBusinessType()));
    log.setBusinessTypes(proto.getBusinessTypesList().toArray(new Integer[0]));
    log.setMethod(emptyToNull(proto.getMethod()));
    log.setRequestMethod(emptyToNull(proto.getRequestMethod()));
    log.setOperatorType(toInteger(proto.getOperatorType()));
    log.setOperName(emptyToNull(proto.getOperName()));
    log.setDeptName(emptyToNull(proto.getDeptName()));
    log.setOperUrl(emptyToNull(proto.getOperUrl()));
    log.setOperIp(emptyToNull(proto.getOperIp()));
    log.setOperParam(emptyToNull(proto.getOperParam()));
    log.setJsonResult(emptyToNull(proto.getJsonResult()));
    log.setStatus(toInteger(proto.getStatus()));
    log.setErrorMsg(emptyToNull(proto.getErrorMsg()));
    log.setOperTime(toDate(proto.getOperTime()));
    log.setCostTime(toLong(proto.getCostTime()));
    log.setUserId(toLong(proto.getUserId()));
    log.setSessionId(emptyToNull(proto.getSessionId()));
    log.setBizKey(emptyToNull(proto.getBizKey()));
    log.setBizType(emptyToNull(proto.getBizType()));
    log.setEventType(emptyToNull(proto.getEventType()));
    log.setResultCode(toInteger(proto.getResultCode()));
    log.setRequestIp(emptyToNull(proto.getRequestIp()));
    log.setBeforeValue(emptyToNull(proto.getBeforeValue()));
    log.setAfterValue(emptyToNull(proto.getAfterValue()));
    log.setCreateBy(emptyToNull(proto.getCreateBy()));
    log.setCreateTime(toDate(proto.getCreateTime()));
    log.setUpdateBy(emptyToNull(proto.getUpdateBy()));
    log.setUpdateTime(toDate(proto.getUpdateTime()));
    log.setRemark(emptyToNull(proto.getRemark()));
    log.setVersion(toInteger(proto.getVersion()));
    return log;
  }
}
