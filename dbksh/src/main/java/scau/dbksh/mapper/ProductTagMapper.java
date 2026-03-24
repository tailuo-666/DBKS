package scau.dbksh.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import scau.dbksh.dto.ProductTagRelationDTO;
import scau.dbksh.entity.ProductTag;

import java.util.List;

@Mapper
public interface ProductTagMapper {

    List<Long> selectProductIdsByTagIds(@Param("tagIds") List<Long> tagIds);

    List<ProductTagRelationDTO> selectTagNamesByProductIds(@Param("productIds") List<Long> productIds);

    int insertBatch(@Param("productTags") List<ProductTag> productTags);

    int deleteByProductId(@Param("productId") Long productId);
}
