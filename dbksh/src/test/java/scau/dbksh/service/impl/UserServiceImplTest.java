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
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    private static final String DEFAULT_ROLE = "\u7528\u6237\u7aef";
    private static final String ADMIN_ROLE = "\u7ba1\u7406\u5458";

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
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
        lenient().doReturn(true).when(environment).acceptsProfiles(any(Profiles.class));
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
    void shouldRejectUserLoginWhenCodeDoesNotMatch() {
        LoginFormDTO loginFormDTO = loginForm("wechat_001", "654321");
        when(valueOperations.get(RedisConstants.LOGIN_CODE_KEY + "wechat_001")).thenReturn("123456");

        Result<String> result = userService.userLogin(loginFormDTO);

        assertEquals(0, result.getCode());
        assertEquals("verification code error", result.getMsg());
    }

    @Test
    void shouldLoginExistingUserAndStoreToken() {
        LoginFormDTO loginFormDTO = loginForm("wechat_001", "123456");
        when(valueOperations.get(RedisConstants.LOGIN_CODE_KEY + "wechat_001")).thenReturn("123456");
        when(userMapper.selectByWechat("wechat_001")).thenReturn(user(1L, "alice", "wechat_001", DEFAULT_ROLE));

        Result<String> result = userService.userLogin(loginFormDTO);

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
        LoginFormDTO loginFormDTO = loginForm("wechat_002", "123456");
        when(valueOperations.get(RedisConstants.LOGIN_CODE_KEY + "wechat_002")).thenReturn("123456");
        when(userMapper.selectByWechat("wechat_002")).thenReturn(null);
        doAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(2L);
            return 1;
        }).when(userMapper).insertUser(any(User.class));

        Result<String> result = userService.userLogin(loginFormDTO);

        assertEquals(1, result.getCode());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insertUser(userCaptor.capture());
        assertEquals("wechat_002", userCaptor.getValue().getWechat());
        assertEquals(DEFAULT_ROLE, userCaptor.getValue().getRole());
        assertTrue(userCaptor.getValue().getUsername().startsWith("user_"));
        verify(stringRedisTemplate, never()).delete(anyString());
    }

    @Test
    void shouldRejectUserLoginForAdminAccount() {
        LoginFormDTO loginFormDTO = loginForm("admin_001", "123456");
        when(valueOperations.get(RedisConstants.LOGIN_CODE_KEY + "admin_001")).thenReturn("123456");
        when(userMapper.selectByWechat("admin_001")).thenReturn(user(9L, "admin", "admin_001", ADMIN_ROLE));

        Result<String> result = userService.userLogin(loginFormDTO);

        assertEquals(0, result.getCode());
        assertEquals("forbidden", result.getMsg());
    }

    @Test
    void shouldLoginExistingAdminAndStoreToken() {
        LoginFormDTO loginFormDTO = loginForm("admin_001", "123456");
        when(valueOperations.get(RedisConstants.LOGIN_CODE_KEY + "admin_001")).thenReturn("123456");
        when(userMapper.selectByWechat("admin_001")).thenReturn(user(9L, "admin", "admin_001", ADMIN_ROLE));

        Result<String> result = userService.adminLogin(loginFormDTO);

        assertEquals(1, result.getCode());
        assertNotNull(result.getData());
        assertFalse(result.getData().isBlank());
    }

    @Test
    void shouldRejectAdminLoginWhenUserDoesNotExist() {
        LoginFormDTO loginFormDTO = loginForm("admin_002", "123456");
        when(valueOperations.get(RedisConstants.LOGIN_CODE_KEY + "admin_002")).thenReturn("123456");
        when(userMapper.selectByWechat("admin_002")).thenReturn(null);

        Result<String> result = userService.adminLogin(loginFormDTO);

        assertEquals(0, result.getCode());
        assertEquals("user not found", result.getMsg());
    }

    @Test
    void shouldRejectAdminLoginForUserAccount() {
        LoginFormDTO loginFormDTO = loginForm("wechat_001", "123456");
        when(valueOperations.get(RedisConstants.LOGIN_CODE_KEY + "wechat_001")).thenReturn("123456");
        when(userMapper.selectByWechat("wechat_001")).thenReturn(user(1L, "alice", "wechat_001", DEFAULT_ROLE));

        Result<String> result = userService.adminLogin(loginFormDTO);

        assertEquals(0, result.getCode());
        assertEquals("forbidden", result.getMsg());
    }

    @Test
    void shouldRejectAdminLoginWhenCodeDoesNotMatch() {
        LoginFormDTO loginFormDTO = loginForm("admin_001", "654321");
        when(valueOperations.get(RedisConstants.LOGIN_CODE_KEY + "admin_001")).thenReturn("123456");

        Result<String> result = userService.adminLogin(loginFormDTO);

        assertEquals(0, result.getCode());
        assertEquals("verification code error", result.getMsg());
    }

    private LoginFormDTO loginForm(String wechat, String code) {
        LoginFormDTO loginFormDTO = new LoginFormDTO();
        loginFormDTO.setWechat(wechat);
        loginFormDTO.setCode(code);
        return loginFormDTO;
    }

    private User user(Long id, String username, String wechat, String role) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setWechat(wechat);
        user.setRole(role);
        return user;
    }
}
