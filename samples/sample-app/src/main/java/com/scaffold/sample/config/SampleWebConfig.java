package com.scaffold.sample.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import com.scaffold.sample.security.HeaderAuthInterceptor;

/**
 * Web MVC 配置：注册网关身份头解析拦截器
 *
 * 网关 AuthFilter 完成 JWT 校验后透传身份头，MVC 业务服务在 Servlet 侧
 * 解析并写入 SecurityContextHolder，使 @RequiresPermissions / SecurityUtils 生效
 * （平台 Triple REST 服务由 Dubbo SecurityContextFilter 承担同等职责）。
 *
 * @author scaffold
 */
@Configuration
public class SampleWebConfig implements WebMvcConfigurer
{
    @Autowired
    private HeaderAuthInterceptor headerAuthInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry)
    {
        registry.addInterceptor(headerAuthInterceptor).addPathPatterns("/**");
    }
}
