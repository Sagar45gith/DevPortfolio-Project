package org.devanalyzer.devport.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Configures in-memory Caffeine caching with two named caches:
 * <ul>
 *   <li><b>githubData</b> — caches aggregated GitHub API responses,
 *       keyed by username, 15-minute TTL, max 500 entries</li>
 *   <li><b>resumeData</b> — caches parsed resume data,
 *       keyed by SHA-256 hash of the PDF bytes, 30-minute TTL, max 500 entries</li>
 * </ul>
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCache githubDataCache = new CaffeineCache("githubData",
                Caffeine.newBuilder()
                        .expireAfterWrite(15, TimeUnit.MINUTES)
                        .maximumSize(500)
                        .recordStats()
                        .build());

        CaffeineCache resumeDataCache = new CaffeineCache("resumeData",
                Caffeine.newBuilder()
                        .expireAfterWrite(30, TimeUnit.MINUTES)
                        .maximumSize(500)
                        .recordStats()
                        .build());

        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(List.of(githubDataCache, resumeDataCache));
        return cacheManager;
    }
}
