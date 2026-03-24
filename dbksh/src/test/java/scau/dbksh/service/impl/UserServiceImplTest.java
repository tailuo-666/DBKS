package scau.dbksh.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import scau.dbksh.constants.RedisConstants;
import scau.dbksh.dto.LoginFormDTO;
import scau.dbksh.entity.User;
import scau.dbksh.mapper.UserMapper;
import scau.dbksh.result.Result;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    private static final String DEFAULT_ROLE = "\u7528\u6237\u7aef";

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private UserMapper userMapper;

    @Mock
    private Environment environment;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @InjectMocks
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
        doReturn(true).when(environment).acceptsProfiles(any(Profiles.class));
    }

    @Test
    void shouldStoreSixDigitCodeInRedis() {
        Result<Void> result = userService.sendCode("wechat_001");

        assertEquals(1, result.getCode());

        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(
                eq(RedisConstants.LOGIN_CODE_KEY + "wechat_001"),
                codeCaptor.capture(),
                eq(RedisConstants.LOGIN_CODE_TTL),
                eq(TimeUnit.MINUTES)
        );
        assertTrue(codeCaptor.getValue().matches("\\d{6}"));
    }

    @Test
    void shouldRejectLoginWhenCodeDoesNotMatch() {
        LoginFormDTO loginFormDTO = new LoginFormDTO();
        loginFormDTO.setWechat("wechat_001");
        loginFormDTO.setCode("654321");
        when(valueOperations.get(RedisConstants.LOGIN_CODE_KEY + "wechat_001")).thenReturn("123456");

        Result<String> result = userService.login(loginFormDTO);

        assertEquals(0, result.getCode());
        assertEquals("verification code error", result.getMsg());
    }

    @Test
    void shouldLoginExistingUserAndStoreToken() {
        LoginFormDTO loginFormDTO = new LoginFormDTO();
        loginFormDTO.setWechat("wechat_001");
        loginFormDTO.setCode("123456");
        when(valueOperations.get(RedisConstants.LOGIN_CODE_KEY + "wechat_001")).thenReturn("123456");

        User user = new User();
        user.setId(1L);
        user.setWechat("wechat_001");
        user.setUsername("alice");
        user.setRole(DEFAULT_ROLE);
        when(userMapper.selectByWechat("wechat_001")).thenReturn(user);

        Result<String> result = userService.login(loginFormDTO);

        assertEquals(1, result.getCode());
        assertNotNull(result.getData());
        assertFalse(result.getData().isBlank());

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map<String, String>> mapCaptor = ArgumentCaptor.forClass(Map.class);
        verify(hashOperations).putAll(keyCaptor.capture(), mapCaptor.capture());
        verify(stringRedisTemplate).expire(keyCaptor.getValue(), RedisConstants.LOGIN_USER_TTL, TimeUnit.MINUTES);

        assertTrue(keyCaptor.getValue().startsWith(RedisConstants.LOGIN_USER_KEY));
        assertEquals("1", mapCaptor.getValue().get("id"));
        assertEquals("alice", mapCaptor.getValue().get("username"));
        assertEquals(DEFAULT_ROLE, mapCaptor.getValue().get("role"));
    }

    @Test
    void shouldCreateUserWhenWechatDoesNotExist() {
        LoginFormDTO loginFormDTO = new LoginFormDTO();
        loginFormDTO.setWechat("wechat_002");
        loginFormDTO.setCode("123456");
        when(valueOperations.get(RedisConstants.LOGIN_CODE_KEY + "wechat_002")).thenReturn("123456");
        when(userMapper.selectByWechat("wechat_002")).thenReturn(null);
        doAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(2L);
            return 1;
        }).when(userMapper).insertUser(any(User.class));

        Result<String> result = userService.login(loginFormDTO);

        assertEquals(1, result.getCode());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insertUser(userCaptor.capture());
        assertEquals("wechat_002", userCaptor.getValue().getWechat());
        assertEquals(DEFAULT_ROLE, userCaptor.getValue().getRole());
        assertTrue(userCaptor.getValue().getUsername().startsWith("user_"));
        verify(stringRedisTemplate, never()).delete(anyString());
    }
}
