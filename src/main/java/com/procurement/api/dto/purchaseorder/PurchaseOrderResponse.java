package com.procurement.api.dto.purchaseorder;

import com.procurement.api.domain.PurchaseOrder;
import com.procurement.api.domain.PurchaseOrderStatus;
import com.procurement.api.dto.supplier.SupplierResponse;
import com.procurement.api.dto.warehouse.WarehouseResponse;

import java.time.OffsetDateTime;
import java.util.List;

public record PurchaseOrderResponse(
        Long id,
        String poNumber,
        Long purchaseRequestId,
        SupplierResponse supplier,
        WarehouseResponse warehouse,
        PurchaseOrderStatus status,
        List<PurchaseOrderItemResponse> items,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static PurchaseOrderResponse from(PurchaseOrder po) {
        return new PurchaseOrderResponse(
                po.getId(),
                po.getPoNumber(),
                po.getPurchaseRequest().getId(),
                SupplierResponse.from(po.getSupplier()),
                WarehouseResponse.from(po.getWarehouse()),
                po.getStatus(),
                po.getItems().stream().map(PurchaseOrderItemResponse::from).toList(),
                po.getCreatedAt(),
                po.getUpdatedAt());
    }
}
