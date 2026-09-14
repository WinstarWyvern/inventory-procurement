package com.procurement.api.service;

import com.procurement.api.repository.GoodsReceiptRepository;
import com.procurement.api.repository.PurchaseOrderRepository;
import com.procurement.api.repository.PurchaseRequestRepository;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.Year;
import java.time.ZoneOffset;

/**
 * Generates sequential, human-readable document numbers such as
 * PR-2026-000001, PO-2026-000001 and GR-2026-000001, by counting existing
 * rows created so far this year and adding one. This is simple and
 * sufficient for this system's expected volume; each call happens inside
 * the same transaction as the insert it numbers, which is the important
 * part for correctness (see the service methods that call these).
 */
@Service
public class DocumentNumberService {

    private final PurchaseRequestRepository purchaseRequestRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final GoodsReceiptRepository goodsReceiptRepository;

    public DocumentNumberService(
            PurchaseRequestRepository purchaseRequestRepository,
            PurchaseOrderRepository purchaseOrderRepository,
            GoodsReceiptRepository goodsReceiptRepository) {
        this.purchaseRequestRepository = purchaseRequestRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.goodsReceiptRepository = goodsReceiptRepository;
    }

    private static OffsetDateTime startOfCurrentYear() {
        return OffsetDateTime.of(Year.now().getValue(), 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    }

    private static String format(String prefix, long sequence) {
        return "%s-%d-%06d".formatted(prefix, Year.now().getValue(), sequence);
    }

    public String nextPurchaseRequestNumber() {
        long count = purchaseRequestRepository.countByCreatedAtGreaterThanEqual(startOfCurrentYear());
        return format("PR", count + 1);
    }

    public String nextPurchaseOrderNumber() {
        long count = purchaseOrderRepository.countByCreatedAtGreaterThanEqual(startOfCurrentYear());
        return format("PO", count + 1);
    }

    public String nextGoodsReceiptNumber() {
        long count = goodsReceiptRepository.countByCreatedAtGreaterThanEqual(startOfCurrentYear());
        return format("GR", count + 1);
    }
}
