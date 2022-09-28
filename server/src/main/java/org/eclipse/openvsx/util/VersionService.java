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
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

import static org.eclipse.openvsx.cache.CacheService.*;
import static org.eclipse.openvsx.cache.CacheService.GENERATOR_LATEST_EXTENSION_VERSION_DTO;

@Component
public class VersionService {

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

    // groupedByTargetPlatform is used by cache key generator, don't remove this parameter
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
