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

import org.eclipse.openvsx.entities.ExtensionVersion;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.eclipse.openvsx.cache.CacheService.CACHE_LATEST_EXTENSION_VERSION;
import static org.eclipse.openvsx.cache.CacheService.GENERATOR_LATEST_EXTENSION_VERSION;

@Component
public class VersionService {

    // groupedByTargetPlatform is used by cache key generator, don't remove this parameter
    @Cacheable(value = CACHE_LATEST_EXTENSION_VERSION, keyGenerator = GENERATOR_LATEST_EXTENSION_VERSION)
    public ExtensionVersion getLatest(List<ExtensionVersion> versions, boolean groupedByTargetPlatform) {
        return getLatest(versions, groupedByTargetPlatform, false);
    }

    // groupedByTargetPlatform is used by cache key generator, don't remove this parameter
    @Cacheable(value = CACHE_LATEST_EXTENSION_VERSION, keyGenerator = GENERATOR_LATEST_EXTENSION_VERSION)
    public ExtensionVersion getLatest(List<ExtensionVersion> versions, boolean groupedByTargetPlatform, boolean onlyPreRelease) {
        if(versions == null || versions.isEmpty()) {
            return null;
        }

        var stream = versions.stream();
        if(onlyPreRelease) {
            stream = stream.filter(ExtensionVersion::isPreRelease);
        }

        return stream.min(ExtensionVersion.SORT_COMPARATOR).orElse(null);
    }
}
