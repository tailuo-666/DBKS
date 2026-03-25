package scau.dbksh.controller.user;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import scau.dbksh.dto.MyProductListDTO;
import scau.dbksh.dto.ProductCreateDTO;
import scau.dbksh.dto.ProductUpdateDTO;
import scau.dbksh.result.Result;
import scau.dbksh.service.ProductService;

import java.util.List;

@RestController
@RequestMapping("/user/product")
public class UserProductController {

    private final ProductService productService;

    public UserProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/mine")
    public Result<List<MyProductListDTO>> mine() {
        return productService.listCurrentUserProducts();
    }

    @PostMapping
    public Result<Long> create(@RequestBody ProductCreateDTO dto) {
        return productService.createProduct(dto);
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable("id") Long id, @RequestBody ProductUpdateDTO dto) {
        return productService.updateProduct(id, dto);
    }
}
