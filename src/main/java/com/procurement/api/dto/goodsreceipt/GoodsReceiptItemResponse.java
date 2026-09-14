package com.procurement.api.dto.goodsreceipt;

import com.procurement.api.domain.GoodsReceiptItem;
import com.procurement.api.dto.product.ProductResponse;

public record GoodsReceiptItemResponse(Long id, ProductResponse product, int quantity) {
    public static GoodsReceiptItemResponse from(GoodsReceiptItem item) {
        return new GoodsReceiptItemResponse(
                item.getId(), ProductResponse.from(item.getProduct()), item.getQuantity());
    }
}
