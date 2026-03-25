package scau.dbksh.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import scau.dbksh.controller.admin.AdminController;
import scau.dbksh.dto.AdminReportDTO;
import scau.dbksh.dto.UserDTO;
import scau.dbksh.result.Result;
import scau.dbksh.service.ReportService;
import scau.dbksh.service.UserService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private ReportService reportService;

    @InjectMocks
    private AdminController adminController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminController).build();
    }

    @Test
    void shouldReturnAdminProfile() throws Exception {
        UserDTO userDTO = new UserDTO();
        userDTO.setId(1L);
        userDTO.setUsername("admin");
        userDTO.setRole("\u7ba1\u7406\u5458");
        when(userService.me()).thenReturn(Result.success(userDTO));

        mockMvc.perform(get("/admin/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.username").value("admin"));
    }

    @Test
    void shouldListReports() throws Exception {
        AdminReportDTO dto = new AdminReportDTO();
        dto.setId(3L);
        dto.setUserId(7L);
        dto.setProductId(5L);
        dto.setSellerId(9L);
        dto.setReason("spam");
        dto.setStatus("\u5f85\u5904\u7406");
        dto.setProductName("keyboard");
        dto.setProductStatus("\u5ba1\u6838\u4e2d");
        dto.setCreateTime(LocalDateTime.of(2026, 3, 25, 10, 0));
        when(reportService.listReportsForAdmin()).thenReturn(Result.success(List.of(dto)));

        mockMvc.perform(get("/admin/reports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data[0].productName").value("keyboard"))
                .andExpect(jsonPath("$.data[0].status").value("\u5f85\u5904\u7406"));
    }

    @Test
    void shouldHandleReport() throws Exception {
        when(reportService.handleReport(eq(3L), any())).thenReturn(Result.success());

        mockMvc.perform(put("/admin/reports/3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productStatus\":\"\\u5df2\\u4e0b\\u67b6\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));
    }
}
