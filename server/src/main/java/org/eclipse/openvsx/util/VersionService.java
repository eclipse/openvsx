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

import org.eclipse.openvsx.dto.ExtensionVersionDTO;
import org.eclipse.openvsx.entities.Extension;
import org.eclipse.openvsx.entities.ExtensionVersion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.util.Comparator;
import java.util.List;

import static org.eclipse.openvsx.cache.CacheService.*;
import static org.eclipse.openvsx.cache.CacheService.GENERATOR_LATEST_EXTENSION_VERSION_DTO;

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
        var versions = extension.getVersions();
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

        return stream.max(Comparator.comparing(ExtensionVersion::getSemanticVersion)
                .thenComparing(TargetPlatform::isUniversal)
                .thenComparing(ExtensionVersion::getTargetPlatform))
                .orElse(null);
    }

    /**
     * Get latest extension version for DTO objects.
     * @param versions list to find latest version in.
     * @param groupedByTargetPlatform whether the list only contains one specific target platform.
     * This parameter is used to generate a unique cache key, do not remove this parameter.
     * @return the latest ExtensionVersionDTO.
     */
    @Cacheable(value = CACHE_LATEST_EXTENSION_VERSION_DTO, keyGenerator = GENERATOR_LATEST_EXTENSION_VERSION_DTO)
    public ExtensionVersionDTO getLatest(List<ExtensionVersionDTO> versions, boolean groupedByTargetPlatform) {
        if(versions == null || versions.isEmpty()) {
            return null;
        }

        return versions.stream().max(Comparator.comparing(ExtensionVersionDTO::getSemanticVersion)
                .thenComparing(TargetPlatform::isUniversal)
                .thenComparing(ExtensionVersionDTO::getTargetPlatform))
                .orElse(null);
    }
}
