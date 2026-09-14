package com.procurement.api.web;

import com.procurement.api.common.DataResponse;
import com.procurement.api.domain.PurchaseRequestStatus;
import com.procurement.api.dto.purchaserequest.*;
import com.procurement.api.security.AuthenticatedUser;
import com.procurement.api.service.PurchaseRequestService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/purchase-requests")
public class PurchaseRequestController {

    private final PurchaseRequestService purchaseRequestService;

    public PurchaseRequestController(PurchaseRequestService purchaseRequestService) {
        this.purchaseRequestService = purchaseRequestService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DataResponse<PurchaseRequestResponse> create(
            @Valid @RequestBody PurchaseRequestCreateRequest request,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return DataResponse.of(purchaseRequestService.create(request, principal.id()));
    }

    @GetMapping
    public DataResponse<List<PurchaseRequestResponse>> list(
            @RequestParam(required = false) PurchaseRequestStatus status) {
        return DataResponse.of(purchaseRequestService.list(status));
    }

    @GetMapping("/{id}")
    public DataResponse<PurchaseRequestResponse> getById(@PathVariable Long id) {
        return DataResponse.of(purchaseRequestService.getById(id));
    }

    @PatchMapping("/{id}")
    public DataResponse<PurchaseRequestResponse> updateWarehouse(
            @PathVariable Long id, @Valid @RequestBody PurchaseRequestWarehouseUpdateRequest request) {
        return DataResponse.of(purchaseRequestService.updateWarehouse(id, request));
    }

    @PostMapping("/{id}/items")
    @ResponseStatus(HttpStatus.CREATED)
    public DataResponse<PurchaseRequestResponse> addItem(
            @PathVariable Long id, @Valid @RequestBody PurchaseRequestItemInput input) {
        return DataResponse.of(purchaseRequestService.addItem(id, input));
    }

    @PatchMapping("/{id}/items/{itemId}")
    public DataResponse<PurchaseRequestResponse> updateItem(
            @PathVariable Long id, @PathVariable Long itemId,
            @Valid @RequestBody ItemQuantityUpdateRequest request) {
        return DataResponse.of(purchaseRequestService.updateItemQuantity(id, itemId, request));
    }

    @DeleteMapping("/{id}/items/{itemId}")
    public DataResponse<PurchaseRequestResponse> removeItem(
            @PathVariable Long id, @PathVariable Long itemId) {
        return DataResponse.of(purchaseRequestService.removeItem(id, itemId));
    }

    @PostMapping("/{id}/submit")
    public DataResponse<PurchaseRequestResponse> submit(@PathVariable Long id) {
        return DataResponse.of(purchaseRequestService.submit(id));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('APPROVER')")
    public DataResponse<PurchaseRequestResponse> approve(
            @PathVariable Long id,
            @RequestBody(required = false) DecisionRequest request,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return DataResponse.of(purchaseRequestService.approve(id, request, principal.id()));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('APPROVER')")
    public DataResponse<PurchaseRequestResponse> reject(
            @PathVariable Long id,
            @RequestBody(required = false) DecisionRequest request,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return DataResponse.of(purchaseRequestService.reject(id, request, principal.id()));
    }
}
