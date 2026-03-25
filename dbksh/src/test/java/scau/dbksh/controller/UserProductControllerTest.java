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
import scau.dbksh.dto.MyProductListDTO;
import scau.dbksh.result.Result;
import scau.dbksh.service.ProductService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
    void shouldListCurrentUserProducts() throws Exception {
        MyProductListDTO dto = new MyProductListDTO();
        dto.setId(11L);
        dto.setName("router");
        dto.setTags(List.of("tag1", "tag2"));
        dto.setImageUrl("https://img.example.com/router.jpg");
        dto.setDescription("desc");
        dto.setStatus("\u5ba1\u6838\u4e2d");
        when(productService.listCurrentUserProducts()).thenReturn(Result.success(List.of(dto)));

        mockMvc.perform(get("/user/product/mine"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data[0].status").value("\u5ba1\u6838\u4e2d"))
                .andExpect(jsonPath("$.data[0].price").doesNotExist())
                .andExpect(jsonPath("$.data[0].relativeTime").doesNotExist());
    }

    @Test
    void shouldCreateProduct() throws Exception {
        when(productService.createProduct(any())).thenReturn(Result.success(9L));

        mockMvc.perform(post("/user/product")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"\u7535\u5b50\u4ea7\u54c1\",\"name\":\"keyboard\",\"imageUrls\":[\"https://img.example.com/1.jpg\"],\"description\":\"desc\",\"price\":199.00,\"wechat\":\"wx_001\",\"address\":\"dorm\",\"tags\":\"tag1 tag2\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").value(9));
    }

    @Test
    void shouldUpdateProduct() throws Exception {
        when(productService.updateProduct(eq(9L), any())).thenReturn(Result.success());

        mockMvc.perform(put("/user/product/9")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"\u7535\u5b50\u4ea7\u54c1\",\"name\":\"keyboard\",\"imageUrls\":[\"https://img.example.com/1.jpg\"],\"description\":\"desc\",\"price\":199.00,\"wechat\":\"wx_001\",\"address\":\"dorm\",\"tags\":\"tag1 tag2\",\"status\":\"\u5df2\u4e0b\u67b6\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));
    }
}
