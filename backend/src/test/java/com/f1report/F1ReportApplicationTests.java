package com.f1report;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * F1ReportApplicationTests – smoke test that verifies the Spring context loads.
 *
 * @SpringBootTest: boots the entire application context (all beans, DB, etc.)
 * If any bean fails to initialise (misconfigured, missing dependency, etc.),
 * this test will fail – giving you early detection of configuration problems.
 *
 * @TestPropertySource: overrides application.properties values for testing.
 * We use H2 in-memory DB and dummy API keys so the test doesn't need
 * a real PostgreSQL instance or Groq API key to run.
 *
 * Real-world analogy: this is like a fire drill. It checks that the
 * building's systems (Spring context) can all start up correctly before
 * any real "fire" (production traffic) happens.
 */
@SpringBootTest
@TestPropertySource(properties = {
    // Use H2 in-memory DB so tests don't need PostgreSQL running
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop",

    // Dummy API keys for test context
    "groq.api.key=test-key-not-real",
    "groq.api.model=test-model",

    // Suppress unnecessary logs during test
    "spring.jpa.show-sql=false"
})
class F1ReportApplicationTests {

    /**
     * Verifies the Spring application context loads without errors.
     * If any @Bean, @Service, @Repository, or @Controller fails to wire up,
     * this test will throw an exception and fail.
     */
    @Test
    void contextLoads() {
        // No assertions needed: if the context fails to load, the test fails automatically.
        // This is the most important test to have in any Spring Boot project.
    }
}
