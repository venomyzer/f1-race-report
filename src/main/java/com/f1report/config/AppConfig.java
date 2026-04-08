package com.f1report.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.Executor;

/**
 * AppConfig – defines beans that are shared across the whole application.
 *
 * NOTE: Spring Boot 3.2 removed connectTimeout(Duration) / readTimeout(Duration)
 * from RestTemplateBuilder. We now configure timeouts directly on
 * SimpleClientHttpRequestFactory, which is the underlying HTTP transport.
 *
 * Real-world analogy: AppConfig is the "company storeroom" – tools are built
 * and stored here once, then handed out to any employee (service) who needs them.
 */
@Configuration
public class AppConfig {

    @Value("${ergast.api.timeout-ms:10000}")
    private int ergastTimeoutMs;

    @Value("${groq.api.timeout-ms:30000}")
    private int groqTimeoutMs;

    /**
     * ergastRestTemplate – HTTP client for the Jolpica/Ergast F1 API.
     *
     * SimpleClientHttpRequestFactory wraps Java's built-in HttpURLConnection.
     * connectTimeout = max ms to establish the TCP connection.
     * readTimeout    = max ms to wait for the server to begin responding.
     *
     * Analogy: like two different phone lines – one for quick domestic calls,
     * one for international calls with a longer wait allowed.
     */
    @Bean(name = "ergastRestTemplate")
    public RestTemplate ergastRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);           // 5s to connect
        factory.setReadTimeout(ergastTimeoutMs);   // 10s to read
        return new RestTemplate(factory);
    }

    /**
     * groqRestTemplate – HTTP client for the Groq AI API.
     * Longer read timeout because LLM inference can take 5–20 seconds.
     */
    @Bean(name = "groqRestTemplate")
    public RestTemplate groqRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);          // 5s to connect
        factory.setReadTimeout(groqTimeoutMs);    // 30s to read
        return new RestTemplate(factory);
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
