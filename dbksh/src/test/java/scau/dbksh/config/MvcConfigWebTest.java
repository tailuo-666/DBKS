package scau.dbksh.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import scau.dbksh.controller.admin.AdminAuthController;
import scau.dbksh.controller.admin.AdminController;
import scau.dbksh.dto.UserDTO;
import scau.dbksh.interceptor.AdminInterceptor;
import scau.dbksh.interceptor.LoginInterceptor;
import scau.dbksh.interceptor.RefreshTokenInterceptor;
import scau.dbksh.result.Result;
import scau.dbksh.service.ReportService;
import scau.dbksh.service.UserService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {AdminAuthController.class, AdminController.class})
@Import(MvcConfig.class)
class MvcConfigWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private ReportService reportService;

    @MockBean
    private RefreshTokenInterceptor refreshTokenInterceptor;

    @MockBean
    private LoginInterceptor loginInterceptor;

    @MockBean
    private AdminInterceptor adminInterceptor;

    @BeforeEach
    void setUp() throws Exception {
        when(refreshTokenInterceptor.preHandle(any(), any(), any())).thenReturn(true);
        when(loginInterceptor.preHandle(any(), any(), any())).thenReturn(true);
        when(adminInterceptor.preHandle(any(), any(), any())).thenReturn(true);
    }

    @Test
    void shouldAllowAnonymousAdminLoginWithoutLoginOrAdminInterceptor() throws Exception {
        when(userService.adminLogin(any())).thenReturn(Result.success("token_001"));

        mockMvc.perform(post("/admin/login")
                        .contentType(APPLICATION_JSON)
                        .content("{\"wechat\":\"admin_001\",\"code\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").value("token_001"));

        verify(refreshTokenInterceptor).preHandle(any(), any(), any());
        verify(loginInterceptor, never()).preHandle(any(), any(), any());
        verify(adminInterceptor, never()).preHandle(any(), any(), any());
    }

    @Test
    void shouldApplyProtectedInterceptorsForOtherAdminEndpoints() throws Exception {
        UserDTO userDTO = new UserDTO();
        userDTO.setId(1L);
        userDTO.setUsername("admin");
        userDTO.setRole("\u7ba1\u7406\u5458");
        when(userService.me()).thenReturn(Result.success(userDTO));

        mockMvc.perform(get("/admin/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.username").value("admin"));

        verify(refreshTokenInterceptor).preHandle(any(), any(), any());
        verify(loginInterceptor).preHandle(any(), any(), any());
        verify(adminInterceptor).preHandle(any(), any(), any());
    }

    @Test
    void shouldReturnNotFoundForRemovedAuthLogin() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("{\"wechat\":\"admin_001\",\"code\":\"123456\"}"))
                .andExpect(status().isNotFound());
    }
}
