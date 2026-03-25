package scau.dbksh.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import scau.dbksh.interceptor.AdminInterceptor;
import scau.dbksh.interceptor.LoginInterceptor;
import scau.dbksh.interceptor.RefreshTokenInterceptor;

@Configuration
public class MvcConfig implements WebMvcConfigurer {

    private final RefreshTokenInterceptor refreshTokenInterceptor;
    private final LoginInterceptor loginInterceptor;
    private final AdminInterceptor adminInterceptor;

    public MvcConfig(
            RefreshTokenInterceptor refreshTokenInterceptor,
            LoginInterceptor loginInterceptor,
            AdminInterceptor adminInterceptor
    ) {
        this.refreshTokenInterceptor = refreshTokenInterceptor;
        this.loginInterceptor = loginInterceptor;
        this.adminInterceptor = adminInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 先尝试从 token 恢复登录态，后续拦截器和业务代码都依赖 UserHolder 中的用户信息。
        registry.addInterceptor(refreshTokenInterceptor)
                .addPathPatterns("/**")
                .order(0);

        // 公开浏览接口放行，其余 /user/** 接口默认要求登录。
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/user/code",
                        "/user/login",
                        "/shop/**"
                )
                .order(1);

        registry.addInterceptor(adminInterceptor)
                .addPathPatterns("/admin/**")
                .order(2);
    }
}
