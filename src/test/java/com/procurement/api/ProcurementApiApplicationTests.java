package com.procurement.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * The simplest possible test: does the whole Spring application context
 * start up without errors (all beans wire together, JPA entities map
 * cleanly onto the Flyway-migrated schema, security config is valid)?
 * Run this first if something looks wrong - it fails fast with a clear
 * stack trace before you even get to the business-rule tests.
 */
@SpringBootTest
@ActiveProfiles("test")
class ProcurementApiApplicationTests {

    @Test
    void contextLoads() {
        // Intentionally empty - @SpringBootTest already does the work:
        // this test fails if the application context cannot start.
    }
}
