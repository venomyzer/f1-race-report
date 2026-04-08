package com.f1report.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CorsConfig – Cross-Origin Resource Sharing configuration.
 *
 * WHY THIS IS NEEDED:
 * Browsers enforce the "Same-Origin Policy" – a security rule that blocks
 * JavaScript on http://localhost:5173 (React) from calling an API on
 * http://localhost:8080 (Spring Boot) because the ports differ.
 *
 * Real-world analogy: CORS is like a bouncer at a club. Without this config,
 * the bouncer blocks everyone from a different postcode. With it, we hand the
 * bouncer an explicit guest list (allowed origins).
 *
 * This configuration tells Spring MVC to add CORS headers to every response
 * so the browser stops blocking our frontend-to-backend calls.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry
            // Apply CORS policy to ALL endpoints under /api/**
            .addMapping("/api/**")

            // Allowed origins: React dev server + production domain
            // In production, replace * with your actual domain
            .allowedOrigins(
                "http://localhost:5173",   // Vite default dev port
                "http://localhost:3000",   // CRA / alternate port
                "http://localhost:4173"    // Vite preview port
            )

            // Allow standard HTTP methods
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")

            // Allow all headers (including Authorization, Content-Type)
            .allowedHeaders("*")

            // Allow cookies / credentials to be sent cross-origin
            .allowCredentials(true)

            // Cache the preflight OPTIONS response for 1 hour (3600 seconds)
            // Reduces the number of preflight requests from the browser
            .maxAge(3600);
    }
}
