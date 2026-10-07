package com.scaffold.audit.api.convert;

import static com.scaffold.audit.api.convert.ProtoConverts.emptyToNull;
import static com.scaffold.audit.api.convert.ProtoConverts.toDate;
import static com.scaffold.audit.api.convert.ProtoConverts.toInteger;
import static com.scaffold.audit.api.convert.ProtoConverts.toLong;
import static com.scaffold.audit.api.convert.ProtoConverts.toMillis;

import com.scaffold.audit.api.proto.SysLogininforProto;
import com.scaffold.system.api.domain.SysLogininfor;

/**
 * SysLogininfor <-> SysLogininforProto 转换。
 *
 * @author ct
 */
public final class SysLogininforConvert {
  private SysLogininforConvert() {}

  public static SysLogininforProto toProto(SysLogininfor logininfor) {
    if (logininfor == null) {
      return null;
    }
    SysLogininforProto.Builder builder = SysLogininforProto.newBuilder();
    if (logininfor.getInfoId() != null) {
      builder.setInfoId(logininfor.getInfoId());
    }
    if (logininfor.getUserName() != null) {
      builder.setUserName(logininfor.getUserName());
    }
    if (logininfor.getStatus() != null) {
      builder.setStatus(logininfor.getStatus());
    }
    if (logininfor.getIpaddr() != null) {
      builder.setIpaddr(logininfor.getIpaddr());
    }
    if (logininfor.getMsg() != null) {
      builder.setMsg(logininfor.getMsg());
    }
    if (logininfor.getAccessTime() != null) {
      builder.setAccessTime(toMillis(logininfor.getAccessTime()));
    }
    if (logininfor.getCreateBy() != null) {
      builder.setCreateBy(logininfor.getCreateBy());
    }
    if (logininfor.getCreateTime() != null) {
      builder.setCreateTime(toMillis(logininfor.getCreateTime()));
    }
    if (logininfor.getUpdateBy() != null) {
      builder.setUpdateBy(logininfor.getUpdateBy());
    }
    if (logininfor.getUpdateTime() != null) {
      builder.setUpdateTime(toMillis(logininfor.getUpdateTime()));
    }
    if (logininfor.getRemark() != null) {
      builder.setRemark(logininfor.getRemark());
    }
    if (logininfor.getVersion() != null) {
      builder.setVersion(logininfor.getVersion());
    }
    return builder.build();
  }

  public static SysLogininfor toJava(SysLogininforProto proto) {
    if (proto == null) {
      return null;
    }
    SysLogininfor logininfor = new SysLogininfor();
    logininfor.setInfoId(toLong(proto.getInfoId()));
    logininfor.setUserName(emptyToNull(proto.getUserName()));
    logininfor.setStatus(emptyToNull(proto.getStatus()));
    logininfor.setIpaddr(emptyToNull(proto.getIpaddr()));
    logininfor.setMsg(emptyToNull(proto.getMsg()));
    logininfor.setAccessTime(toDate(proto.getAccessTime()));
    logininfor.setCreateBy(emptyToNull(proto.getCreateBy()));
    logininfor.setCreateTime(toDate(proto.getCreateTime()));
    logininfor.setUpdateBy(emptyToNull(proto.getUpdateBy()));
    logininfor.setUpdateTime(toDate(proto.getUpdateTime()));
    logininfor.setRemark(emptyToNull(proto.getRemark()));
    logininfor.setVersion(toInteger(proto.getVersion()));
    return logininfor;
  }
}
