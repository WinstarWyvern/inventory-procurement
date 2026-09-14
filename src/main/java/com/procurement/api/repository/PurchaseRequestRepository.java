package com.procurement.api.repository;

import com.procurement.api.domain.PurchaseRequest;
import com.procurement.api.domain.PurchaseRequestStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface PurchaseRequestRepository extends JpaRepository<PurchaseRequest, Long> {

    @EntityGraph(attributePaths = {"items", "items.product", "warehouse", "requester", "approver"})
    Optional<PurchaseRequest> findWithDetailsById(Long id);

    @EntityGraph(attributePaths = {"items", "items.product", "warehouse", "requester", "approver"})
    List<PurchaseRequest> findAllByOrderByIdDesc();

    @EntityGraph(attributePaths = {"items", "items.product", "warehouse", "requester", "approver"})
    List<PurchaseRequest> findByStatusOrderByIdDesc(PurchaseRequestStatus status);

    long countByCreatedAtGreaterThanEqual(OffsetDateTime start);

    @Query("SELECT COUNT(i) FROM PurchaseRequestItem i WHERE i.purchaseRequest.id = :purchaseRequestId")
    long countItems(Long purchaseRequestId);
}
