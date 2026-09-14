package com.procurement.api.repository;

import com.procurement.api.domain.GoodsReceipt;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface GoodsReceiptRepository extends JpaRepository<GoodsReceipt, Long> {

    @EntityGraph(attributePaths = {
            "items", "items.product",
            "purchaseOrder", "purchaseOrder.supplier", "purchaseOrder.warehouse"
    })
    Optional<GoodsReceipt> findWithDetailsById(Long id);

    long countByCreatedAtGreaterThanEqual(OffsetDateTime start);
}
