package scau.dbksh.service;

import scau.dbksh.dto.ProductCreateDTO;
import scau.dbksh.dto.ProductDetailDTO;
import scau.dbksh.dto.ProductListDTO;
import scau.dbksh.dto.ProductUpdateDTO;
import scau.dbksh.dto.MyProductListDTO;
import scau.dbksh.result.Result;

import java.util.List;

public interface ProductService {

    Result<List<ProductListDTO>> listByCategory(String category);

    Result<List<ProductListDTO>> searchByKeyword(String keyword);

    Result<ProductDetailDTO> getPublishedDetail(Long id);

    Result<List<MyProductListDTO>> listCurrentUserProducts();

    Result<Long> createProduct(ProductCreateDTO dto);

    Result<Void> updateProduct(Long id, ProductUpdateDTO dto);
}
