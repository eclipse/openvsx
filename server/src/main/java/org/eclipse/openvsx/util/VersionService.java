/** ******************************************************************************
 * Copyright (c) 2022 Precies. Software Ltd and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.util;

import org.eclipse.openvsx.entities.Extension;
import org.eclipse.openvsx.entities.ExtensionVersion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.util.List;

import static org.eclipse.openvsx.cache.CacheService.*;

@Component
public class VersionService {

    @Autowired
    EntityManager entityManager;

    /**
     * Get extension versions.
     * Utility method for read operations.
     * @param extension Extension to get versions for.
     * @return list of ExtensionVersion.
     */
    @Transactional
    public List<ExtensionVersion> getVersionsTrxn(Extension extension) {
        extension = entityManager.merge(extension);
        var versions = extension.getVersions();
        versions.forEach(entityManager::detach);
        return versions;
    }

    /**
     * Get latest extension version.
     * Utility method for read operations.
     * @param extension extension to get latest version for.
     * @param targetPlatform target platform to filter by.
     * @param onlyPreRelease whether to only include pre-release versions.
     * @param onlyActive whether to only include active versions.
     * @return the latest ExtensionVersion.
     */
    @Transactional
    @Cacheable(value = CACHE_LATEST_EXTENSION_VERSION, keyGenerator = GENERATOR_LATEST_EXTENSION_VERSION)
    public ExtensionVersion getLatestTrxn(Extension extension, String targetPlatform, boolean onlyPreRelease, boolean onlyActive) {
        extension = entityManager.merge(extension);
        return getLatest(extension, targetPlatform, onlyPreRelease, onlyActive);
    }

    /**
     * Get latest extension version.
     * @param extension extension to get latest version for.
     * @param targetPlatform target platform to filter by.
     * @param onlyPreRelease whether to only include pre-release versions.
     * @param onlyActive whether to only include active versions.
     * @return the latest ExtensionVersion.
     */
    @Cacheable(value = CACHE_LATEST_EXTENSION_VERSION, keyGenerator = GENERATOR_LATEST_EXTENSION_VERSION)
    public ExtensionVersion getLatest(Extension extension, String targetPlatform, boolean onlyPreRelease, boolean onlyActive) {
        return getLatest(extension.getVersions(), targetPlatform, onlyPreRelease, onlyActive);
    }

    // groupedByTargetPlatform is used by cache key generator, don't remove this parameter
    @Cacheable(value = CACHE_LATEST_EXTENSION_VERSION, keyGenerator = GENERATOR_LATEST_EXTENSION_VERSION)
    public ExtensionVersion getLatest(List<ExtensionVersion> versions, boolean groupedByTargetPlatform) {
        return getLatest(versions, null, false, false);
    }

    // groupedByTargetPlatform is used by cache key generator, don't remove this parameter
    @Cacheable(value = CACHE_LATEST_EXTENSION_VERSION, keyGenerator = GENERATOR_LATEST_EXTENSION_VERSION)
    public ExtensionVersion getLatest(List<ExtensionVersion> versions, boolean groupedByTargetPlatform, boolean onlyPreRelease) {
        return getLatest(versions, null, onlyPreRelease, false);
    }

    private ExtensionVersion getLatest(List<ExtensionVersion> versions, String targetPlatform, boolean onlyPreRelease, boolean onlyActive) {
        if(versions == null || versions.isEmpty()) {
            return null;
        }

        var stream = versions.stream();
        if(TargetPlatform.isValid(targetPlatform)) {
            stream = stream.filter(ev -> ev.getTargetPlatform().equals(targetPlatform));
        }
        if(onlyPreRelease) {
            stream = stream.filter(ExtensionVersion::isPreRelease);
        }
        if(onlyActive) {
            stream = stream.filter(ExtensionVersion::isActive);
        }

        return stream.min(ExtensionVersion.SORT_COMPARATOR).orElse(null);
    }
}
