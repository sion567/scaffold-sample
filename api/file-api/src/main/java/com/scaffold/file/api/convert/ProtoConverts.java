package com.scaffold.file.api.convert;

/**
 * IDL/protobuf 契约转换公共工具。
 * 约定：null <-> proto3 默认值（空串/0/false）。
 *
 * @author ct
 */
public final class ProtoConverts
{
    private ProtoConverts()
    {
    }

    public static String emptyToNull(String value)
    {
        return value == null || value.isEmpty() ? null : value;
    }
}
