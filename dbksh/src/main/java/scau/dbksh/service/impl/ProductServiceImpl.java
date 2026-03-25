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

        // 1. 先查商品主表，只保留当前类别下“已上架”的商品。
        List<Product> products = productMapper.selectPublishedByCategory(category.trim());
        // 2. 再批量补齐标签和主图，最后组装商品列表返回。
        return Result.success(buildProductList(products));
    }

    @Override
    public Result<List<ProductListDTO>> searchByKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return Result.error("keyword cannot be blank");
        }

        String trimmedKeyword = keyword.trim();
        // 1. 先按商品名称模糊查询，收集命中的商品 id。
        LinkedHashSet<Long> productIds = new LinkedHashSet<>(productMapper.selectPublishedIdsByNameLike(trimmedKeyword));

        // 2. 再按标签名称模糊查询，先拿到 tag.id，再通过关联表换成商品 id。
        List<Tag> tags = tagMapper.selectByNameLike(trimmedKeyword);
        if (!tags.isEmpty()) {
            List<Long> tagIds = tags.stream().map(Tag::getId).collect(Collectors.toList());
            productIds.addAll(productTagMapper.selectProductIdsByTagIds(tagIds));
        }

        if (productIds.isEmpty()) {
            return Result.success(Collections.emptyList());
        }

        // 3. 最后按去重后的商品 id 批量回查商品，并补齐标签和主图。
        List<Product> products = productMapper.selectPublishedByIds(new ArrayList<>(productIds));
        return Result.success(buildProductList(products));
    }

    @Override
    @Transactional
    public Result<ProductDetailDTO> getPublishedDetail(Long id) {
        if (id == null || id <= 0) {
            return Result.error("invalid product id");
        }

        // 1. 先查商品主表，确认商品存在且状态为“已上架”。
        Product product = productMapper.selectPublishedById(id);
        if (product == null) {
            return Result.error("product not found");
        }

        // 2. 再判断当前请求是否已登录；已登录才在同一个事务里写入一条浏览记录。
        Result<Void> saveBrowseHistoryResult = saveBrowseHistory(id);
        if (saveBrowseHistoryResult.getCode() == 0) {
            markCurrentTransactionRollbackOnly();
            return Result.error(saveBrowseHistoryResult.getMsg());
        }

        // 3. 浏览记录处理完成后，再查询商品全部图片并组装详情数据。
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

        // 1. 先从登录态里拿当前用户 id。
        // 2. 再按 seller_id 查询该用户发布过的全部商品，不过滤状态。
        List<Product> products = productMapper.selectBySellerId(user.getId());
        // 3. 最后批量补齐标签和主图，组装“我的商品列表”。
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

        // 1. 先从登录态里拿当前发布人的 user_id，写入 seller_id。
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

        // 2. 先写商品主表，拿到新生成的 productId。
        int rows = productMapper.insertProduct(product);
        if (rows != 1 || product.getId() == null) {
            return Result.error("create product failed");
        }

        // 3. 再按前端传入顺序写入图片，保证 sort_order 稳定。
        saveImages(product.getId(), imageUrls);
        // 4. 最后处理标签，先确保 tag 存在，再写商品和标签的关联表。
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

        // 1. 先确认当前商品属于当前登录用户，不能修改别人的商品。
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

        // 2. 更新主表成功后，先删旧图片，再按最新内容重建图片关系。
        productImageMapper.deleteByProductId(id);
        saveImages(id, imageUrls);
        // 3. 再删旧标签关联，并按最新标签重写关联表。
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

        // 1. 先收集当前批次的商品 id。
        List<Long> productIds = products.stream().map(Product::getId).collect(Collectors.toList());
        // 2. 再批量查标签，避免逐条查询产生 N+1。
        Map<Long, List<String>> tagsByProductId = loadTagsByProductId(productIds);
        // 3. 最后批量查图片，并取每个商品排序最靠前的一张作为主图。
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

        // 1. 先批量查询当前商品集合的标签和主图。
        List<Long> productIds = products.stream().map(Product::getId).collect(Collectors.toList());
        Map<Long, List<String>> tagsByProductId = loadTagsByProductId(productIds);
        Map<Long, String> imageByProductId = loadFirstImageByProductId(productIds);

        // 2. 再组装用户侧商品列表，并额外返回每个商品的状态。
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
        // 关联查询返回的是“商品 + 标签”的平铺结果，这里按商品 id 重新归并成列表。
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
        // SQL 已按商品和排序顺序排好，这里保留每个商品遇到的第一张图片即可。
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
            // 1. 只有当前请求已登录时，才向 browse_history 写入一条浏览记录。
            int rows = browseHistoryMapper.insertBrowseHistory(browseHistory);
            // 2. 如果写入条数不正确，就返回失败，让外层事务统一回滚。
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
            // 单元测试直接调用 service 时没有事务代理，这里只做兜底保护。
        }
    }

    private void saveImages(Long productId, List<String> imageUrls) {
        List<ProductImage> images = new ArrayList<>(imageUrls.size());
        // 按前端传入顺序写 sort_order，保证主图选择和详情图顺序稳定。
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

        // 1. 先把每个标签名称解析成对应的 tag id。
        List<Long> tagIds = resolveTagIds(tagNames);
        // 2. 再写商品和标签的关联记录。
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

        // 标签由前端按空格分隔传入，这里做 trim、分割和去重。
        LinkedHashSet<String> tagNames = Arrays.stream(rawTags.trim().split("\\s+"))
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return new ArrayList<>(tagNames);
    }

    private List<Long> resolveTagIds(List<String> tagNames) {
        // 1. 先批量查询已经存在的标签。
        List<Tag> existingTags = tagMapper.selectByNames(tagNames);
        Map<String, Long> tagIdMap = existingTags.stream()
                .collect(Collectors.toMap(Tag::getName, Tag::getId, (left, right) -> left, LinkedHashMap::new));

        // 2. 不存在的标签再补创建，最后按原顺序返回完整的 tagId 列表。
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
            // 1. 先尝试直接插入，单线程场景下一次就能拿到新 tagId。
            int rows = tagMapper.insertTag(tag);
            if (rows == 1 && tag.getId() != null) {
                return tag.getId();
            }
        } catch (DuplicateKeyException ignored) {
            // 2. 并发场景下，可能别的请求刚好创建了同名标签。
        }

        // 3. 插入失败或并发冲突时，按名称回查一次，拿到最终 tagId。
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
