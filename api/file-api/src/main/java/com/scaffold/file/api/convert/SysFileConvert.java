package com.scaffold.file.api.convert;

import com.scaffold.file.api.domain.SysFile;
import com.scaffold.file.api.proto.SysFileProto;

import static com.scaffold.file.api.convert.ProtoConverts.emptyToNull;

/**
 * SysFile <-> SysFileProto 转换。
 *
 * @author ct
 */
public final class SysFileConvert
{
    private SysFileConvert()
    {
    }

    public static SysFileProto toProto(SysFile file)
    {
        if (file == null)
        {
            return null;
        }
        SysFileProto.Builder builder = SysFileProto.newBuilder();
        if (file.getName() != null)
        {
            builder.setName(file.getName());
        }
        if (file.getUrl() != null)
        {
            builder.setUrl(file.getUrl());
        }
        return builder.build();
    }

    public static SysFile toJava(SysFileProto proto)
    {
        if (proto == null)
        {
            return null;
        }
        SysFile file = new SysFile();
        file.setName(emptyToNull(proto.getName()));
        file.setUrl(emptyToNull(proto.getUrl()));
        return file;
    }
}
