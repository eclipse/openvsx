/** ******************************************************************************
 * Copyright (c) 2022 Precies. Software Ltd and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.json;

import org.eclipse.openvsx.cache.CacheService;
import org.eclipse.openvsx.entities.Extension;
import org.eclipse.openvsx.entities.ExtensionVersion;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.search.ExtensionSearch;
import org.eclipse.openvsx.search.SearchUtilService;
import org.eclipse.openvsx.storage.StorageUtilService;
import org.eclipse.openvsx.util.UrlUtil;
import org.eclipse.openvsx.util.VersionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.eclipse.openvsx.entities.FileResource.DOWNLOAD;
import static org.eclipse.openvsx.entities.FileResource.ICON;
import static org.eclipse.openvsx.util.UrlUtil.createApiUrl;

@Component
public class SearchEntryService {

    @Autowired
    EntityManager entityManager;

    @Autowired
    VersionService versions;

    @Autowired
    StorageUtilService storageUtil;

    @Autowired
    SearchUtilService search;

    @Autowired
    RepositoryService repositories;

    @Autowired
    CacheService cache;

    @Transactional
    public SearchEntryJson toJson(SearchHit<ExtensionSearch> searchHit, boolean includeAllVersions) {
        var searchEntry = cache.getSearchEntryJson(searchHit, includeAllVersions);
        if(searchEntry != null) {
            return searchEntry;
        }

        var extension = getExtension(searchHit);
        if(extension == null) {
            return null;
        }

        var serverUrl = UrlUtil.getBaseUrl();
        if(includeAllVersions && cache != null) {
            searchEntry = cache.getSearchEntryJson(searchHit, false);
        }
        if(searchEntry == null) {
            var latest = versions.getLatest(extension, null, false, true);
            searchEntry = latest.toSearchEntryJson();
            searchEntry.url = createApiUrl(serverUrl, "api", searchEntry.namespace, searchEntry.name);
            searchEntry.files = storageUtil.getFileUrls(latest, serverUrl, DOWNLOAD, ICON);
            cache.putSearchEntryJson(searchEntry, searchHit, false);
        }
        if (includeAllVersions) {
            var activeVersions = repositories.findActiveVersions(extension).toList();
            var versionUrls = storageUtil.getFileUrls(activeVersions, serverUrl, DOWNLOAD);
            searchEntry.allVersions = getAllVersionReferences(activeVersions, versionUrls, serverUrl);
            cache.putSearchEntryJson(searchEntry, searchHit, true);
        }

        return searchEntry;
    }

    private Extension getExtension(SearchHit<ExtensionSearch> searchHit) {
        var searchItem = searchHit.getContent();
        var extension = entityManager.find(Extension.class, searchItem.id);
        if (extension == null || !extension.isActive()) {
            extension = new Extension();
            extension.setId(searchItem.id);
            search.removeSearchEntry(extension);
            return null;
        }

        return extension;
    }

    private List<SearchEntryJson.VersionReference> getAllVersionReferences(
            List<ExtensionVersion> extVersions,
            Map<Long, Map<String, String>> versionUrls,
            String serverUrl
    ) {
        return extVersions.stream()
                .sorted(ExtensionVersion.SORT_COMPARATOR)
                .map(extVersion -> {
                    var ref = new SearchEntryJson.VersionReference();
                    ref.version = extVersion.getVersion();
                    ref.engines = extVersion.getEnginesMap();
                    ref.url = UrlUtil.createApiVersionUrl(serverUrl, extVersion);
                    ref.files = versionUrls.get(extVersion.getId());
                    return ref;
                }).collect(Collectors.toList());
    }
}
