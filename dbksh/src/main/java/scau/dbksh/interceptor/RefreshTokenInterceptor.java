package scau.dbksh.interceptor;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import scau.dbksh.constants.RedisConstants;
import scau.dbksh.dto.UserDTO;
import scau.dbksh.utils.UserHolder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class RefreshTokenInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate stringRedisTemplate;

    public RefreshTokenInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String token = request.getHeader("authorization");
        if (!StringUtils.hasText(token)) {
            return true;
        }

        String tokenKey = RedisConstants.LOGIN_USER_KEY + token;
        Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries(tokenKey);
        if (userMap == null || userMap.isEmpty()) {
            return true;
        }

        // 请求开始时恢复当前用户，并顺手刷新 token 过期时间。
        UserDTO userDTO = new UserDTO();
        userDTO.setId(Long.valueOf(String.valueOf(userMap.get("id"))));
        userDTO.setUsername(String.valueOf(userMap.get("username")));
        userDTO.setRole(String.valueOf(userMap.get("role")));
        UserHolder.saveUser(userDTO);
        stringRedisTemplate.expire(tokenKey, RedisConstants.LOGIN_USER_TTL, TimeUnit.MINUTES);
        return true;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception ex
    ) {
        // ThreadLocal 必须在请求结束后清理，避免线程复用导致用户串号。
        UserHolder.removeUser();
    }
}
