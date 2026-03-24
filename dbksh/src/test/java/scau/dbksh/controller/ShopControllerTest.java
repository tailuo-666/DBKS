package scau.dbksh.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import scau.dbksh.controller.user.ShopController;
import scau.dbksh.dto.ProductDetailDTO;
import scau.dbksh.dto.ProductListDTO;
import scau.dbksh.result.Result;
import scau.dbksh.service.ProductService;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ShopControllerTest {

    @Mock
    private ProductService productService;

    @InjectMocks
    private ShopController shopController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(shopController).build();
    }

    @Test
    void shouldListProductsByCategory() throws Exception {
        ProductListDTO dto = new ProductListDTO();
        dto.setId(1L);
        dto.setName("Java编程思想");
        dto.setTags(List.of("教材", "二手"));
        dto.setPrice(new BigDecimal("39.90"));
        dto.setImageUrl("https://img.example.com/1.jpg");
        dto.setRelativeTime("2小时前");
        dto.setDescription("八成新");
        when(productService.listByCategory("二手书")).thenReturn(Result.success(List.of(dto)));

        mockMvc.perform(get("/shop/products/category").param("category", "二手书"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data[0].name").value("Java编程思想"))
                .andExpect(jsonPath("$.data[0].tags[0]").value("教材"));
    }

    @Test
    void shouldSearchProducts() throws Exception {
        ProductListDTO dto = new ProductListDTO();
        dto.setId(2L);
        dto.setName("蓝牙耳机");
        dto.setTags(List.of("耳机"));
        dto.setPrice(new BigDecimal("129.00"));
        dto.setImageUrl("https://img.example.com/2.jpg");
        dto.setRelativeTime("刚刚");
        dto.setDescription("几乎全新");
        when(productService.searchByKeyword("耳机")).thenReturn(Result.success(List.of(dto)));

        mockMvc.perform(get("/shop/products/search").param("keyword", "耳机"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data[0].name").value("蓝牙耳机"));
    }

    @Test
    void shouldReturnProductDetail() throws Exception {
        ProductDetailDTO detailDTO = new ProductDetailDTO();
        detailDTO.setId(3L);
        detailDTO.setName("小米手环");
        detailDTO.setCategory("电子产品");
        detailDTO.setImageUrls(List.of("https://img.example.com/3-1.jpg", "https://img.example.com/3-2.jpg"));
        detailDTO.setDescription("功能正常");
        detailDTO.setPrice(new BigDecimal("99.00"));
        detailDTO.setWechat("wechat_003");
        detailDTO.setAddress("教学楼A区");
        when(productService.getPublishedDetail(3L)).thenReturn(Result.success(detailDTO));

        mockMvc.perform(get("/shop/products/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.category").value("电子产品"))
                .andExpect(jsonPath("$.data.imageUrls[1]").value("https://img.example.com/3-2.jpg"));
    }
}
