package com.procurement.api.repository;

import com.procurement.api.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findBySku(String sku);
    boolean existsBySku(String sku);
    boolean existsBySkuAndIdNot(String sku, Long id);
}
