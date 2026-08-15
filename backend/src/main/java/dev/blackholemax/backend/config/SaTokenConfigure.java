package dev.blackholemax.backend.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SaTokenConfigure implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> {
            // 后台 API 需要登录（管理后台登录接口 /api/auth/admin/login 不在 /api/admin/** 下，天然放行）
            SaRouter.match("/api/admin/**").check(r -> StpUtil.checkLogin());
            // 用户管理接口仅限管理员角色
            SaRouter.match("/api/admin/users/**").check(r -> StpUtil.checkRole("admin"));
            // 后台页面需要登录（登录/注册页放行）
            SaRouter.match("/admin/**")
                    .notMatch("/admin/login", "/admin/register")
                    .check(r -> StpUtil.checkLogin());
        })).addPathPatterns("/**");
    }
}