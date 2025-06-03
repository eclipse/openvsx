/** ******************************************************************************
 * Copyright (c) 2022 Precies. Software and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.cache;

import io.micrometer.observation.annotation.Observed;
import org.eclipse.openvsx.entities.*;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.util.TargetPlatform;
import org.eclipse.openvsx.util.VersionAlias;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class CacheService {

    public static final String CACHE_DATABASE_SEARCH = "database.search";
    public static final String CACHE_WEB_RESOURCE_FILES = "files.webresource";
    public static final String CACHE_BROWSE_EXTENSION_FILES = "files.browse";
    public static final String CACHE_EXTENSION_FILES = "files.extension";
    public static final String CACHE_EXTENSION_JSON = "extension.json";
    public static final String CACHE_LATEST_EXTENSION_VERSION = "latest.extension.version";
    public static final String CACHE_NAMESPACE_DETAILS_JSON = "namespace.details.json";
    public static final String CACHE_AVERAGE_REVIEW_RATING = "average.review.rating";
    public static final String CACHE_SITEMAP = "sitemap";
    public static final String CACHE_MALICIOUS_EXTENSIONS = "malicious.extensions";

    public static final String GENERATOR_EXTENSION_JSON = "extensionJsonCacheKeyGenerator";
    public static final String GENERATOR_LATEST_EXTENSION_VERSION = "latestExtensionVersionCacheKeyGenerator";
    public static final String GENERATOR_FILES = "filesCacheKeyGenerator";

    private final CacheManager cacheManager;
    private final RepositoryService repositories;
    private final ExtensionJsonCacheKeyGenerator extensionJsonCacheKey;
    private final LatestExtensionVersionCacheKeyGenerator latestExtensionVersionCacheKey;
    private final FilesCacheKeyGenerator filesCacheKeyGenerator;

    public CacheService(
            CacheManager cacheManager,
            RepositoryService repositories,
            ExtensionJsonCacheKeyGenerator extensionJsonCacheKey,
            LatestExtensionVersionCacheKeyGenerator latestExtensionVersionCacheKey,
            FilesCacheKeyGenerator filesCacheKeyGenerator
    ) {
        this.cacheManager = cacheManager;
        this.repositories = repositories;
        this.extensionJsonCacheKey = extensionJsonCacheKey;
        this.latestExtensionVersionCacheKey = latestExtensionVersionCacheKey;
        this.filesCacheKeyGenerator = filesCacheKeyGenerator;
    }

    public void evictSitemap() {
        invalidateCache(CACHE_SITEMAP);
    }

    public void evictNamespaceDetails() {
        invalidateCache(CACHE_NAMESPACE_DETAILS_JSON);
    }

    public void evictNamespaceDetails(Namespace namespace) {
        evictNamespaceDetails(namespace.getName());
    }

    public void evictNamespaceDetails(Extension extension) {
        evictNamespaceDetails(extension.getNamespace().getName());
    }

    private void evictNamespaceDetails(String namespaceName) {
        var cache = cacheManager.getCache(CACHE_NAMESPACE_DETAILS_JSON);
        if(cache == null) {
            return; // cache is not created
        }

        cache.evictIfPresent(namespaceName);
    }

    public void evictExtensionJsons() {
        invalidateCache(CACHE_EXTENSION_JSON);
    }

    public void evictExtensionJsons(UserData user) {
        repositories.findExtensions(user).forEach(this::evictExtensionJsons);
    }

    public void evictExtensionJsons(Extension extension) {
        var cache = cacheManager.getCache(CACHE_EXTENSION_JSON);
        if (cache == null) {
            return; // cache is not created
        }
        if (extension.getVersions() == null) {
            return;
        }

        var versions = new ArrayList<>(VersionAlias.ALIAS_NAMES);
        extension.getVersions().stream()
                .map(ExtensionVersion::getVersion)
                .forEach(versions::add);

        var namespaceName = extension.getNamespace().getName();
        var extensionName = extension.getName();
        var targetPlatforms = new ArrayList<>(TargetPlatform.TARGET_PLATFORM_NAMES);
        targetPlatforms.add("null");
        for (var version : versions) {
            for (var targetPlatform : targetPlatforms) {
                cache.evictIfPresent(extensionJsonCacheKey.generate(namespaceName, extensionName, targetPlatform, version));
            }
        }
    }

    public void evictExtensionJsons(ExtensionVersion extVersion) {
        var cache = cacheManager.getCache(CACHE_EXTENSION_JSON);
        if (cache == null) {
            return; // cache is not created
        }

        var extension = extVersion.getExtension();
        var namespace = extension.getNamespace();
        var versions = new ArrayList<>(List.of(VersionAlias.LATEST, extVersion.getVersion()));
        if (extVersion.isPreRelease()) {
            versions.add(VersionAlias.PRE_RELEASE);
        }
        if (extVersion.isPreview()) {
            versions.add(VersionAlias.PREVIEW);
        }
        for (var version : versions) {
            cache.evictIfPresent(extensionJsonCacheKey.generate(namespace.getName(), extension.getName(), extVersion.getTargetPlatform(), version));
        }
    }

    public void evictLatestExtensionVersions() {
        invalidateCache(CACHE_LATEST_EXTENSION_VERSION);
    }

    public void evictLatestExtensionVersion(Extension extension) {
        var cache = cacheManager.getCache(CACHE_LATEST_EXTENSION_VERSION);
        if(cache == null) {
            return;
        }

        var targetPlatforms = new ArrayList<>(TargetPlatform.TARGET_PLATFORM_NAMES);
        targetPlatforms.add(null);
        for (var targetPlatform : targetPlatforms) {
            for (var preRelease : List.of(true, false)) {
                for (var onlyActive : List.of(true, false)) {
                    for(var type : ExtensionVersion.Type.values()) {
                        var key = latestExtensionVersionCacheKey.generate(extension, targetPlatform, preRelease, onlyActive, type);
                        cache.evictIfPresent(key);
                    }
                }
            }
        }
    }

    private void invalidateCache(String cacheName) {
        var cache = cacheManager.getCache(cacheName);
        if(cache == null) {
            return;
        }

        cache.invalidate();
    }

    public void evictExtensionFile(FileResource download) {
        var cache = cacheManager.getCache(CACHE_EXTENSION_FILES);
        if(cache == null) {
            return;
        }

        cache.evict(filesCacheKeyGenerator.generate(download));
    }

    @Observed
    public void evictWebResourceFile(String namespaceName, String extensionName, String targetPlatform, String version, String path) {
        var cache = cacheManager.getCache(CACHE_WEB_RESOURCE_FILES);
        if(cache == null) {
            return;
        }

        cache.evict(filesCacheKeyGenerator.generate(namespaceName, extensionName, targetPlatform, version, path));
    }
}
