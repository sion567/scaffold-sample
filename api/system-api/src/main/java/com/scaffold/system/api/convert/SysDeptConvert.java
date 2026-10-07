package com.scaffold.system.api.convert;

import java.util.ArrayList;
import java.util.List;

import com.scaffold.system.api.domain.SysDept;
import com.scaffold.system.api.proto.SysDeptProto;

import static com.scaffold.system.api.convert.ProtoConverts.emptyToNull;
import static com.scaffold.system.api.convert.ProtoConverts.toDate;
import static com.scaffold.system.api.convert.ProtoConverts.toInteger;
import static com.scaffold.system.api.convert.ProtoConverts.toLong;
import static com.scaffold.system.api.convert.ProtoConverts.toMillis;

/**
 * SysDept <-> SysDeptProto 转换（IDL/protobuf 契约与 Java 领域对象之间的边界映射）。
 *
 * @author ct
 */
public final class SysDeptConvert
{
    private SysDeptConvert()
    {
    }

    public static SysDeptProto toProto(SysDept dept)
    {
        if (dept == null)
        {
            return null;
        }
        SysDeptProto.Builder builder = SysDeptProto.newBuilder();
        if (dept.getDeptId() != null)
        {
            builder.setDeptId(dept.getDeptId());
        }
        if (dept.getParentId() != null)
        {
            builder.setParentId(dept.getParentId());
        }
        if (dept.getAncestors() != null)
        {
            builder.setAncestors(dept.getAncestors());
        }
        if (dept.getDeptName() != null)
        {
            builder.setDeptName(dept.getDeptName());
        }
        if (dept.getOrderNum() != null)
        {
            builder.setOrderNum(dept.getOrderNum());
        }
        if (dept.getLeader() != null)
        {
            builder.setLeader(dept.getLeader());
        }
        if (dept.getPhone() != null)
        {
            builder.setPhone(dept.getPhone());
        }
        if (dept.getEmail() != null)
        {
            builder.setEmail(dept.getEmail());
        }
        if (dept.getStatus() != null)
        {
            builder.setStatus(dept.getStatus());
        }
        if (dept.getDelFlag() != null)
        {
            builder.setDelFlag(dept.getDelFlag());
        }
        if (dept.getParentName() != null)
        {
            builder.setParentName(dept.getParentName());
        }
        if (dept.getChildren() != null)
        {
            for (SysDept child : dept.getChildren())
            {
                builder.addChildren(toProto(child));
            }
        }
        if (dept.getCreateBy() != null)
        {
            builder.setCreateBy(dept.getCreateBy());
        }
        if (dept.getCreateTime() != null)
        {
            builder.setCreateTime(toMillis(dept.getCreateTime()));
        }
        if (dept.getUpdateBy() != null)
        {
            builder.setUpdateBy(dept.getUpdateBy());
        }
        if (dept.getUpdateTime() != null)
        {
            builder.setUpdateTime(toMillis(dept.getUpdateTime()));
        }
        if (dept.getRemark() != null)
        {
            builder.setRemark(dept.getRemark());
        }
        if (dept.getVersion() != null)
        {
            builder.setVersion(dept.getVersion());
        }
        return builder.build();
    }

    public static SysDept toJava(SysDeptProto proto)
    {
        if (proto == null)
        {
            return null;
        }
        SysDept dept = new SysDept();
        dept.setDeptId(toLong(proto.getDeptId()));
        dept.setParentId(toLong(proto.getParentId()));
        dept.setAncestors(emptyToNull(proto.getAncestors()));
        dept.setDeptName(emptyToNull(proto.getDeptName()));
        dept.setOrderNum(toInteger(proto.getOrderNum()));
        dept.setLeader(emptyToNull(proto.getLeader()));
        dept.setPhone(emptyToNull(proto.getPhone()));
        dept.setEmail(emptyToNull(proto.getEmail()));
        dept.setStatus(emptyToNull(proto.getStatus()));
        dept.setDelFlag(emptyToNull(proto.getDelFlag()));
        dept.setParentName(emptyToNull(proto.getParentName()));
        List<SysDept> children = new ArrayList<>();
        for (SysDeptProto child : proto.getChildrenList())
        {
            children.add(toJava(child));
        }
        dept.setChildren(children);
        dept.setCreateBy(emptyToNull(proto.getCreateBy()));
        dept.setCreateTime(toDate(proto.getCreateTime()));
        dept.setUpdateBy(emptyToNull(proto.getUpdateBy()));
        dept.setUpdateTime(toDate(proto.getUpdateTime()));
        dept.setRemark(emptyToNull(proto.getRemark()));
        dept.setVersion(toInteger(proto.getVersion()));
        return dept;
    }
}
