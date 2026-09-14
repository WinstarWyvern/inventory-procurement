package com.procurement.api.service;

import com.procurement.api.common.AppException;
import com.procurement.api.domain.Warehouse;
import com.procurement.api.dto.warehouse.WarehouseCreateRequest;
import com.procurement.api.dto.warehouse.WarehouseResponse;
import com.procurement.api.dto.warehouse.WarehouseUpdateRequest;
import com.procurement.api.repository.WarehouseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;

    public WarehouseService(WarehouseRepository warehouseRepository) {
        this.warehouseRepository = warehouseRepository;
    }

    public WarehouseResponse create(WarehouseCreateRequest request) {
        if (warehouseRepository.existsByCode(request.code())) {
            throw AppException.conflict("WAREHOUSE_CODE_ALREADY_EXISTS",
                    "Code '" + request.code() + "' is already in use");
        }

        Warehouse warehouse = new Warehouse();
        warehouse.setCode(request.code());
        warehouse.setName(request.name());
        warehouse.setLocation(request.location());
        warehouse.setActive(request.active() == null || request.active());

        return WarehouseResponse.from(warehouseRepository.save(warehouse));
    }

    @Transactional(readOnly = true)
    public List<WarehouseResponse> list() {
        return warehouseRepository.findAll().stream()
                .sorted((a, b) -> Long.compare(a.getId(), b.getId()))
                .map(WarehouseResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public WarehouseResponse getById(Long id) {
        return WarehouseResponse.from(findEntity(id));
    }

    public WarehouseResponse update(Long id, WarehouseUpdateRequest request) {
        Warehouse warehouse = findEntity(id);

        if (request.code() != null && !request.code().equals(warehouse.getCode())) {
            if (warehouseRepository.existsByCodeAndIdNot(request.code(), id)) {
                throw AppException.conflict("WAREHOUSE_CODE_ALREADY_EXISTS",
                        "Code '" + request.code() + "' is already in use");
            }
            warehouse.setCode(request.code());
        }
        if (request.name() != null) warehouse.setName(request.name());
        if (request.location() != null) warehouse.setLocation(request.location());
        if (request.active() != null) warehouse.setActive(request.active());

        return WarehouseResponse.from(warehouseRepository.save(warehouse));
    }

    Warehouse findEntity(Long id) {
        return warehouseRepository.findById(id).orElseThrow(() -> AppException.notFound("Warehouse"));
    }

    public Warehouse assertUsable(Long id) {
        Warehouse warehouse = findEntity(id);
        if (!warehouse.isActive()) {
            throw AppException.badRequest("WAREHOUSE_INACTIVE",
                    "Warehouse '" + warehouse.getCode() + "' is not active and cannot be used for new transactions");
        }
        return warehouse;
    }
}
