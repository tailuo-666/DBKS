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
import scau.dbksh.controller.user.UserProductController;
import scau.dbksh.result.Result;
import scau.dbksh.service.ProductService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserProductControllerTest {

    @Mock
    private ProductService productService;

    @InjectMocks
    private UserProductController userProductController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userProductController).build();
    }

    @Test
    void shouldCreateProduct() throws Exception {
        when(productService.createProduct(any())).thenReturn(Result.success(9L));

        mockMvc.perform(post("/user/product")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"电子产品\",\"name\":\"机械键盘\",\"imageUrls\":[\"https://img.example.com/1.jpg\"],\"description\":\"九成新\",\"price\":199.00,\"wechat\":\"wx_001\",\"address\":\"宿舍楼\",\"tags\":\"键盘 外设\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").value(9));
    }

    @Test
    void shouldUpdateProduct() throws Exception {
        when(productService.updateProduct(eq(9L), any())).thenReturn(Result.success());

        mockMvc.perform(put("/user/product/9")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"电子产品\",\"name\":\"机械键盘\",\"imageUrls\":[\"https://img.example.com/1.jpg\"],\"description\":\"九成新\",\"price\":199.00,\"wechat\":\"wx_001\",\"address\":\"宿舍楼\",\"tags\":\"键盘 外设\",\"status\":\"已下架\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));
    }
}
