package com.procurement.api.service;

import com.procurement.api.common.AppException;
import com.procurement.api.domain.*;
import com.procurement.api.dto.purchaseorder.PurchaseOrderCreateRequest;
import com.procurement.api.dto.purchaseorder.PurchaseOrderResponse;
import com.procurement.api.repository.PurchaseOrderRepository;
import com.procurement.api.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class PurchaseOrderService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final UserRepository userRepository;
    private final PurchaseRequestService purchaseRequestService;
    private final SupplierService supplierService;
    private final DocumentNumberService documentNumberService;

    public PurchaseOrderService(
            PurchaseOrderRepository purchaseOrderRepository,
            UserRepository userRepository,
            PurchaseRequestService purchaseRequestService,
            SupplierService supplierService,
            DocumentNumberService documentNumberService) {
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.userRepository = userRepository;
        this.purchaseRequestService = purchaseRequestService;
        this.supplierService = supplierService;
        this.documentNumberService = documentNumberService;
    }

    public PurchaseOrderResponse create(PurchaseOrderCreateRequest request, Long createdByUserId) {
        PurchaseRequest pr = purchaseRequestService.findDetailedOrThrow(request.purchaseRequestId());

        if (pr.getStatus() != PurchaseRequestStatus.APPROVED) {
            throw AppException.badRequest("PURCHASE_REQUEST_NOT_APPROVED",
                    "Purchase Request must be APPROVED before creating a Purchase Order");
        }

        if (purchaseOrderRepository.existsByPurchaseRequestId(pr.getId())) {
            throw AppException.conflict("PURCHASE_ORDER_ALREADY_EXISTS",
                    "This Purchase Request already has a Purchase Order");
        }

        Supplier supplier = supplierService.assertUsable(request.supplierId());
        User createdBy = userRepository.findById(createdByUserId)
                .orElseThrow(() -> AppException.notFound("User"));

        PurchaseOrder po = new PurchaseOrder();
        po.setPoNumber(documentNumberService.nextPurchaseOrderNumber());
        po.setPurchaseRequest(pr);
        po.setSupplier(supplier);
        po.setWarehouse(pr.getWarehouse());
        po.setStatus(PurchaseOrderStatus.DRAFT);
        po.setCreatedBy(createdBy);

        // Product and quantity are copied straight from the approved Purchase Request.
        for (PurchaseRequestItem prItem : pr.getItems()) {
            PurchaseOrderItem poItem = new PurchaseOrderItem();
            poItem.setProduct(prItem.getProduct());
            poItem.setOrderedQuantity(prItem.getQuantity());
            poItem.setReceivedQuantity(0);
            po.addItem(poItem);
        }

        purchaseOrderRepository.save(po);
        return PurchaseOrderResponse.from(po);
    }

    @Transactional(readOnly = true)
    public List<PurchaseOrderResponse> list(PurchaseOrderStatus status) {
        List<PurchaseOrder> rows = status == null
                ? purchaseOrderRepository.findAllByOrderByIdDesc()
                : purchaseOrderRepository.findByStatusOrderByIdDesc(status);
        return rows.stream().map(PurchaseOrderResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public PurchaseOrderResponse getById(Long id) {
        return PurchaseOrderResponse.from(findDetailedOrThrow(id));
    }

    public PurchaseOrderResponse markAsOrdered(Long id) {
        PurchaseOrder po = findDetailedOrThrow(id);

        if (po.getStatus() != PurchaseOrderStatus.DRAFT) {
            throw AppException.badRequest("PURCHASE_ORDER_NOT_DRAFT",
                    "Only a DRAFT Purchase Order can be marked as ORDERED");
        }

        po.setStatus(PurchaseOrderStatus.ORDERED);
        purchaseOrderRepository.save(po);
        return PurchaseOrderResponse.from(po);
    }

    /** Package-visible so GoodsReceiptService can load + mutate the PO entity directly. */
    PurchaseOrder findDetailedOrThrow(Long id) {
        return purchaseOrderRepository.findWithDetailsById(id)
                .orElseThrow(() -> AppException.notFound("Purchase Order"));
    }

}
