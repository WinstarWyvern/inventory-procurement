package com.procurement.api.repository;

import com.procurement.api.domain.PurchaseRequestItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PurchaseRequestItemRepository extends JpaRepository<PurchaseRequestItem, Long> {

    Optional<PurchaseRequestItem> findByPurchaseRequestIdAndProductId(Long purchaseRequestId, Long productId);
}
