package com.collegeerp.erp.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.BatchStrategies;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {
    public static final String ADMIN_ANALYTICS = "adminAnalytics";
    public static final String ACTIVE_COLLEGES = "activeColleges";
    public static final String ACTIVE_DEPARTMENTS = "activeDepartments";
    public static final String ACTIVE_COURSE_YEARS = "activeCourseYears";
    public static final String ACTIVE_DIVISIONS = "activeDivisions";
    public static final String TEACHER_TIMETABLE = "teacherTimetable";
    public static final String STUDENT_ATTENDANCE = "studentAttendanceSummary";
    public static final String NOTICE_INBOX = "noticeInbox";
    public static final String FEE_SUMMARY = "feeSummary";
    public static final String[] ADMIN_CACHES = {ADMIN_ANALYTICS, ACTIVE_COLLEGES,
            ACTIVE_DEPARTMENTS, ACTIVE_COURSE_YEARS, ACTIVE_DIVISIONS, TEACHER_TIMETABLE,
            STUDENT_ATTENDANCE, NOTICE_INBOX, FEE_SUMMARY};
    private static final Logger log = LoggerFactory.getLogger(CacheConfig.class);

    @Bean
    @ConditionalOnProperty(name = "app.cache.redis-enabled", havingValue = "true")
    CacheManager redisCacheManager(RedisConnectionFactory factory,
            @Value("${spring.application.name:college-erp}") String application,
            @Value("${app.cache.environment:default}") String environment) {
        RedisSerializer<Object> serializer = redisValueSerializer();
        RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
                .computePrefixWith(name -> application + ":" + environment + ":" + name + ":")
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
                .disableCachingNullValues();
        Map<String, RedisCacheConfiguration> configurations = Map.of(
                ADMIN_ANALYTICS, defaults.entryTtl(Duration.ofSeconds(55)),
                ACTIVE_COLLEGES, defaults.entryTtl(Duration.ofMinutes(10)),
                ACTIVE_DEPARTMENTS, defaults.entryTtl(Duration.ofMinutes(10)),
                ACTIVE_COURSE_YEARS, defaults.entryTtl(Duration.ofMinutes(5)),
                ACTIVE_DIVISIONS, defaults.entryTtl(Duration.ofMinutes(5)),
                TEACHER_TIMETABLE, defaults.entryTtl(Duration.ofMinutes(3)),
                STUDENT_ATTENDANCE, defaults.entryTtl(Duration.ofSeconds(90)),
                NOTICE_INBOX, defaults.entryTtl(Duration.ofSeconds(30)),
                FEE_SUMMARY, defaults.entryTtl(Duration.ofSeconds(35)));
        RedisCacheWriter writer = RedisCacheWriter.nonLockingRedisCacheWriter(
                factory, BatchStrategies.scan(1000));
        return RedisCacheManager.builder(writer)
                .cacheDefaults(defaults.entryTtl(Duration.ofMinutes(5)))
                .withInitialCacheConfigurations(configurations)
                .transactionAware()
                .build();
    }

    static RedisSerializer<Object> redisValueSerializer() {
        return new GenericJackson2JsonRedisSerializer().configure(mapper -> {
            mapper.registerModule(new JavaTimeModule());
            mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        });
    }

    @Bean
    @ConditionalOnProperty(name = "app.cache.redis-enabled", havingValue = "false", matchIfMissing = true)
    CacheManager localCacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager(ADMIN_CACHES);
        manager.setCaffeine(Caffeine.newBuilder().maximumSize(10_000)
                .expireAfterWrite(5, TimeUnit.MINUTES).recordStats());
        manager.setAllowNullValues(false);
        return manager;
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override public void handleCacheGetError(RuntimeException e, Cache cache, Object key) { warn(e, cache, key); }
            @Override public void handleCachePutError(RuntimeException e, Cache cache, Object key, Object value) { warn(e, cache, key); }
            @Override public void handleCacheEvictError(RuntimeException e, Cache cache, Object key) { warn(e, cache, key); }
            @Override public void handleCacheClearError(RuntimeException e, Cache cache) { warn(e, cache, "*"); }
            private void warn(RuntimeException error, Cache cache, Object key) {
                log.warn("Cache {} failed for key {}; using database result", cache.getName(), key, error);
            }
        };
    }
}
