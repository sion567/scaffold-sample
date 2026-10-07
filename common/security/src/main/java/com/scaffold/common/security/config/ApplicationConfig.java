package com.scaffold.common.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.util.TimeZone;

import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;

/**
 * 系统配置
 *
 * @author ct
 */
public class ApplicationConfig
{
    /**
     * 时区配置 spring boot 4
     */
//    @Bean
//    public ObjectMapper objectMapper()
//    {
//        ObjectMapper objectMapper = JsonMapper.builder().build();
//        objectMapper.findAndRegisterModules();
//        objectMapper.setTimeZone(TimeZone.getDefault());
//        return objectMapper;
//    }


    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonObjectMapperCustomization()
    {
        return jacksonObjectMapperBuilder -> jacksonObjectMapperBuilder.timeZone(TimeZone.getDefault());
    }
}
