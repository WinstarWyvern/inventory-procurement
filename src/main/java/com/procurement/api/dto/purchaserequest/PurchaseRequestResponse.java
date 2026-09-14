package com.procurement.api.dto.purchaserequest;

import com.procurement.api.domain.PurchaseRequest;
import com.procurement.api.domain.PurchaseRequestStatus;
import com.procurement.api.dto.common.UserSummaryResponse;
import com.procurement.api.dto.warehouse.WarehouseResponse;

import java.time.OffsetDateTime;
import java.util.List;

public record PurchaseRequestResponse(
        Long id,
        String requestNumber,
        WarehouseResponse warehouse,
        UserSummaryResponse requester,
        PurchaseRequestStatus status,
        UserSummaryResponse approver,
        OffsetDateTime decisionAt,
        String decisionRemarks,
        List<PurchaseRequestItemResponse> items,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static PurchaseRequestResponse from(PurchaseRequest pr) {
        return new PurchaseRequestResponse(
                pr.getId(),
                pr.getRequestNumber(),
                WarehouseResponse.from(pr.getWarehouse()),
                UserSummaryResponse.from(pr.getRequester()),
                pr.getStatus(),
                UserSummaryResponse.from(pr.getApprover()),
                pr.getDecisionAt(),
                pr.getDecisionRemarks(),
                pr.getItems().stream().map(PurchaseRequestItemResponse::from).toList(),
                pr.getCreatedAt(),
                pr.getUpdatedAt());
    }
}
