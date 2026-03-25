package scau.dbksh.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import scau.dbksh.entity.Product;

import java.util.List;

@Mapper
public interface ProductMapper {

    List<Product> selectPublishedByCategory(@Param("category") String category);

    List<Long> selectPublishedIdsByNameLike(@Param("keyword") String keyword);

    List<Product> selectPublishedByIds(@Param("ids") List<Long> ids);

    Product selectPublishedById(@Param("id") Long id);

    List<Product> selectBySellerId(@Param("sellerId") Long sellerId);

    Product selectByIdAndSellerId(@Param("id") Long id, @Param("sellerId") Long sellerId);

    int insertProduct(Product product);

    int updateProduct(Product product);
}
