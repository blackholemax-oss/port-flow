package dev.blackholemax.backend.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final VisitCounterInterceptor visitCounterInterceptor;

    public WebConfig(VisitCounterInterceptor visitCounterInterceptor) {
        this.visitCounterInterceptor = visitCounterInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(visitCounterInterceptor).addPathPatterns("/p/*", "/api/p/*");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 将上传的文件目录映射为可访问的静态资源
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");
    }
}