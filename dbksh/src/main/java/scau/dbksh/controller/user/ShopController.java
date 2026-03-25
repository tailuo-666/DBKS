package scau.dbksh.controller.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import scau.dbksh.dto.ProductDetailDTO;
import scau.dbksh.dto.ProductListDTO;
import scau.dbksh.result.Result;
import scau.dbksh.service.ProductService;

import java.util.List;

@RestController
@RequestMapping("/shop")
public class ShopController {

    //private final ProductService productService;

    //public ShopController(ProductService productService) {
     //   this.productService = productService;
    //}

    @Autowired
    private ProductService productService;
    @GetMapping("/products/category")
    public Result<List<ProductListDTO>> listByCategory(@RequestParam("category") String category) {
        return productService.listByCategory(category);
    }

    @GetMapping("/products/search")
    public Result<List<ProductListDTO>> search(@RequestParam("keyword") String keyword) {
        return productService.searchByKeyword(keyword);
    }

    @GetMapping("/products/{id}")
    public Result<ProductDetailDTO> detail(@PathVariable("id") Long id) {
        return productService.getPublishedDetail(id);
    }
}
