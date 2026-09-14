package com.procurement.api.web;

import com.procurement.api.common.DataResponse;
import com.procurement.api.dto.supplier.SupplierCreateRequest;
import com.procurement.api.dto.supplier.SupplierResponse;
import com.procurement.api.dto.supplier.SupplierUpdateRequest;
import com.procurement.api.service.SupplierService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/suppliers")
public class SupplierController {

    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DataResponse<SupplierResponse> create(@Valid @RequestBody SupplierCreateRequest request) {
        return DataResponse.of(supplierService.create(request));
    }

    @GetMapping
    public DataResponse<List<SupplierResponse>> list() {
        return DataResponse.of(supplierService.list());
    }

    @GetMapping("/{id}")
    public DataResponse<SupplierResponse> getById(@PathVariable Long id) {
        return DataResponse.of(supplierService.getById(id));
    }

    @PatchMapping("/{id}")
    public DataResponse<SupplierResponse> update(
            @PathVariable Long id, @Valid @RequestBody SupplierUpdateRequest request) {
        return DataResponse.of(supplierService.update(id, request));
    }
}
