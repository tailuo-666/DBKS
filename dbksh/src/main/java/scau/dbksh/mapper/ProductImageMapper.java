package scau.dbksh.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import scau.dbksh.entity.ProductImage;

import java.util.List;

@Mapper
public interface ProductImageMapper {

    List<ProductImage> selectByProductIds(@Param("productIds") List<Long> productIds);

    List<ProductImage> selectByProductId(@Param("productId") Long productId);

    int insertBatch(@Param("images") List<ProductImage> images);

    int deleteByProductId(@Param("productId") Long productId);
}
