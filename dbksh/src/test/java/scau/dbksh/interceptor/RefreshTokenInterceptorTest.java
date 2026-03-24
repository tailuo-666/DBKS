package scau.dbksh.interceptor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import scau.dbksh.constants.RedisConstants;
import scau.dbksh.dto.UserDTO;
import scau.dbksh.utils.UserHolder;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenInterceptorTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @InjectMocks
    private RefreshTokenInterceptor refreshTokenInterceptor;

    @BeforeEach
    void setUp() {
        when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
    }

    @AfterEach
    void tearDown() {
        UserHolder.removeUser();
    }

    @Test
    void shouldPassThroughWhenTokenIsMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertTrue(refreshTokenInterceptor.preHandle(request, response, new Object()));
        assertNull(UserHolder.getUser());
        verifyNoInteractions(hashOperations);
    }

    @Test
    void shouldRestoreUserAndRefreshTtlWhenTokenExists() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("authorization", "token_001");
        MockHttpServletResponse response = new MockHttpServletResponse();

        Map<Object, Object> userMap = new HashMap<>();
        userMap.put("id", "1");
        userMap.put("username", "alice");
        userMap.put("role", "\u7528\u6237\u7aef");
        when(hashOperations.entries(RedisConstants.LOGIN_USER_KEY + "token_001")).thenReturn(userMap);

        assertTrue(refreshTokenInterceptor.preHandle(request, response, new Object()));

        UserDTO userDTO = UserHolder.getUser();
        assertEquals(1L, userDTO.getId());
        assertEquals("alice", userDTO.getUsername());
        assertEquals("\u7528\u6237\u7aef", userDTO.getRole());
        verify(stringRedisTemplate).expire(
                RedisConstants.LOGIN_USER_KEY + "token_001",
                RedisConstants.LOGIN_USER_TTL,
                TimeUnit.MINUTES
        );

        refreshTokenInterceptor.afterCompletion(request, response, new Object(), null);
        assertNull(UserHolder.getUser());
    }
}
