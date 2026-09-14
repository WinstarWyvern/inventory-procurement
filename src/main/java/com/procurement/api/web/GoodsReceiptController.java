package com.procurement.api.web;

import com.procurement.api.common.DataResponse;
import com.procurement.api.dto.goodsreceipt.GoodsReceiptCreateRequest;
import com.procurement.api.dto.goodsreceipt.GoodsReceiptResponse;
import com.procurement.api.security.AuthenticatedUser;
import com.procurement.api.service.GoodsReceiptService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/goods-receipts")
public class GoodsReceiptController {

    private final GoodsReceiptService goodsReceiptService;

    public GoodsReceiptController(GoodsReceiptService goodsReceiptService) {
        this.goodsReceiptService = goodsReceiptService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DataResponse<GoodsReceiptResponse> create(
            @Valid @RequestBody GoodsReceiptCreateRequest request,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return DataResponse.of(goodsReceiptService.create(request, principal.id()));
    }

    @GetMapping("/{id}")
    public DataResponse<GoodsReceiptResponse> getById(@PathVariable Long id) {
        return DataResponse.of(goodsReceiptService.getById(id));
    }
}
