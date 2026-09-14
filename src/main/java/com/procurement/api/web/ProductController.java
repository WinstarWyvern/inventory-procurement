package com.procurement.api.web;

import com.procurement.api.common.DataResponse;
import com.procurement.api.dto.product.ProductCreateRequest;
import com.procurement.api.dto.product.ProductResponse;
import com.procurement.api.dto.product.ProductUpdateRequest;
import com.procurement.api.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DataResponse<ProductResponse> create(@Valid @RequestBody ProductCreateRequest request) {
        return DataResponse.of(productService.create(request));
    }

    @GetMapping
    public DataResponse<List<ProductResponse>> list() {
        return DataResponse.of(productService.list());
    }

    @GetMapping("/{id}")
    public DataResponse<ProductResponse> getById(@PathVariable Long id) {
        return DataResponse.of(productService.getById(id));
    }

    @PatchMapping("/{id}")
    public DataResponse<ProductResponse> update(
            @PathVariable Long id, @Valid @RequestBody ProductUpdateRequest request) {
        return DataResponse.of(productService.update(id, request));
    }
}
