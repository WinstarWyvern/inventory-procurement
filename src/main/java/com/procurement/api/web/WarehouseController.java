package com.procurement.api.web;

import com.procurement.api.common.DataResponse;
import com.procurement.api.dto.warehouse.WarehouseCreateRequest;
import com.procurement.api.dto.warehouse.WarehouseResponse;
import com.procurement.api.dto.warehouse.WarehouseUpdateRequest;
import com.procurement.api.service.WarehouseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/warehouses")
public class WarehouseController {

    private final WarehouseService warehouseService;

    public WarehouseController(WarehouseService warehouseService) {
        this.warehouseService = warehouseService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DataResponse<WarehouseResponse> create(@Valid @RequestBody WarehouseCreateRequest request) {
        return DataResponse.of(warehouseService.create(request));
    }

    @GetMapping
    public DataResponse<List<WarehouseResponse>> list() {
        return DataResponse.of(warehouseService.list());
    }

    @GetMapping("/{id}")
    public DataResponse<WarehouseResponse> getById(@PathVariable Long id) {
        return DataResponse.of(warehouseService.getById(id));
    }

    @PatchMapping("/{id}")
    public DataResponse<WarehouseResponse> update(
            @PathVariable Long id, @Valid @RequestBody WarehouseUpdateRequest request) {
        return DataResponse.of(warehouseService.update(id, request));
    }
}
