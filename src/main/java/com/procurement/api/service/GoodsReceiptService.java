package com.procurement.api.service;

import com.procurement.api.common.AppException;
import com.procurement.api.domain.*;
import com.procurement.api.dto.goodsreceipt.GoodsReceiptCreateRequest;
import com.procurement.api.dto.goodsreceipt.GoodsReceiptItemInput;
import com.procurement.api.dto.goodsreceipt.GoodsReceiptResponse;
import com.procurement.api.repository.GoodsReceiptRepository;
import com.procurement.api.repository.InventoryBalanceRepository;
import com.procurement.api.repository.InventoryMovementRepository;
import com.procurement.api.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Service
@Transactional
public class GoodsReceiptService {

    private final GoodsReceiptRepository goodsReceiptRepository;
    private final InventoryBalanceRepository inventoryBalanceRepository;
    private final InventoryMovementRepository inventoryMovementRepository;
    private final UserRepository userRepository;
    private final PurchaseOrderService purchaseOrderService;
    private final DocumentNumberService documentNumberService;

    public GoodsReceiptService(
            GoodsReceiptRepository goodsReceiptRepository,
            InventoryBalanceRepository inventoryBalanceRepository,
            InventoryMovementRepository inventoryMovementRepository,
            UserRepository userRepository,
            PurchaseOrderService purchaseOrderService,
            DocumentNumberService documentNumberService) {
        this.goodsReceiptRepository = goodsReceiptRepository;
        this.inventoryBalanceRepository = inventoryBalanceRepository;
        this.inventoryMovementRepository = inventoryMovementRepository;
        this.userRepository = userRepository;
        this.purchaseOrderService = purchaseOrderService;
        this.documentNumberService = documentNumberService;
    }

