package com.procurement.api.repository;

import com.procurement.api.domain.PurchaseOrder;
import com.procurement.api.domain.PurchaseOrderStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    @EntityGraph(attributePaths = {"items", "items.product", "supplier", "warehouse", "purchaseRequest"})
    Optional<PurchaseOrder> findWithDetailsById(Long id);

    @EntityGraph(attributePaths = {"items", "items.product", "supplier", "warehouse", "purchaseRequest"})
    List<PurchaseOrder> findAllByOrderByIdDesc();

    @EntityGraph(attributePaths = {"items", "items.product", "supplier", "warehouse", "purchaseRequest"})
    List<PurchaseOrder> findByStatusOrderByIdDesc(PurchaseOrderStatus status);

    Optional<PurchaseOrder> findByPurchaseRequestId(Long purchaseRequestId);

    boolean existsByPurchaseRequestId(Long purchaseRequestId);

    long countByCreatedAtGreaterThanEqual(OffsetDateTime start);
}
