package com.procurement.api.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "purchase_requests")
@Getter
@Setter
@NoArgsConstructor
public class PurchaseRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_number", nullable = false, unique = true, length = 32)
    private String requestNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requested_by", nullable = false)
    private User requester;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PurchaseRequestStatus status = PurchaseRequestStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approver;

    @Column(name = "decision_at")
    private OffsetDateTime decisionAt;

    @Column(name = "decision_remarks", columnDefinition = "text")
    private String decisionRemarks;

    @OneToMany(mappedBy = "purchaseRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PurchaseRequestItem> items = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public void addItem(PurchaseRequestItem item) {
        items.add(item);
        item.setPurchaseRequest(this);
    }

    public void removeItem(PurchaseRequestItem item) {
        items.remove(item);
        item.setPurchaseRequest(null);
    }
}
