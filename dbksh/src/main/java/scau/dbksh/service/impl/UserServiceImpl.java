package scau.dbksh.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import scau.dbksh.constants.RedisConstants;
import scau.dbksh.dto.LoginFormDTO;
import scau.dbksh.dto.UserDTO;
import scau.dbksh.entity.User;
import scau.dbksh.mapper.UserMapper;
import scau.dbksh.result.Result;
import scau.dbksh.service.UserService;
import scau.dbksh.utils.UserHolder;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Service
public class UserServiceImpl implements UserService {

    private static final String DEFAULT_ROLE = "\u7528\u6237\u7aef";
    private static final String USERNAME_PREFIX = "user_";
    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final StringRedisTemplate stringRedisTemplate;
    private final UserMapper userMapper;
    private final Environment environment;

    public UserServiceImpl(StringRedisTemplate stringRedisTemplate, UserMapper userMapper, Environment environment) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.userMapper = userMapper;
        this.environment = environment;
    }

    @Override
    public Result<Void> sendCode(String wechat) {
        if (!StringUtils.hasText(wechat)) {
            return Result.error("wechat cannot be blank");
        }
        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
        String redisKey = RedisConstants.LOGIN_CODE_KEY + wechat;
        stringRedisTemplate.opsForValue().set(redisKey, code, RedisConstants.LOGIN_CODE_TTL, TimeUnit.MINUTES);
        if (environment.acceptsProfiles(Profiles.of("dev"))) {
            log.info("send login code, wechat={}, code={}", wechat, code);
        }
        return Result.success();
    }

    @Override
    public Result<String> login(LoginFormDTO loginFormDTO) {
        if (loginFormDTO == null || !StringUtils.hasText(loginFormDTO.getWechat())) {
            return Result.error("wechat cannot be blank");
        }
        if (!StringUtils.hasText(loginFormDTO.getCode())) {
            return Result.error("code cannot be blank");
        }

        String wechat = loginFormDTO.getWechat();
        String cacheCode = stringRedisTemplate.opsForValue().get(RedisConstants.LOGIN_CODE_KEY + wechat);
        if (!loginFormDTO.getCode().equals(cacheCode)) {
            return Result.error("verification code error");
        }

        User user = userMapper.selectByWechat(wechat);
        if (user == null) {
            user = createUserWithWechat(wechat);
            int rows = userMapper.insertUser(user);
            if (rows != 1 || user.getId() == null) {
                return Result.error("create user failed");
            }
        }

        String token = UUID.randomUUID().toString().replace("-", "");
        UserDTO userDTO = toUserDTO(user);
        String tokenKey = RedisConstants.LOGIN_USER_KEY + token;
        stringRedisTemplate.opsForHash().putAll(tokenKey, toRedisMap(userDTO));
        stringRedisTemplate.expire(tokenKey, RedisConstants.LOGIN_USER_TTL, TimeUnit.MINUTES);
        return Result.success(token);
    }

    @Override
    public Result<UserDTO> me() {
        return Result.success(UserHolder.getUser());
    }

    private User createUserWithWechat(String wechat) {
        User user = new User();
        user.setWechat(wechat);
        user.setUsername(USERNAME_PREFIX + randomSuffix());
        user.setRole(DEFAULT_ROLE);
        return user;
    }

    private UserDTO toUserDTO(User user) {
        UserDTO userDTO = new UserDTO();
        userDTO.setId(user.getId());
        userDTO.setUsername(user.getUsername());
        userDTO.setRole(user.getRole());
        return userDTO;
    }

    private Map<String, String> toRedisMap(UserDTO userDTO) {
        Map<String, String> userMap = new HashMap<>(4);
        userMap.put("id", String.valueOf(userDTO.getId()));
        userMap.put("username", userDTO.getUsername());
        userMap.put("role", userDTO.getRole());
        return userMap;
    }

    private String randomSuffix() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }
}
