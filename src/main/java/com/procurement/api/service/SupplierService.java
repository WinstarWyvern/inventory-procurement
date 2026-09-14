package com.procurement.api.service;

import com.procurement.api.common.AppException;
import com.procurement.api.domain.Supplier;
import com.procurement.api.dto.supplier.SupplierCreateRequest;
import com.procurement.api.dto.supplier.SupplierResponse;
import com.procurement.api.dto.supplier.SupplierUpdateRequest;
import com.procurement.api.repository.SupplierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class SupplierService {

    private final SupplierRepository supplierRepository;

    public SupplierService(SupplierRepository supplierRepository) {
        this.supplierRepository = supplierRepository;
    }

    public SupplierResponse create(SupplierCreateRequest request) {
        Supplier supplier = new Supplier();
        supplier.setName(request.name());
        supplier.setEmail(request.email());
        supplier.setPhone(request.phone());
        supplier.setActive(request.active() == null || request.active());

        return SupplierResponse.from(supplierRepository.save(supplier));
    }

    @Transactional(readOnly = true)
    public List<SupplierResponse> list() {
        return supplierRepository.findAll().stream()
                .sorted((a, b) -> Long.compare(a.getId(), b.getId()))
                .map(SupplierResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public SupplierResponse getById(Long id) {
        return SupplierResponse.from(findEntity(id));
    }

    public SupplierResponse update(Long id, SupplierUpdateRequest request) {
        Supplier supplier = findEntity(id);

        if (request.name() != null) supplier.setName(request.name());
        if (request.email() != null) supplier.setEmail(request.email());
        if (request.phone() != null) supplier.setPhone(request.phone());
        if (request.active() != null) supplier.setActive(request.active());

        return SupplierResponse.from(supplierRepository.save(supplier));
    }

    Supplier findEntity(Long id) {
        return supplierRepository.findById(id).orElseThrow(() -> AppException.notFound("Supplier"));
    }

    public Supplier assertUsable(Long id) {
        Supplier supplier = findEntity(id);
        if (!supplier.isActive()) {
            throw AppException.badRequest("SUPPLIER_INACTIVE",
                    "Supplier '" + supplier.getName() + "' is not active and cannot be used for new Purchase Orders");
        }
        return supplier;
    }
}
