package com.procurement.api.dto.goodsreceipt;

import com.procurement.api.domain.GoodsReceipt;
import com.procurement.api.dto.common.UserSummaryResponse;
import com.procurement.api.dto.purchaseorder.PurchaseOrderResponse;

import java.time.OffsetDateTime;
import java.util.List;

public record GoodsReceiptResponse(
        Long id,
        String grNumber,
        PurchaseOrderResponse purchaseOrder,
        UserSummaryResponse receivedBy,
        List<GoodsReceiptItemResponse> items,
        OffsetDateTime createdAt
) {
    public static GoodsReceiptResponse from(GoodsReceipt gr) {
        return new GoodsReceiptResponse(
                gr.getId(),
                gr.getGrNumber(),
                PurchaseOrderResponse.from(gr.getPurchaseOrder()),
                UserSummaryResponse.from(gr.getReceivedBy()),
                gr.getItems().stream().map(GoodsReceiptItemResponse::from).toList(),
                gr.getCreatedAt());
    }
}
