package com.f1report.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.concurrent.Executor;

/**
 * AppConfig – defines beans that are shared across the whole application.
 *
 * A @Bean method is like a factory method registered with Spring's IoC
 * (Inversion of Control) container. Instead of "new RestTemplate()" scattered
 * throughout your code (hard to configure, test, or swap), you declare it
 * once here and Spring injects it wherever you use @Autowired or constructor
 * injection.
 *
 * Real-world analogy: AppConfig is the "company storeroom" – tools are built
 * and stored here once, then handed out to any employee (service) who needs them.
 */
@Configuration
public class AppConfig {

    // Read timeout values from application.properties
    @Value("${ergast.api.timeout-ms:10000}")
    private int ergastTimeoutMs;

    @Value("${groq.api.timeout-ms:30000}")
    private int groqTimeoutMs;

    /**
     * ergastRestTemplate – configured HTTP client for the Ergast/Jolpica F1 API.
     *
     * RestTemplate is Spring's synchronous HTTP client.
     * We create a separate bean per external API so each has its own timeout
     * settings (Ergast is fast; Groq AI can be slow under load).
     *
     * Analogy: like two different phone lines – one for quick domestic calls,
     * one for international calls with longer connection time allowed.
     */
    @Bean(name = "ergastRestTemplate")
    public RestTemplate ergastRestTemplate(RestTemplateBuilder builder) {
        return builder
            // How long to wait for a TCP connection to be established
            .connectTimeout(Duration.ofMillis(5000))
            // How long to wait for the server to send a response
            .readTimeout(Duration.ofMillis(ergastTimeoutMs))
            .build();
    }

    /**
     * groqRestTemplate – configured HTTP client for the Groq AI API.
     * Longer read timeout because LLM inference can take 5–20 seconds.
     */
    @Bean(name = "groqRestTemplate")
    public RestTemplate groqRestTemplate(RestTemplateBuilder builder) {
        return builder
            .connectTimeout(Duration.ofMillis(5000))
            .readTimeout(Duration.ofMillis(groqTimeoutMs))
            .build();
    }

    /**
     * taskExecutor – thread pool for @Async methods (e.g., AI report generation).
     *
     * Without this, @Async would use a single shared thread pool which could
     * block other async operations. This dedicated pool is sized for I/O-bound
     * AI API calls.
     *
     * Analogy: like a call centre with 10 agents (core pool). During peak hours,
     * you can hire up to 20 agents (max pool). After the rush, extra agents leave.
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);        // Always-ready threads
        executor.setMaxPoolSize(20);        // Max threads under load
        executor.setQueueCapacity(100);     // Tasks queued if all threads busy
        executor.setThreadNamePrefix("f1-async-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
