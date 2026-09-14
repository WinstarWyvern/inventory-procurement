package com.procurement.api.service;

import com.procurement.api.common.AppException;
import com.procurement.api.domain.*;
import com.procurement.api.dto.purchaserequest.*;
import com.procurement.api.repository.PurchaseRequestItemRepository;
import com.procurement.api.repository.PurchaseRequestRepository;
import com.procurement.api.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class PurchaseRequestService {

    private final PurchaseRequestRepository purchaseRequestRepository;
    private final PurchaseRequestItemRepository purchaseRequestItemRepository;
    private final UserRepository userRepository;
    private final WarehouseService warehouseService;
    private final ProductService productService;
    private final DocumentNumberService documentNumberService;

    public PurchaseRequestService(
            PurchaseRequestRepository purchaseRequestRepository,
            PurchaseRequestItemRepository purchaseRequestItemRepository,
            UserRepository userRepository,
            WarehouseService warehouseService,
            ProductService productService,
            DocumentNumberService documentNumberService) {
        this.purchaseRequestRepository = purchaseRequestRepository;
        this.purchaseRequestItemRepository = purchaseRequestItemRepository;
        this.userRepository = userRepository;
        this.warehouseService = warehouseService;
        this.productService = productService;
        this.documentNumberService = documentNumberService;
    }

    public PurchaseRequestResponse create(PurchaseRequestCreateRequest request, Long requesterId) {
        List<PurchaseRequestItemInput> items = request.itemsOrEmpty();
        assertNoDuplicateProducts(items);

        Warehouse warehouse = warehouseService.assertUsable(request.warehouseId());
        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> AppException.notFound("User"));

        PurchaseRequest pr = new PurchaseRequest();
        pr.setRequestNumber(documentNumberService.nextPurchaseRequestNumber());
        pr.setWarehouse(warehouse);
        pr.setRequester(requester);
        pr.setStatus(PurchaseRequestStatus.DRAFT);

        for (PurchaseRequestItemInput itemInput : items) {
            Product product = productService.assertUsable(itemInput.productId());
            PurchaseRequestItem item = new PurchaseRequestItem();
            item.setProduct(product);
            item.setQuantity(itemInput.quantity());
            pr.addItem(item);
        }

        purchaseRequestRepository.save(pr);
        return PurchaseRequestResponse.from(pr);
    }

    @Transactional(readOnly = true)
    public List<PurchaseRequestResponse> list(PurchaseRequestStatus status) {
        List<PurchaseRequest> rows = status == null
                ? purchaseRequestRepository.findAllByOrderByIdDesc()
                : purchaseRequestRepository.findByStatusOrderByIdDesc(status);
        return rows.stream().map(PurchaseRequestResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public PurchaseRequestResponse getById(Long id) {
        return PurchaseRequestResponse.from(findDetailedOrThrow(id));
    }

    public PurchaseRequestResponse updateWarehouse(Long id, PurchaseRequestWarehouseUpdateRequest request) {
        PurchaseRequest pr = findDetailedOrThrow(id);
        assertDraft(pr);

        Warehouse warehouse = warehouseService.assertUsable(request.warehouseId());
        pr.setWarehouse(warehouse);

        purchaseRequestRepository.save(pr);
        return PurchaseRequestResponse.from(pr);
    }

    public PurchaseRequestResponse addItem(Long id, PurchaseRequestItemInput input) {
        PurchaseRequest pr = findDetailedOrThrow(id);
        assertDraft(pr);

        purchaseRequestItemRepository.findByPurchaseRequestIdAndProductId(id, input.productId())
                .ifPresent(existing -> {
                    throw AppException.conflict("DUPLICATE_PRODUCT_IN_REQUEST",
                            "The same product cannot appear more than once in a Purchase Request");
                });

        Product product = productService.assertUsable(input.productId());
        PurchaseRequestItem item = new PurchaseRequestItem();
        item.setProduct(product);
        item.setQuantity(input.quantity());
        pr.addItem(item);

        purchaseRequestRepository.save(pr);
        return PurchaseRequestResponse.from(pr);
    }

    public PurchaseRequestResponse updateItemQuantity(Long id, Long itemId, ItemQuantityUpdateRequest request) {
        PurchaseRequest pr = findDetailedOrThrow(id);
        assertDraft(pr);

        PurchaseRequestItem item = pr.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> AppException.notFound("Purchase Request item"));

        item.setQuantity(request.quantity());
        purchaseRequestRepository.save(pr);
        return PurchaseRequestResponse.from(pr);
    }

    public PurchaseRequestResponse removeItem(Long id, Long itemId) {
        PurchaseRequest pr = findDetailedOrThrow(id);
        assertDraft(pr);

        PurchaseRequestItem item = pr.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> AppException.notFound("Purchase Request item"));

        pr.removeItem(item);
        purchaseRequestRepository.save(pr);
        return PurchaseRequestResponse.from(pr);
    }

    public PurchaseRequestResponse submit(Long id) {
        PurchaseRequest pr = findDetailedOrThrow(id);
        assertDraft(pr);

        if (pr.getItems().isEmpty()) {
            throw AppException.badRequest("PURCHASE_REQUEST_EMPTY",
                    "Purchase Request cannot be submitted without at least one item");
        }

        pr.setStatus(PurchaseRequestStatus.SUBMITTED);
        purchaseRequestRepository.save(pr);
        return PurchaseRequestResponse.from(pr);
    }

    public PurchaseRequestResponse approve(Long id, DecisionRequest request, Long approverId) {
        PurchaseRequest pr = findDetailedOrThrow(id);
        assertSubmitted(pr);

        User approver = userRepository.findById(approverId)
                .orElseThrow(() -> AppException.notFound("User"));

        pr.setStatus(PurchaseRequestStatus.APPROVED);
        pr.setApprover(approver);
        pr.setDecisionAt(OffsetDateTime.now());
        pr.setDecisionRemarks(request == null ? null : request.remarks());

        purchaseRequestRepository.save(pr);
        return PurchaseRequestResponse.from(pr);
    }

    public PurchaseRequestResponse reject(Long id, DecisionRequest request, Long approverId) {
        PurchaseRequest pr = findDetailedOrThrow(id);
        assertSubmitted(pr);

        User approver = userRepository.findById(approverId)
                .orElseThrow(() -> AppException.notFound("User"));

        pr.setStatus(PurchaseRequestStatus.REJECTED);
        pr.setApprover(approver);
        pr.setDecisionAt(OffsetDateTime.now());
        pr.setDecisionRemarks(request == null ? null : request.remarks());

        purchaseRequestRepository.save(pr);
        return PurchaseRequestResponse.from(pr);
    }

    /** Package-visible so PurchaseOrderService can load the approved PR entity directly. */
    PurchaseRequest findDetailedOrThrow(Long id) {
        return purchaseRequestRepository.findWithDetailsById(id)
                .orElseThrow(() -> AppException.notFound("Purchase Request"));
    }

    private void assertDraft(PurchaseRequest pr) {
        if (pr.getStatus() != PurchaseRequestStatus.DRAFT) {
            throw AppException.badRequest("PURCHASE_REQUEST_NOT_DRAFT",
                    "Purchase Request can only be edited while it is in DRAFT status");
        }
    }

    private void assertSubmitted(PurchaseRequest pr) {
        if (pr.getStatus() != PurchaseRequestStatus.SUBMITTED) {
            throw AppException.badRequest("PURCHASE_REQUEST_NOT_SUBMITTED",
                    "Only a SUBMITTED Purchase Request can be approved or rejected");
        }
    }

    private void assertNoDuplicateProducts(List<PurchaseRequestItemInput> items) {
        Set<Long> seen = new HashSet<>();
        for (PurchaseRequestItemInput item : items) {
            if (!seen.add(item.productId())) {
                throw AppException.badRequest("DUPLICATE_PRODUCT_IN_REQUEST",
                        "The same product cannot appear more than once in a Purchase Request");
            }
        }
    }
}
