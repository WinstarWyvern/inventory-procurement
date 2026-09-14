package com.procurement.api.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * The current stock quantity of one product in one warehouse. This is a
 * materialized/cached value - the source of truth for "why" the balance is
 * what it is lives in {@link InventoryMovement}. Keeping a materialized
 * balance avoids summing the whole movement history on every stock lookup.
 */
@Entity
@Table(name = "inventory_balances",
        uniqueConstraints = @UniqueConstraint(columnNames = {"warehouse_id", "product_id"}))
@Getter
@Setter
@NoArgsConstructor
public class InventoryBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private int quantity = 0;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = OffsetDateTime.now();
    }
}
