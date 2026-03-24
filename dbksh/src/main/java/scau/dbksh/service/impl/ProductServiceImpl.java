package scau.dbksh.service.impl;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
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
import scau.dbksh.service.ProductService;
import scau.dbksh.utils.UserHolder;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl implements ProductService {

    private static final String STATUS_PUBLISHED = "已上架";
    private static final String STATUS_OFF_SHELF = "已下架";
    private static final Set<String> VALID_CATEGORIES = Set.of("二手书", "闲置物品", "电子产品", "日用品");

    private final ProductMapper productMapper;
    private final ProductImageMapper productImageMapper;
    private final ProductTagMapper productTagMapper;
    private final TagMapper tagMapper;

    public ProductServiceImpl(
            ProductMapper productMapper,
            ProductImageMapper productImageMapper,
            ProductTagMapper productTagMapper,
            TagMapper tagMapper
    ) {
        this.productMapper = productMapper;
        this.productImageMapper = productImageMapper;
        this.productTagMapper = productTagMapper;
        this.tagMapper = tagMapper;
    }

    @Override
    public Result<List<ProductListDTO>> listByCategory(String category) {
        if (!StringUtils.hasText(category) || !VALID_CATEGORIES.contains(category.trim())) {
            return Result.error("invalid category");
        }
        List<Product> products = productMapper.selectPublishedByCategory(category.trim());
        return Result.success(buildProductList(products));
    }

    @Override
    public Result<List<ProductListDTO>> searchByKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return Result.error("keyword cannot be blank");
        }

        String trimmedKeyword = keyword.trim();
        LinkedHashSet<Long> productIds = new LinkedHashSet<>(productMapper.selectPublishedIdsByNameLike(trimmedKeyword));

        List<Tag> tags = tagMapper.selectByNameLike(trimmedKeyword);
        if (!tags.isEmpty()) {
            List<Long> tagIds = tags.stream().map(Tag::getId).collect(Collectors.toList());
            productIds.addAll(productTagMapper.selectProductIdsByTagIds(tagIds));
        }

        if (productIds.isEmpty()) {
            return Result.success(Collections.emptyList());
        }

        List<Product> products = productMapper.selectPublishedByIds(new ArrayList<>(productIds));
        return Result.success(buildProductList(products));
    }

    @Override
    public Result<ProductDetailDTO> getPublishedDetail(Long id) {
        if (id == null || id <= 0) {
            return Result.error("invalid product id");
        }

        Product product = productMapper.selectPublishedById(id);
        if (product == null) {
            return Result.error("product not found");
        }

        List<ProductImage> images = productImageMapper.selectByProductId(id);
        ProductDetailDTO detailDTO = new ProductDetailDTO();
        detailDTO.setId(product.getId());
        detailDTO.setName(product.getName());
        detailDTO.setCategory(product.getCategory());
        detailDTO.setImageUrls(images.stream().map(ProductImage::getImageUrl).collect(Collectors.toList()));
        detailDTO.setDescription(product.getDescription());
        detailDTO.setPrice(product.getPrice());
        detailDTO.setWechat(product.getWechat());
        detailDTO.setAddress(product.getAddress());
        return Result.success(detailDTO);
    }

    @Override
    @Transactional
    public Result<Long> createProduct(ProductCreateDTO dto) {
        if (dto == null) {
            return Result.error("request body cannot be null");
        }

        List<String> imageUrls = normalizeImageUrls(dto.getImageUrls());
        String validationMessage = validateBaseFields(
                dto.getCategory(),
                dto.getName(),
                dto.getDescription(),
                dto.getPrice(),
                imageUrls
        );
        if (validationMessage != null) {
            return Result.error(validationMessage);
        }

        UserDTO user = UserHolder.getUser();
        if (user == null || user.getId() == null) {
            return Result.error("user not logged in");
        }

        Product product = new Product();
        product.setSellerId(user.getId());
        product.setCategory(dto.getCategory().trim());
        product.setName(dto.getName().trim());
        product.setDescription(dto.getDescription().trim());
        product.setPrice(dto.getPrice());
        product.setWechat(trimToNull(dto.getWechat()));
        product.setAddress(trimToNull(dto.getAddress()));
        product.setStatus(STATUS_PUBLISHED);

        int rows = productMapper.insertProduct(product);
        if (rows != 1 || product.getId() == null) {
            return Result.error("create product failed");
        }

        saveImages(product.getId(), imageUrls);
        saveTags(product.getId(), dto.getTags());
        return Result.success(product.getId());
    }

    @Override
    @Transactional
    public Result<Void> updateProduct(Long id, ProductUpdateDTO dto) {
        if (id == null || id <= 0) {
            return Result.error("invalid product id");
        }
        if (dto == null) {
            return Result.error("request body cannot be null");
        }
        if (!STATUS_PUBLISHED.equals(dto.getStatus()) && !STATUS_OFF_SHELF.equals(dto.getStatus())) {
            return Result.error("invalid status");
        }

        List<String> imageUrls = normalizeImageUrls(dto.getImageUrls());
        String validationMessage = validateBaseFields(
                dto.getCategory(),
                dto.getName(),
                dto.getDescription(),
                dto.getPrice(),
                imageUrls
        );
        if (validationMessage != null) {
            return Result.error(validationMessage);
        }

        UserDTO user = UserHolder.getUser();
        if (user == null || user.getId() == null) {
            return Result.error("user not logged in");
        }

        Product existingProduct = productMapper.selectByIdAndSellerId(id, user.getId());
        if (existingProduct == null) {
            return Result.error("product not found");
        }

        Product product = new Product();
        product.setId(id);
        product.setSellerId(user.getId());
        product.setCategory(dto.getCategory().trim());
        product.setName(dto.getName().trim());
        product.setDescription(dto.getDescription().trim());
        product.setPrice(dto.getPrice());
        product.setWechat(trimToNull(dto.getWechat()));
        product.setAddress(trimToNull(dto.getAddress()));
        product.setStatus(dto.getStatus());

        int rows = productMapper.updateProduct(product);
        if (rows != 1) {
            return Result.error("update product failed");
        }

        productImageMapper.deleteByProductId(id);
        saveImages(id, imageUrls);
        productTagMapper.deleteByProductId(id);
        saveTags(id, dto.getTags());
        return Result.success();
    }

    private String validateBaseFields(
            String category,
            String name,
            String description,
            BigDecimal price,
            List<String> imageUrls
    ) {
        if (!StringUtils.hasText(category) || !VALID_CATEGORIES.contains(category.trim())) {
            return "invalid category";
        }
        if (!StringUtils.hasText(name)) {
            return "name cannot be blank";
        }
        if (!StringUtils.hasText(description)) {
            return "description cannot be blank";
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            return "price must be greater than 0";
        }
        if (imageUrls.isEmpty()) {
            return "imageUrls cannot be empty";
        }
        return null;
    }

    private List<String> normalizeImageUrls(List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return Collections.emptyList();
        }
        return imageUrls.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.toList());
    }

    private List<ProductListDTO> buildProductList(List<Product> products) {
        if (products == null || products.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> productIds = products.stream().map(Product::getId).collect(Collectors.toList());
        Map<Long, List<String>> tagsByProductId = loadTagsByProductId(productIds);
        Map<Long, String> imageByProductId = loadFirstImageByProductId(productIds);

        List<ProductListDTO> result = new ArrayList<>(products.size());
        for (Product product : products) {
            ProductListDTO dto = new ProductListDTO();
            dto.setId(product.getId());
            dto.setName(product.getName());
            dto.setTags(tagsByProductId.getOrDefault(product.getId(), Collections.emptyList()));
            dto.setPrice(product.getPrice());
            dto.setImageUrl(imageByProductId.get(product.getId()));
            dto.setRelativeTime(formatRelativeTime(product.getCreateTime()));
            dto.setDescription(product.getDescription());
            result.add(dto);
        }
        return result;
    }

    private Map<Long, List<String>> loadTagsByProductId(List<Long> productIds) {
        Map<Long, LinkedHashSet<String>> groupedTags = new LinkedHashMap<>();
        for (ProductTagRelationDTO relation : productTagMapper.selectTagNamesByProductIds(productIds)) {
            groupedTags.computeIfAbsent(relation.getProductId(), key -> new LinkedHashSet<>()).add(relation.getTagName());
        }

        Map<Long, List<String>> result = new LinkedHashMap<>();
        for (Map.Entry<Long, LinkedHashSet<String>> entry : groupedTags.entrySet()) {
            result.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        return result;
    }

    private Map<Long, String> loadFirstImageByProductId(List<Long> productIds) {
        Map<Long, String> imageByProductId = new LinkedHashMap<>();
        for (ProductImage image : productImageMapper.selectByProductIds(productIds)) {
            imageByProductId.putIfAbsent(image.getProductId(), image.getImageUrl());
        }
        return imageByProductId;
    }

    private String formatRelativeTime(LocalDateTime createTime) {
        if (createTime == null) {
            return "";
        }

        long seconds = Math.max(Duration.between(createTime, LocalDateTime.now()).getSeconds(), 0);
        if (seconds < 60) {
            return "刚刚";
        }

        long minutes = seconds / 60;
        if (minutes < 60) {
            return minutes + "分钟前";
        }

        long hours = minutes / 60;
        if (hours < 24) {
            return hours + "小时前";
        }

        long days = hours / 24;
        if (days < 30) {
            return days + "天前";
        }

        long months = days / 30;
        if (months < 12) {
            return months + "个月前";
        }
        return (months / 12) + "年前";
    }

    private void saveImages(Long productId, List<String> imageUrls) {
        List<ProductImage> images = new ArrayList<>(imageUrls.size());
        for (int i = 0; i < imageUrls.size(); i++) {
            ProductImage image = new ProductImage();
            image.setProductId(productId);
            image.setImageUrl(imageUrls.get(i));
            image.setSortOrder(i + 1);
            images.add(image);
        }
        productImageMapper.insertBatch(images);
    }

    private void saveTags(Long productId, String rawTags) {
        List<String> tagNames = parseTags(rawTags);
        if (tagNames.isEmpty()) {
            return;
        }

        List<Long> tagIds = resolveTagIds(tagNames);
        List<ProductTag> productTags = new ArrayList<>(tagIds.size());
        for (Long tagId : tagIds) {
            ProductTag productTag = new ProductTag();
            productTag.setProductId(productId);
            productTag.setTagId(tagId);
            productTags.add(productTag);
        }
        productTagMapper.insertBatch(productTags);
    }

    private List<String> parseTags(String rawTags) {
        if (!StringUtils.hasText(rawTags)) {
            return Collections.emptyList();
        }

        LinkedHashSet<String> tagNames = Arrays.stream(rawTags.trim().split("\\s+"))
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return new ArrayList<>(tagNames);
    }

    private List<Long> resolveTagIds(List<String> tagNames) {
        List<Tag> existingTags = tagMapper.selectByNames(tagNames);
        Map<String, Long> tagIdMap = existingTags.stream()
                .collect(Collectors.toMap(Tag::getName, Tag::getId, (left, right) -> left, LinkedHashMap::new));

        List<Long> tagIds = new ArrayList<>(tagNames.size());
        for (String tagName : tagNames) {
            Long tagId = tagIdMap.get(tagName);
            if (tagId == null) {
                tagId = createOrLoadTagId(tagName);
                tagIdMap.put(tagName, tagId);
            }
            tagIds.add(tagId);
        }
        return tagIds;
    }

    private Long createOrLoadTagId(String tagName) {
        Tag tag = new Tag();
        tag.setName(tagName);
        try {
            int rows = tagMapper.insertTag(tag);
            if (rows == 1 && tag.getId() != null) {
                return tag.getId();
            }
        } catch (DuplicateKeyException ignored) {
            // Another request may have inserted the same tag concurrently.
        }

        Tag existingTag = tagMapper.selectByName(tagName);
        if (existingTag == null || existingTag.getId() == null) {
            throw new IllegalStateException("load tag failed");
        }
        return existingTag.getId();
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
