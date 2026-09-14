package com.procurement.api.web;

import com.procurement.api.common.DataResponse;
import com.procurement.api.domain.PurchaseOrderStatus;
import com.procurement.api.dto.purchaseorder.PurchaseOrderCreateRequest;
import com.procurement.api.dto.purchaseorder.PurchaseOrderResponse;
import com.procurement.api.security.AuthenticatedUser;
import com.procurement.api.service.PurchaseOrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/purchase-orders")
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    public PurchaseOrderController(PurchaseOrderService purchaseOrderService) {
        this.purchaseOrderService = purchaseOrderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DataResponse<PurchaseOrderResponse> create(
            @Valid @RequestBody PurchaseOrderCreateRequest request,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return DataResponse.of(purchaseOrderService.create(request, principal.id()));
    }

    @GetMapping
    public DataResponse<List<PurchaseOrderResponse>> list(
            @RequestParam(required = false) PurchaseOrderStatus status) {
        return DataResponse.of(purchaseOrderService.list(status));
    }

    @GetMapping("/{id}")
    public DataResponse<PurchaseOrderResponse> getById(@PathVariable Long id) {
        return DataResponse.of(purchaseOrderService.getById(id));
    }

    @PostMapping("/{id}/mark-ordered")
    public DataResponse<PurchaseOrderResponse> markAsOrdered(@PathVariable Long id) {
        return DataResponse.of(purchaseOrderService.markAsOrdered(id));
    }
}
