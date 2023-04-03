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

import org.eclipse.openvsx.entities.Extension;
import org.eclipse.openvsx.entities.ExtensionVersion;
import org.eclipse.openvsx.entities.UserData;
import org.eclipse.openvsx.json.SearchEntryJson;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.search.ExtensionSearch;
import org.eclipse.openvsx.util.TargetPlatform;
import org.eclipse.openvsx.util.VersionAlias;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class CacheService {

    public static final String CACHE_DATABASE_SEARCH = "database.search";
    public static final String CACHE_EXTENSION_JSON = "extension.json";
    public static final String CACHE_SEARCH_ENTRY_JSON = "search.entry.json";
    public static final String CACHE_LATEST_EXTENSION_VERSION = "latest.extension.version";
    public static final String CACHE_NAMESPACE_DETAILS_JSON = "namespace.details.json";
    public static final String CACHE_AVERAGE_REVIEW_RATING = "average.review.rating";

    public static final String GENERATOR_EXTENSION_JSON = "extensionJsonCacheKeyGenerator";
    public static final String GENERATOR_LATEST_EXTENSION_VERSION = "latestExtensionVersionCacheKeyGenerator";

    @Autowired
    CacheManager cacheManager;

    @Autowired
    RepositoryService repositoryService;

    @Autowired
    ExtensionJsonCacheKeyGenerator extensionJsonCacheKey;

    @Autowired
    SearchEntryJsonCacheKeyGenerator searchEntryJsonCacheKeyGenerator;

    @Autowired
    LatestExtensionVersionCacheKeyGenerator latestExtensionVersionCacheKey;

    public void evictNamespaceDetails(Extension extension) {
        var cache = cacheManager.getCache(CACHE_NAMESPACE_DETAILS_JSON);
        if(cache == null) {
            return; // cache is not created
        }

        var namespaceName = extension.getNamespace().getName();
        cache.evictIfPresent(namespaceName);
    }

    public void evictExtensionJsons(String namespaceName, String extensionName) {
        evictExtensionJsons(repositoryService.findExtension(extensionName, namespaceName));
    }

    public void evictExtensionJsons(UserData user) {
        repositoryService.findVersions(user)
                .map(ExtensionVersion::getExtension)
                .toSet()
                .forEach(this::evictExtensionJsons);
    }

    public void evictExtensionJsons(Extension extension) {
        var cache = cacheManager.getCache(CACHE_EXTENSION_JSON);
        if(cache == null) {
            return; // cache is not created
        }
        if(extension.getVersions() == null) {
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
        for(var version : versions) {
            for(var targetPlatform : targetPlatforms) {
                cache.evictIfPresent(extensionJsonCacheKey.generate(namespaceName, extensionName, targetPlatform, version));
            }
        }
    }

    public SearchEntryJson getSearchEntryJson(SearchHit<ExtensionSearch> searchHit, boolean includeAllVersions) {
        var cache = cacheManager.getCache(CACHE_SEARCH_ENTRY_JSON);
        return cache != null
                ? cache.get(searchEntryJsonCacheKeyGenerator.generate(searchHit.getContent().id, includeAllVersions), SearchEntryJson.class)
                : null;
    }

    public void putSearchEntryJson(SearchEntryJson searchEntry, SearchHit<ExtensionSearch> searchHit, boolean includeAllVersions) {
        var cache = cacheManager.getCache(CACHE_SEARCH_ENTRY_JSON);
        if(cache != null) {
            cache.put(searchEntryJsonCacheKeyGenerator.generate(searchHit.getContent().id, includeAllVersions), searchEntry);
        }
    }

    public void evictSearchEntryJsons(Extension extension) {
        var cache = cacheManager.getCache(CACHE_SEARCH_ENTRY_JSON);
        if(cache == null) {
            return; // cache is not created
        }

        var includeAllVersionsList = List.of(true, false);
        for(var includeAllVersions : includeAllVersionsList) {
            var key = searchEntryJsonCacheKeyGenerator.generate(extension.getId(), includeAllVersions);
            cache.evictIfPresent(key);
        }
    }

    public void evictLatestExtensionVersion(Extension extension) {
        var cache = cacheManager.getCache(CACHE_LATEST_EXTENSION_VERSION);
        if(cache != null) {
            var targetPlatforms = new ArrayList<>(TargetPlatform.TARGET_PLATFORM_NAMES);
            targetPlatforms.add(null);
            for (var targetPlatform : targetPlatforms) {
                for (var preRelease : List.of(true, false)) {
                    for (var onlyActive : List.of(true, false)) {
                        var key = latestExtensionVersionCacheKey.generate(null, null, extension, targetPlatform, preRelease, onlyActive);
                        cache.evictIfPresent(key);
                    }
                }
            }
        }
    }
}
