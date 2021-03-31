package org.eclipse.openvsx;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

/**
 * CacheService
 */
@Component
public class CacheService {

    @Autowired
    CacheManager cacheManager;

    public void clearCaches() {
        cacheManager.getCacheNames().stream().forEach(cacheName -> cacheManager.getCache(cacheName).clear());
    }
}