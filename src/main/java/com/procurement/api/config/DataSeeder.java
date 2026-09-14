package com.procurement.api.config;

import com.procurement.api.domain.*;
import com.procurement.api.repository.ProductRepository;
import com.procurement.api.repository.SupplierRepository;
import com.procurement.api.repository.UserRepository;
import com.procurement.api.repository.WarehouseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds master data + the minimum "1 USER, 1 APPROVER" required by the
 * brief. Runs on every application startup but is idempotent (checked by
 * unique business key), and only does anything when {@code app.seed.enabled}
 * is true - so it's a deliberate one-time action, not something that fires
 * on every normal run of the app.
 *
 * Enable it with: SEED_ENABLED=true, or --app.seed.enabled=true, then run
 * the app once and turn it back off (or just leave it - re-running is safe).
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final boolean enabled;
    private final UserRepository userRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProductRepository productRepository;
    private final SupplierRepository supplierRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(
            @Value("${app.seed.enabled}") boolean enabled,
            UserRepository userRepository,
            WarehouseRepository warehouseRepository,
            ProductRepository productRepository,
            SupplierRepository supplierRepository,
            PasswordEncoder passwordEncoder) {
        this.enabled = enabled;
        this.userRepository = userRepository;
        this.warehouseRepository = warehouseRepository;
        this.productRepository = productRepository;
        this.supplierRepository = supplierRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!enabled) {
            return;
        }

        log.info("Seeding database...");

        seedUser("john.user", "John (Warehouse Staff)", UserRole.USER);
        seedUser("sarah.approver", "Sarah (Purchasing Approver)", UserRole.APPROVER);

        seedWarehouse("WH-JKT", "Jakarta Warehouse", "Jakarta, Indonesia");
        seedWarehouse("WH-SBY", "Surabaya Warehouse", "Surabaya, Indonesia");

        seedProduct("SKU-OIL-001", "Industrial Oil", "PCS");
        seedProduct("SKU-GLV-001", "Safety Gloves", "BOX");
        seedProduct("SKU-HLM-001", "Safety Helmet", "PCS");

        seedSupplier("PT Sumber Makmur", "sales@sumbermakmur.co.id", "021-5550101");
        seedSupplier("PT Cahaya Industri", "sales@cahayaindustri.co.id", "021-5550102");

        log.info("Seeding complete.");
        log.info("Test credentials -> USER: john.user / password123, APPROVER: sarah.approver / password123");
    }

    private void seedUser(String username, String name, UserRole role) {
        if (userRepository.existsByUsername(username)) return;

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode("password123"));
        user.setName(name);
        user.setRole(role);
        userRepository.save(user);
    }

    private void seedWarehouse(String code, String name, String location) {
        if (warehouseRepository.existsByCode(code)) return;

        Warehouse warehouse = new Warehouse();
        warehouse.setCode(code);
        warehouse.setName(name);
        warehouse.setLocation(location);
        warehouseRepository.save(warehouse);
    }

    private void seedProduct(String sku, String name, String unit) {
        if (productRepository.existsBySku(sku)) return;

        Product product = new Product();
        product.setSku(sku);
        product.setName(name);
        product.setUnit(unit);
        productRepository.save(product);
    }

    private void seedSupplier(String name, String email, String phone) {
        boolean exists = supplierRepository.findAll().stream().anyMatch(s -> s.getName().equals(name));
        if (exists) return;

        Supplier supplier = new Supplier();
        supplier.setName(name);
        supplier.setEmail(email);
        supplier.setPhone(phone);
        supplierRepository.save(supplier);
    }
}
