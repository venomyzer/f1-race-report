package com.f1report.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * CacheConfig – sets up named Caffeine caches with different TTL policies.
 *
 * WHY PER-CACHE CONFIG?
 * Not all data expires at the same rate:
 *   • Season list → changes once a year (long TTL, e.g. 24 hours)
 *   • Race list per season → changes during the season (medium TTL, 6 hours)
 *   • Race results → immutable once a race ends (very long TTL)
 *   • AI reports → generated on demand, we cache them in DB instead
 *
 * Real-world analogy: a restaurant's "prep cook" (cache) pre-makes dishes
 * that are ordered often. Salads (short TTL) are made fresh every hour.
 * Bread (long TTL) is baked once in the morning and lasts all day.
 *
 * These cache names map 1:1 to @Cacheable("seasons") annotations in services.
 */
@Configuration
public class CacheConfig {

    // Cache name constants – used as @Cacheable("seasons") etc.
    public static final String CACHE_SEASONS      = "seasons";
    public static final String CACHE_RACES        = "races";
    public static final String CACHE_RACE_RESULTS = "raceResults";
    public static final String CACHE_LAP_DATA     = "lapData";
    public static final String CACHE_STANDINGS    = "standings";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();

        // List all named caches – each will be configured with the same
        // default Caffeine spec from application.properties.
        // For different TTLs per cache, use registerCustomCache() below.
        manager.setCacheNames(List.of(
            CACHE_SEASONS,
            CACHE_RACES,
            CACHE_RACE_RESULTS,
            CACHE_LAP_DATA,
            CACHE_STANDINGS
        ));

        // Default spec: max 500 entries, expire 6 hours after last write
        manager.setCaffeine(defaultCacheSpec());

        // ── PER-CACHE OVERRIDES ─────────────────────────────────────────────
        // seasons: only changes once a year → cache for 24 hours
        manager.registerCustomCache(CACHE_SEASONS,
            Caffeine.newBuilder()
                .maximumSize(20)
                .expireAfterWrite(24, TimeUnit.HOURS)
                .recordStats()           // enables cache hit/miss metrics
                .build()
        );

        // raceResults: race results are immutable after the race → 12 hours
        manager.registerCustomCache(CACHE_RACE_RESULTS,
            Caffeine.newBuilder()
                .maximumSize(200)
                .expireAfterWrite(12, TimeUnit.HOURS)
                .recordStats()
                .build()
        );

        // lapData: largest payload, cache aggressively
        manager.registerCustomCache(CACHE_LAP_DATA,
            Caffeine.newBuilder()
                .maximumSize(100)
                .expireAfterWrite(12, TimeUnit.HOURS)
                .recordStats()
                .build()
        );

        return manager;
    }

    /**
     * Default Caffeine builder: 500 entries max, expires 6h after write.
     * Used for race lists and standings (moderate frequency of change).
     */
    private Caffeine<Object, Object> defaultCacheSpec() {
        return Caffeine.newBuilder()
            .maximumSize(500)
            .expireAfterWrite(6, TimeUnit.HOURS)
            .recordStats();
    }
}
