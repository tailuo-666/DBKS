package scau.dbksh.interceptor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import scau.dbksh.dto.UserDTO;
import scau.dbksh.utils.UserHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginInterceptorTest {

    private final LoginInterceptor loginInterceptor = new LoginInterceptor();

    @AfterEach
    void tearDown() {
        UserHolder.removeUser();
    }

    @Test
    void shouldReturn401WhenUserIsMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = loginInterceptor.preHandle(request, response, new Object());

        assertFalse(allowed);
        assertEquals(401, response.getStatus());
    }

    @Test
    void shouldPassWhenUserExists() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        UserDTO userDTO = new UserDTO();
        userDTO.setId(1L);
        userDTO.setUsername("alice");
        userDTO.setRole("\u7528\u6237\u7aef");
        UserHolder.saveUser(userDTO);

        boolean allowed = loginInterceptor.preHandle(request, response, new Object());

        assertTrue(allowed);
    }
}
