package com.jadhavr.erp.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class AdminCacheInvalidationListener {
    private static final Logger log = LoggerFactory.getLogger(AdminCacheInvalidationListener.class);
    private final CacheManager caches;

    public AdminCacheInvalidationListener(CacheManager caches) { this.caches = caches; }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void invalidate(AdminDataChangedEvent event) {
        for (String name : CacheConfig.ADMIN_CACHES) {
            var cache = caches.getCache(name);
            if (cache != null) cache.clear();
        }
        log.debug("Invalidated Admin read caches after {}", event.source());
    }
}
