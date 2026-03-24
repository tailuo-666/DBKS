package scau.dbksh.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import scau.dbksh.interceptor.LoginInterceptor;
import scau.dbksh.interceptor.RefreshTokenInterceptor;

@Configuration
public class MvcConfig implements WebMvcConfigurer {

    private final RefreshTokenInterceptor refreshTokenInterceptor;
    private final LoginInterceptor loginInterceptor;

    public MvcConfig(RefreshTokenInterceptor refreshTokenInterceptor, LoginInterceptor loginInterceptor) {
        this.refreshTokenInterceptor = refreshTokenInterceptor;
        this.loginInterceptor = loginInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(refreshTokenInterceptor)
                .addPathPatterns("/**")
                .order(0);

        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/user/code",
                        "/user/login",
                        "/shop/**"
                )
                .order(1);
    }
}
