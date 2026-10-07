package com.scaffold.common.sensitive.config;

import java.io.IOException;
import java.util.Objects;
import com.scaffold.common.core.constant.UserConstants;
import com.scaffold.common.core.context.SecurityContextHolder;
import com.scaffold.common.sensitive.annotation.Sensitive;
import com.scaffold.common.sensitive.enums.DesensitizedType;


import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;


/**
 * 数据脱敏序列化过滤
 *
 * @author scaffold
 */
public class SensitiveJsonSerializer extends JsonSerializer<String> implements ContextualSerializer
{
    private DesensitizedType desensitizedType;

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException
    {
        if (desensitization())
        {
            gen.writeString(desensitizedType.desensitizer().apply(value));
        }
        else
        {
            gen.writeString(value);
        }
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property)
            throws JsonMappingException
    {
        Sensitive annotation = property.getAnnotation(Sensitive.class);
        if (Objects.nonNull(annotation) && Objects.equals(String.class, property.getType().getRawClass()))
        {
            this.desensitizedType = annotation.desensitizedType();
            return this;
        }
        return prov.findValueSerializer(property.getType(), property);
    }

    /**
     * 是否需要脱敏处理
     */
    private boolean desensitization()
    {
        try
        {
            Long userId = SecurityContextHolder.getUserId();
            // 管理员不脱敏
            return !UserConstants.isAdmin(userId);
        }
        catch (Exception e)
        {
            return true;
        }
    }
}



//import tools.jackson.core.JacksonException;
//import tools.jackson.core.JsonGenerator;
//import tools.jackson.databind.BeanProperty;
//import tools.jackson.databind.DatabindException;
//import tools.jackson.databind.SerializationContext;
//import tools.jackson.databind.ValueSerializer;
//import tools.jackson.databind.ser.std.StdSerializer;
/**
 * spring boot 4
 * 数据脱敏序列化过滤
 *
 * @author ct
 */
//public class SensitiveJsonSerializer extends StdSerializer<String>
//{
//    private final DesensitizedType desensitizedType;
//
//    public SensitiveJsonSerializer()
//    {
//        super(String.class);
//        this.desensitizedType = null;
//    }
//
//    public SensitiveJsonSerializer(DesensitizedType desensitizedType)
//    {
//        super(String.class);
//        this.desensitizedType = desensitizedType;
//    }
//
//    @Override
//    public void serialize(String value, JsonGenerator gen, SerializationContext ctxt) throws JacksonException
//    {
//        if (desensitizedType != null && desensitization())
//        {
//            gen.writeString(desensitizedType.desensitizer().apply(value));
//        }
//        else
//        {
//            gen.writeString(value);
//        }
//    }
//
//    @Override
//    public ValueSerializer<?> createContextual(SerializationContext ctxt, BeanProperty property) throws DatabindException
//    {
//        Sensitive annotation = property.getAnnotation(Sensitive.class);
//        if (Objects.nonNull(annotation) && Objects.equals(String.class, property.getType().getRawClass()))
//        {
//            return new SensitiveJsonSerializer(annotation.desensitizedType());
//        }
//        return ctxt.findValueSerializer(property.getType());
//    }
//
//    /**
//     * 是否需要脱敏处理
//     */
//    private boolean desensitization()
//    {
//        try
//        {
//            Long userId = SecurityContextHolder.getUserId();
//            // 管理员不脱敏
//            return !UserConstants.isAdmin(userId);
//        }
//        catch (Exception e)
//        {
//            return true;
//        }
//    }
//}
