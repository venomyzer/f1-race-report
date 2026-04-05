package com.f1report;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * F1ReportApplication – the entry point for the entire Spring Boot app.
 *
 * @SpringBootApplication is a shortcut for three annotations:
 *   1. @Configuration       → this class can define Spring beans
 *   2. @EnableAutoConfiguration → Spring Boot auto-wires everything it detects
 *      (e.g., sees PostgreSQL driver on classpath → auto-configures DataSource)
 *   3. @ComponentScan       → scans this package and sub-packages for
 *      @Service, @Controller, @Repository, @Component beans
 *
 * Real-world analogy: SpringApplication.run() is like turning on the ignition
 * of a car. Everything boots up — engine (Tomcat), fuel system (DB pool),
 * electronics (caching, async) — all from one key turn.
 *
 * @EnableCaching  → activates the Caffeine in-memory cache defined in CacheConfig
 * @EnableAsync    → allows @Async methods to run in a separate thread pool
 *                   (used for non-blocking AI report generation)
 */
@SpringBootApplication
@EnableCaching
@EnableAsync
public class F1ReportApplication {

    public static void main(String[] args) {
        SpringApplication.run(F1ReportApplication.class, args);
    }
}
