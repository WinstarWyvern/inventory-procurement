package com.procurement.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.procurement.api.domain.*;
import com.procurement.api.repository.*;
import com.procurement.api.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Base class for all business-rule integration tests.
 *
 * Runs against a real PostgreSQL database (configured in
 * application-test.yml, see the "test" profile) through MockMvc - no mocked
 * repositories - so these tests exercise the real JPA mappings, the real
 * database constraints, and the real transactional Goods Receipt logic.
 *
 * Each test method runs inside a transaction that is rolled back
 * automatically at the end (the default behaviour of Spring's test
 * framework combined with {@code @Transactional}), so tests never leak
 * data into one another and no manual cleanup/truncation is needed.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public abstract class AbstractIntegrationTest {

    @Autowired protected MockMvc mockMvc;
    @Autowired protected ObjectMapper objectMapper;

    @Autowired protected UserRepository userRepository;
    @Autowired protected WarehouseRepository warehouseRepository;
    @Autowired protected ProductRepository productRepository;
    @Autowired protected SupplierRepository supplierRepository;

    @Autowired protected JwtService jwtService;
    @Autowired protected PasswordEncoder passwordEncoder;

    protected User user;
    protected User approver;
    protected String userToken;
    protected String approverToken;

    protected Warehouse warehouse;
    protected Warehouse secondWarehouse;
    protected Product product;
    protected Product secondProduct;
    protected Product inactiveProduct;
    protected Supplier supplier;
    protected Supplier inactiveSupplier;

    @BeforeEach
    void seedFixtures() {
        user = persistUser("test.user", "Test User", UserRole.USER);
        approver = persistUser("test.approver", "Test Approver", UserRole.APPROVER);
        userToken = jwtService.generateToken(user);
        approverToken = jwtService.generateToken(approver);

        warehouse = persistWarehouse("WH-TEST", "Test Warehouse");
        secondWarehouse = persistWarehouse("WH-TEST-2", "Second Test Warehouse");

        product = persistProduct("SKU-TEST-001", "Test Product", "PCS", true);
        secondProduct = persistProduct("SKU-TEST-002", "Second Test Product", "BOX", true);
        inactiveProduct = persistProduct("SKU-TEST-003", "Inactive Product", "PCS", false);

        supplier = persistSupplier("Test Supplier", true);
        inactiveSupplier = persistSupplier("Inactive Supplier", false);
    }

    private User persistUser(String username, String name, UserRole role) {
        User u = new User();
        u.setUsername(username);
        u.setPasswordHash(passwordEncoder.encode("password123"));
        u.setName(name);
        u.setRole(role);
        return userRepository.save(u);
    }

    private Warehouse persistWarehouse(String code, String name) {
        Warehouse w = new Warehouse();
        w.setCode(code);
        w.setName(name);
        return warehouseRepository.save(w);
    }

    private Product persistProduct(String sku, String name, String unit, boolean active) {
        Product p = new Product();
        p.setSku(sku);
        p.setName(name);
        p.setUnit(unit);
        p.setActive(active);
        return productRepository.save(p);
    }

    private Supplier persistSupplier(String name, boolean active) {
        Supplier s = new Supplier();
        s.setName(name);
        s.setActive(active);
        return supplierRepository.save(s);
    }
}
