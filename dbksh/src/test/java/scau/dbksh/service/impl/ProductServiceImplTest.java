package scau.dbksh.service.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import scau.dbksh.dto.ProductCreateDTO;
import scau.dbksh.dto.ProductDetailDTO;
import scau.dbksh.dto.ProductListDTO;
import scau.dbksh.dto.ProductTagRelationDTO;
import scau.dbksh.dto.ProductUpdateDTO;
import scau.dbksh.dto.UserDTO;
import scau.dbksh.entity.Product;
import scau.dbksh.entity.ProductImage;
import scau.dbksh.entity.ProductTag;
import scau.dbksh.entity.Tag;
import scau.dbksh.mapper.ProductImageMapper;
import scau.dbksh.mapper.ProductMapper;
import scau.dbksh.mapper.ProductTagMapper;
import scau.dbksh.mapper.TagMapper;
import scau.dbksh.result.Result;
import scau.dbksh.utils.UserHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductMapper productMapper;

    @Mock
    private ProductImageMapper productImageMapper;

    @Mock
    private ProductTagMapper productTagMapper;

    @Mock
    private TagMapper tagMapper;

    @InjectMocks
    private ProductServiceImpl productService;

    @AfterEach
    void tearDown() {
        UserHolder.removeUser();
    }

    @Test
    void shouldListPublishedProductsByCategory() {
        Product product = product(1L, "游戏手柄", LocalDateTime.now().minusHours(2));
        when(productMapper.selectPublishedByCategory("电子产品")).thenReturn(List.of(product));
        when(productTagMapper.selectTagNamesByProductIds(List.of(1L))).thenReturn(List.of(
                relation(1L, "二手"),
                relation(1L, "包邮")
        ));
        when(productImageMapper.selectByProductIds(List.of(1L))).thenReturn(List.of(
                image(1L, "https://img.example.com/1-1.jpg", 1),
                image(1L, "https://img.example.com/1-2.jpg", 2)
        ));

        Result<List<ProductListDTO>> result = productService.listByCategory("电子产品");

        assertEquals(1, result.getCode());
        assertEquals(1, result.getData().size());
        assertEquals("游戏手柄", result.getData().get(0).getName());
        assertEquals("https://img.example.com/1-1.jpg", result.getData().get(0).getImageUrl());
        assertEquals(List.of("二手", "包邮"), result.getData().get(0).getTags());
        assertFalse(result.getData().get(0).getRelativeTime().isBlank());
    }

    @Test
    void shouldMergeKeywordSearchResultsFromNameAndTag() {
        when(productMapper.selectPublishedIdsByNameLike("耳机")).thenReturn(List.of(1L, 2L));
        when(tagMapper.selectByNameLike("耳机")).thenReturn(List.of(tag(10L, "耳机")));
        when(productTagMapper.selectProductIdsByTagIds(List.of(10L))).thenReturn(List.of(2L, 3L));
        when(productMapper.selectPublishedByIds(anyList())).thenReturn(List.of(
                product(3L, "降噪耳机", LocalDateTime.now().minusDays(1)),
                product(2L, "蓝牙耳机", LocalDateTime.now().minusHours(3)),
                product(1L, "有线耳机", LocalDateTime.now().minusMinutes(20))
        ));
        when(productTagMapper.selectTagNamesByProductIds(anyList())).thenReturn(List.of(
                relation(1L, "耳机"),
                relation(2L, "蓝牙"),
                relation(3L, "降噪")
        ));
        when(productImageMapper.selectByProductIds(anyList())).thenReturn(List.of(
                image(1L, "https://img.example.com/1.jpg", 1),
                image(2L, "https://img.example.com/2.jpg", 1),
                image(3L, "https://img.example.com/3.jpg", 1)
        ));

        Result<List<ProductListDTO>> result = productService.searchByKeyword("耳机");

        assertEquals(1, result.getCode());
        assertEquals(3, result.getData().size());

        ArgumentCaptor<List<Long>> idCaptor = ArgumentCaptor.forClass(List.class);
        verify(productMapper).selectPublishedByIds(idCaptor.capture());
        assertEquals(List.of(1L, 2L, 3L), idCaptor.getValue());
    }

    @Test
    void shouldReturnProductDetailWithAllImages() {
        Product product = product(5L, "单反相机", LocalDateTime.now().minusDays(2));
        product.setCategory("电子产品");
        product.setWechat("wx_camera");
        product.setAddress("图书馆门口");
        when(productMapper.selectPublishedById(5L)).thenReturn(product);
        when(productImageMapper.selectByProductId(5L)).thenReturn(List.of(
                image(5L, "https://img.example.com/5-1.jpg", 1),
                image(5L, "https://img.example.com/5-2.jpg", 2)
        ));

        Result<ProductDetailDTO> result = productService.getPublishedDetail(5L);

        assertEquals(1, result.getCode());
        assertEquals("单反相机", result.getData().getName());
        assertEquals(List.of("https://img.example.com/5-1.jpg", "https://img.example.com/5-2.jpg"), result.getData().getImageUrls());
        assertEquals("wx_camera", result.getData().getWechat());
    }

    @Test
    void shouldCreateProductWithImagesAndTags() {
        UserHolder.saveUser(currentUser(7L));

        ProductCreateDTO dto = new ProductCreateDTO();
        dto.setCategory("电子产品");
        dto.setName(" 机械键盘 ");
        dto.setImageUrls(List.of(" https://img.example.com/k1.jpg ", "https://img.example.com/k2.jpg"));
        dto.setDescription(" 轴体正常 ");
        dto.setPrice(new BigDecimal("299.00"));
        dto.setWechat(" wx_keyboard ");
        dto.setAddress(" 宿舍楼下 ");
        dto.setTags("全新 包邮");

        doAnswer(invocation -> {
            Product product = invocation.getArgument(0);
            product.setId(88L);
            return 1;
        }).when(productMapper).insertProduct(any(Product.class));
        when(tagMapper.selectByNames(List.of("全新", "包邮"))).thenReturn(List.of(tag(1L, "全新")));
        doAnswer(invocation -> {
            Tag tag = invocation.getArgument(0);
            tag.setId(2L);
            return 1;
        }).when(tagMapper).insertTag(any(Tag.class));

        Result<Long> result = productService.createProduct(dto);

        assertEquals(1, result.getCode());
        assertEquals(88L, result.getData());

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productMapper).insertProduct(productCaptor.capture());
        assertEquals(7L, productCaptor.getValue().getSellerId());
        assertEquals("已上架", productCaptor.getValue().getStatus());
        assertEquals("机械键盘", productCaptor.getValue().getName());
        assertEquals("wx_keyboard", productCaptor.getValue().getWechat());

        ArgumentCaptor<List<ProductImage>> imageCaptor = ArgumentCaptor.forClass(List.class);
        verify(productImageMapper).insertBatch(imageCaptor.capture());
        assertEquals(2, imageCaptor.getValue().size());
        assertEquals("https://img.example.com/k1.jpg", imageCaptor.getValue().get(0).getImageUrl());
        assertEquals(1, imageCaptor.getValue().get(0).getSortOrder());
        assertEquals(2, imageCaptor.getValue().get(1).getSortOrder());

        ArgumentCaptor<List<ProductTag>> productTagCaptor = ArgumentCaptor.forClass(List.class);
        verify(productTagMapper).insertBatch(productTagCaptor.capture());
        assertEquals(2, productTagCaptor.getValue().size());
        assertEquals(1L, productTagCaptor.getValue().get(0).getTagId());
        assertEquals(2L, productTagCaptor.getValue().get(1).getTagId());
    }

    @Test
    void shouldRejectCreateWhenImagesEmpty() {
        ProductCreateDTO dto = new ProductCreateDTO();
        dto.setCategory("电子产品");
        dto.setName("蓝牙音箱");
        dto.setDescription("九成新");
        dto.setPrice(new BigDecimal("88.00"));
        dto.setImageUrls(List.of(" ", ""));

        Result<Long> result = productService.createProduct(dto);

        assertEquals(0, result.getCode());
        assertEquals("imageUrls cannot be empty", result.getMsg());
        verify(productMapper, never()).insertProduct(any(Product.class));
    }

    @Test
    void shouldRejectUpdateWhenProductDoesNotBelongToCurrentUser() {
        UserHolder.saveUser(currentUser(7L));

        ProductUpdateDTO dto = validUpdateDTO();
        when(productMapper.selectByIdAndSellerId(9L, 7L)).thenReturn(null);

        Result<Void> result = productService.updateProduct(9L, dto);

        assertEquals(0, result.getCode());
        assertEquals("product not found", result.getMsg());
        verify(productMapper, never()).updateProduct(any(Product.class));
    }

    @Test
    void shouldUpdateProductAndReplaceRelations() {
        UserHolder.saveUser(currentUser(7L));

        ProductUpdateDTO dto = validUpdateDTO();
        when(productMapper.selectByIdAndSellerId(9L, 7L)).thenReturn(product(9L, "旧商品", LocalDateTime.now().minusDays(3)));
        when(productMapper.updateProduct(any(Product.class))).thenReturn(1);
        when(tagMapper.selectByNames(List.of("自提"))).thenReturn(List.of(tag(5L, "自提")));

        Result<Void> result = productService.updateProduct(9L, dto);

        assertEquals(1, result.getCode());

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productMapper).updateProduct(productCaptor.capture());
        assertEquals("已下架", productCaptor.getValue().getStatus());
        assertEquals("路由器", productCaptor.getValue().getName());

        verify(productImageMapper).deleteByProductId(9L);
        verify(productTagMapper).deleteByProductId(9L);

        ArgumentCaptor<List<ProductImage>> imageCaptor = ArgumentCaptor.forClass(List.class);
        verify(productImageMapper).insertBatch(imageCaptor.capture());
        assertEquals(2, imageCaptor.getValue().size());
        assertEquals("https://img.example.com/router-1.jpg", imageCaptor.getValue().get(0).getImageUrl());

        ArgumentCaptor<List<ProductTag>> tagCaptor = ArgumentCaptor.forClass(List.class);
        verify(productTagMapper).insertBatch(tagCaptor.capture());
        assertEquals(1, tagCaptor.getValue().size());
        assertEquals(5L, tagCaptor.getValue().get(0).getTagId());
    }

    private ProductUpdateDTO validUpdateDTO() {
        ProductUpdateDTO dto = new ProductUpdateDTO();
        dto.setCategory("电子产品");
        dto.setName("路由器");
        dto.setDescription("双频可用");
        dto.setPrice(new BigDecimal("66.00"));
        dto.setWechat("wx_router");
        dto.setAddress("食堂门口");
        dto.setImageUrls(List.of("https://img.example.com/router-1.jpg", "https://img.example.com/router-2.jpg"));
        dto.setTags("自提");
        dto.setStatus("已下架");
        return dto;
    }

    private Product product(Long id, String name, LocalDateTime createTime) {
        Product product = new Product();
        product.setId(id);
        product.setName(name);
        product.setCategory("电子产品");
        product.setDescription("描述");
        product.setPrice(new BigDecimal("99.00"));
        product.setCreateTime(createTime);
        return product;
    }

    private ProductImage image(Long productId, String imageUrl, int sortOrder) {
        ProductImage image = new ProductImage();
        image.setProductId(productId);
        image.setImageUrl(imageUrl);
        image.setSortOrder(sortOrder);
        return image;
    }

    private ProductTagRelationDTO relation(Long productId, String tagName) {
        ProductTagRelationDTO relation = new ProductTagRelationDTO();
        relation.setProductId(productId);
        relation.setTagName(tagName);
        return relation;
    }

    private Tag tag(Long id, String name) {
        Tag tag = new Tag();
        tag.setId(id);
        tag.setName(name);
        return tag;
    }

    private UserDTO currentUser(Long id) {
        UserDTO userDTO = new UserDTO();
        userDTO.setId(id);
        userDTO.setUsername("user_" + id);
        userDTO.setRole("用户端");
        assertNotNull(userDTO.getUsername());
        return userDTO;
    }
}
