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
import scau.dbksh.controller.user.UserReportController;
import scau.dbksh.result.Result;
import scau.dbksh.service.ReportService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserReportControllerTest {

    @Mock
    private ReportService reportService;

    @InjectMocks
    private UserReportController userReportController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userReportController).build();
    }

    @Test
    void shouldCreateReport() throws Exception {
        when(reportService.createReport(any())).thenReturn(Result.success(5L));

        mockMvc.perform(post("/user/report")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":9,\"reason\":\"fake item\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").value(5));
    }
}
