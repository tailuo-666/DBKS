package scau.dbksh.service.impl;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.NoTransactionException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.util.StringUtils;
import scau.dbksh.dto.MyProductListDTO;
import scau.dbksh.dto.ProductCreateDTO;
import scau.dbksh.dto.ProductDetailDTO;
import scau.dbksh.dto.ProductListDTO;
import scau.dbksh.dto.ProductTagRelationDTO;
import scau.dbksh.dto.ProductUpdateDTO;
import scau.dbksh.dto.UserDTO;
import scau.dbksh.entity.BrowseHistory;
import scau.dbksh.entity.Product;
import scau.dbksh.entity.ProductImage;
import scau.dbksh.entity.ProductTag;
import scau.dbksh.entity.Tag;
import scau.dbksh.mapper.BrowseHistoryMapper;
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

    private static final String STATUS_PUBLISHED = "\u5df2\u4e0a\u67b6";
    private static final String STATUS_OFF_SHELF = "\u5df2\u4e0b\u67b6";
    private static final Set<String> VALID_CATEGORIES = Set.of(
            "\u4e8c\u624b\u4e66",
            "\u95f2\u7f6e\u7269\u54c1",
            "\u7535\u5b50\u4ea7\u54c1",
            "\u65e5\u7528\u54c1"
    );

    private final ProductMapper productMapper;
    private final ProductImageMapper productImageMapper;
    private final ProductTagMapper productTagMapper;
    private final TagMapper tagMapper;
    private final BrowseHistoryMapper browseHistoryMapper;

    public ProductServiceImpl(
            ProductMapper productMapper,
            ProductImageMapper productImageMapper,
            ProductTagMapper productTagMapper,
            TagMapper tagMapper,
            BrowseHistoryMapper browseHistoryMapper
    ) {
        this.productMapper = productMapper;
        this.productImageMapper = productImageMapper;
        this.productTagMapper = productTagMapper;
        this.tagMapper = tagMapper;
        this.browseHistoryMapper = browseHistoryMapper;
    }

    @Override
    public Result<List<ProductListDTO>> listByCategory(String category) {
        if (!StringUtils.hasText(category) || !VALID_CATEGORIES.contains(category.trim())) {
            return Result.error("invalid category");
        }

        // 1. Query the product table first and keep only published products in this category.
        List<Product> products = productMapper.selectPublishedByCategory(category.trim());
        // 2. Load tags and cover images in batch, then assemble the list DTOs.
        return Result.success(buildProductList(products));
    }

    @Override
    public Result<List<ProductListDTO>> searchByKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return Result.error("keyword cannot be blank");
        }

        String trimmedKeyword = keyword.trim();
        // 1. Search product names first and collect the matched product ids.
        LinkedHashSet<Long> productIds = new LinkedHashSet<>(productMapper.selectPublishedIdsByNameLike(trimmedKeyword));

        // 2. Search tag names next, then convert tag ids to product ids through the relation table.
        List<Tag> tags = tagMapper.selectByNameLike(trimmedKeyword);
        if (!tags.isEmpty()) {
            List<Long> tagIds = tags.stream().map(Tag::getId).collect(Collectors.toList());
            productIds.addAll(productTagMapper.selectProductIdsByTagIds(tagIds));
        }

        if (productIds.isEmpty()) {
            return Result.success(Collections.emptyList());
        }

        // 3. Query the final deduplicated product ids in batch and fill tags plus cover images.
        List<Product> products = productMapper.selectPublishedByIds(new ArrayList<>(productIds));
        return Result.success(buildProductList(products));
    }

    @Override
    @Transactional
    public Result<ProductDetailDTO> getPublishedDetail(Long id) {
        if (id == null || id <= 0) {
            return Result.error("invalid product id");
        }

        // 1. Query the product table first and ensure the product is published.
        Product product = productMapper.selectPublishedById(id);
        if (product == null) {
            return Result.error("product not found");
        }

        // 2. If the current request is logged in, insert one browse history record in the same transaction.
        Result<Void> saveBrowseHistoryResult = saveBrowseHistory(id);
        if (saveBrowseHistoryResult.getCode() == 0) {
            markCurrentTransactionRollbackOnly();
            return Result.error(saveBrowseHistoryResult.getMsg());
        }

        // 3. Query all images after the browse record step succeeds, then assemble the detail DTO.
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
    public Result<List<MyProductListDTO>> listCurrentUserProducts() {
        UserDTO user = UserHolder.getUser();
        if (user == null || user.getId() == null) {
            return Result.error("user not logged in");
        }

        // 1. Read the current user id from the login context.
        // 2. Query all products created by this seller without filtering by status.
        List<Product> products = productMapper.selectBySellerId(user.getId());
        // 3. Fill tags and cover images in batch, then build the user-side product list.
        return Result.success(buildMyProductList(products));
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

        // 1. Read the current seller id from the login context.
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

        // 2. Insert the product first and keep the generated product id.
        int rows = productMapper.insertProduct(product);
        if (rows != 1 || product.getId() == null) {
            return Result.error("create product failed");
        }

        // 3. Insert images in the original order so sort_order stays stable.
        saveImages(product.getId(), imageUrls);
        // 4. Resolve tags and insert the relation records after the product exists.
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

        // 1. Confirm the product belongs to the current user before updating it.
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

        // 2. Replace old images with the latest image list.
        productImageMapper.deleteByProductId(id);
        saveImages(id, imageUrls);
        // 3. Replace old tag relations with the latest tag set.
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

        // 1. Collect the current batch of product ids first.
        List<Long> productIds = products.stream().map(Product::getId).collect(Collectors.toList());
        // 2. Load tags in batch to avoid N+1 queries.
        Map<Long, List<String>> tagsByProductId = loadTagsByProductId(productIds);
        // 3. Load images in batch and keep the first one as the cover image.
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

    private List<MyProductListDTO> buildMyProductList(List<Product> products) {
        if (products == null || products.isEmpty()) {
            return Collections.emptyList();
        }

        // 1. Load tags and cover images in batch for the current product set.
        List<Long> productIds = products.stream().map(Product::getId).collect(Collectors.toList());
        Map<Long, List<String>> tagsByProductId = loadTagsByProductId(productIds);
        Map<Long, String> imageByProductId = loadFirstImageByProductId(productIds);

        // 2. Build the user-side list DTO and include status for every product.
        List<MyProductListDTO> result = new ArrayList<>(products.size());
        for (Product product : products) {
            MyProductListDTO dto = new MyProductListDTO();
            dto.setId(product.getId());
            dto.setName(product.getName());
            dto.setTags(tagsByProductId.getOrDefault(product.getId(), Collections.emptyList()));
            dto.setImageUrl(imageByProductId.get(product.getId()));
            dto.setDescription(product.getDescription());
            dto.setStatus(product.getStatus());
            result.add(dto);
        }
        return result;
    }

    private Map<Long, List<String>> loadTagsByProductId(List<Long> productIds) {
        Map<Long, LinkedHashSet<String>> groupedTags = new LinkedHashMap<>();
        // Flattened product-tag rows are regrouped here by product id.
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
        // Images are already ordered by SQL, so the first one for each product becomes the cover.
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
            return "\u521a\u521a";
        }

        long minutes = seconds / 60;
        if (minutes < 60) {
            return minutes + "\u5206\u949f\u524d";
        }

        long hours = minutes / 60;
        if (hours < 24) {
            return hours + "\u5c0f\u65f6\u524d";
        }

        long days = hours / 24;
        if (days < 30) {
            return days + "\u5929\u524d";
        }

        long months = days / 30;
        if (months < 12) {
            return months + "\u4e2a\u6708\u524d";
        }
        return (months / 12) + "\u5e74\u524d";
    }

    private Result<Void> saveBrowseHistory(Long productId) {
        UserDTO user = UserHolder.getUser();
        if (user == null || user.getId() == null) {
            return Result.success();
        }

        BrowseHistory browseHistory = new BrowseHistory();
        browseHistory.setUserId(user.getId());
        browseHistory.setProductId(productId);

        try {
            // 1. Insert one history row only for logged-in requests.
            int rows = browseHistoryMapper.insertBrowseHistory(browseHistory);
            // 2. Return an error if the insert count is unexpected, so the outer transaction can roll back.
            if (rows != 1) {
                return Result.error("save browse history failed");
            }
            return Result.success();
        } catch (RuntimeException e) {
            return Result.error("save browse history failed");
        }
    }

    private void markCurrentTransactionRollbackOnly() {
        try {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        } catch (NoTransactionException ignored) {
            // Unit tests may call the service directly without a Spring transaction proxy.
        }
    }

    private void saveImages(Long productId, List<String> imageUrls) {
        List<ProductImage> images = new ArrayList<>(imageUrls.size());
        // Keep the incoming order so cover selection and detail image order stay stable.
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

        // 1. Resolve every tag name to a tag id first.
        List<Long> tagIds = resolveTagIds(tagNames);
        // 2. Insert product-tag relation rows after tag ids are ready.
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

        // Tags are split by whitespace, trimmed, and deduplicated in original order.
        LinkedHashSet<String> tagNames = Arrays.stream(rawTags.trim().split("\\s+"))
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return new ArrayList<>(tagNames);
    }

    private List<Long> resolveTagIds(List<String> tagNames) {
        // 1. Query all existing tags in batch first.
        List<Tag> existingTags = tagMapper.selectByNames(tagNames);
        Map<String, Long> tagIdMap = existingTags.stream()
                .collect(Collectors.toMap(Tag::getName, Tag::getId, (left, right) -> left, LinkedHashMap::new));

        // 2. Create missing tags and return the final ordered tag id list.
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
            // 1. Try direct insert first so the common path returns immediately.
            int rows = tagMapper.insertTag(tag);
            if (rows == 1 && tag.getId() != null) {
                return tag.getId();
            }
        } catch (DuplicateKeyException ignored) {
            // 2. Another request may have created the same tag concurrently.
        }

        // 3. Query the existing tag by name as the final fallback path.
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
