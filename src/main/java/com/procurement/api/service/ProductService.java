package com.procurement.api.service;

import com.procurement.api.common.AppException;
import com.procurement.api.domain.Product;
import com.procurement.api.dto.product.ProductCreateRequest;
import com.procurement.api.dto.product.ProductResponse;
import com.procurement.api.dto.product.ProductUpdateRequest;
import com.procurement.api.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public ProductResponse create(ProductCreateRequest request) {
        if (productRepository.existsBySku(request.sku())) {
            throw AppException.conflict("SKU_ALREADY_EXISTS",
                    "SKU '" + request.sku() + "' is already in use");
        }

        Product product = new Product();
        product.setSku(request.sku());
        product.setName(request.name());
        product.setUnit(request.unit());
        product.setActive(request.active() == null || request.active());

        return ProductResponse.from(productRepository.save(product));
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> list() {
        return productRepository.findAll().stream()
                .sorted((a, b) -> Long.compare(a.getId(), b.getId()))
                .map(ProductResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse getById(Long id) {
        return ProductResponse.from(findEntity(id));
    }

    public ProductResponse update(Long id, ProductUpdateRequest request) {
        Product product = findEntity(id);

        if (request.sku() != null && !request.sku().equals(product.getSku())) {
            if (productRepository.existsBySkuAndIdNot(request.sku(), id)) {
                throw AppException.conflict("SKU_ALREADY_EXISTS",
                        "SKU '" + request.sku() + "' is already in use");
            }
            product.setSku(request.sku());
        }
        if (request.name() != null) product.setName(request.name());
        if (request.unit() != null) product.setUnit(request.unit());
        if (request.active() != null) product.setActive(request.active());

        return ProductResponse.from(productRepository.save(product));
    }

    /** Package-visible so other services can validate + load a product entity. */
    Product findEntity(Long id) {
        return productRepository.findById(id).orElseThrow(() -> AppException.notFound("Product"));
    }

    public Product assertUsable(Long id) {
        Product product = findEntity(id);
        if (!product.isActive()) {
            throw AppException.badRequest("PRODUCT_INACTIVE",
                    "Product '" + product.getSku() + "' is not active and cannot be used for new transactions");
        }
        return product;
    }
}