    /**
     * Records a Goods Receipt against a Purchase Order. The whole method runs
     * inside a single database transaction (the class-level {@code @Transactional},
     * inherited here) and performs every step the case study's "Data
     * Consistency" section lists: create the receipt, update the PO's
     * received quantities, recompute the PO status, update the warehouse
     * stock balance, and write an inventory movement. All validation happens
     * before any write; if anything fails, Spring rolls the entire
     * transaction back, so the system never ends up with (say) an inventory
     * balance bumped without a matching movement record.
     */
    public GoodsReceiptResponse create(GoodsReceiptCreateRequest request, Long receivedByUserId) {
        assertNoDuplicateProducts(request.items());

        PurchaseOrder po = purchaseOrderService.findDetailedOrThrow(request.purchaseOrderId());

        switch (po.getStatus()) {
            case CANCELLED -> throw AppException.badRequest(
                    "PURCHASE_ORDER_CANCELLED", "A CANCELLED Purchase Order cannot receive goods");
            case RECEIVED -> throw AppException.badRequest(
                    "PURCHASE_ORDER_ALREADY_RECEIVED", "This Purchase Order has already been fully received");
            case DRAFT -> throw AppException.badRequest(
                    "PURCHASE_ORDER_NOT_ORDERED", "Purchase Order must be marked as ORDERED before it can receive goods");
            default -> { /* ORDERED or PARTIALLY_RECEIVED: OK to receive */ }
        }

        Map<Long, PurchaseOrderItem> itemsByProductId = new HashMap<>();
        for (PurchaseOrderItem item : po.getItems()) {
            itemsByProductId.put(item.getProduct().getId(), item);
        }

        // Validate every line before writing anything.
        for (GoodsReceiptItemInput line : request.items()) {
            PurchaseOrderItem poItem = itemsByProductId.get(line.productId());
            if (poItem == null) {
                throw AppException.badRequest("PRODUCT_NOT_IN_PURCHASE_ORDER",
                        "Product " + line.productId() + " is not part of this Purchase Order");
            }

            int totalAfterReceipt = poItem.getReceivedQuantity() + line.quantity();
            if (totalAfterReceipt > poItem.getOrderedQuantity()) {
                throw AppException.badRequest("RECEIVED_QUANTITY_EXCEEDS_ORDERED",
                        "Receiving %d would bring total received to %d, exceeding the ordered quantity of %d"
                                .formatted(line.quantity(), totalAfterReceipt, poItem.getOrderedQuantity()));
            }
        }

        User receivedBy = userRepository.findById(receivedByUserId)
                .orElseThrow(() -> AppException.notFound("User"));

        GoodsReceipt gr = new GoodsReceipt();
        gr.setGrNumber(documentNumberService.nextGoodsReceiptNumber());
        gr.setPurchaseOrder(po);
        gr.setReceivedBy(receivedBy);

        for (GoodsReceiptItemInput line : request.items()) {
            GoodsReceiptItem grItem = new GoodsReceiptItem();
            grItem.setProduct(itemsByProductId.get(line.productId()).getProduct());
            grItem.setQuantity(line.quantity());
            gr.addItem(grItem);
        }

        // Update PO item received quantities.
        for (GoodsReceiptItemInput line : request.items()) {
            PurchaseOrderItem poItem = itemsByProductId.get(line.productId());
            poItem.setReceivedQuantity(poItem.getReceivedQuantity() + line.quantity());
        }

        // Recompute and persist the PO's overall status.
        po.setStatus(computeStatus(po));

        // Update (or create) the warehouse stock balance, then log the movement.
        for (GoodsReceiptItemInput line : request.items()) {
            Product product = itemsByProductId.get(line.productId()).getProduct();

            InventoryBalance balance = inventoryBalanceRepository
                    .findByWarehouseIdAndProductId(po.getWarehouse().getId(), product.getId())
                    .orElseGet(() -> {
                        InventoryBalance created = new InventoryBalance();
                        created.setWarehouse(po.getWarehouse());
                        created.setProduct(product);
                        created.setQuantity(0);
                        return created;
                    });
            balance.setQuantity(balance.getQuantity() + line.quantity());
            inventoryBalanceRepository.save(balance);

            InventoryMovement movement = new InventoryMovement();
            movement.setWarehouse(po.getWarehouse());
            movement.setProduct(product);
            movement.setMovementType(InventoryMovementType.PURCHASE_RECEIPT);
            movement.setQuantity(line.quantity());
            movement.setReference(gr.getGrNumber());
            inventoryMovementRepository.save(movement);
        }

        goodsReceiptRepository.save(gr);
        return GoodsReceiptResponse.from(gr);
    }

    @Transactional(readOnly = true)
    public GoodsReceiptResponse getById(Long id) {
        GoodsReceipt gr = goodsReceiptRepository.findWithDetailsById(id)
                .orElseThrow(() -> AppException.notFound("Goods Receipt"));
        return GoodsReceiptResponse.from(gr);
    }

    /**
     * Pure function over already-loaded item data - if there is no received
     * quantity yet the PO stays ORDERED, if every item's ordered quantity has
     * been fully received it becomes RECEIVED, otherwise PARTIALLY_RECEIVED.
     * DRAFT/CANCELLED are left untouched by this calculation (callers only
     * invoke it once the PO is already ORDERED or PARTIALLY_RECEIVED).
     */
    private PurchaseOrderStatus computeStatus(PurchaseOrder po) {
        int totalOrdered = po.getItems().stream().mapToInt(PurchaseOrderItem::getOrderedQuantity).sum();
        int totalReceived = po.getItems().stream().mapToInt(PurchaseOrderItem::getReceivedQuantity).sum();

        if (totalReceived <= 0) return PurchaseOrderStatus.ORDERED;
        if (totalReceived >= totalOrdered) return PurchaseOrderStatus.RECEIVED;
        return PurchaseOrderStatus.PARTIALLY_RECEIVED;
    }

    private void assertNoDuplicateProducts(java.util.List<GoodsReceiptItemInput> items) {
        Set<Long> seen = new HashSet<>();
        for (GoodsReceiptItemInput item : items) {
            if (!seen.add(item.productId())) {
                throw AppException.badRequest("DUPLICATE_PRODUCT_IN_RECEIPT",
                        "The same product cannot appear more than once in a single Goods Receipt");
            }
        }
    }
}
