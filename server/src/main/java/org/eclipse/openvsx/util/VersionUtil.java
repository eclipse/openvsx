/** ******************************************************************************
 * Copyright (c) 2022 Precies. Software and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.util;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.eclipse.openvsx.dto.ExtensionVersionDTO;
import org.eclipse.openvsx.entities.ExtensionVersion;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.StreamSupport;

public class VersionUtil {

    private static final class LatestVersionContext {
        Iterable<ExtensionVersion> versions;
        List<Predicate<ExtensionVersion>> predicates;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            LatestVersionContext that = (LatestVersionContext) o;
            return Objects.equals(versions, that.versions) && Objects.equals(predicates, that.predicates);
        }

        @Override
        public int hashCode() {
            return Objects.hash(versions, predicates);
        }
    }

    private static final LoadingCache<Iterable<ExtensionVersionDTO>, Optional<ExtensionVersionDTO>> CACHE_DTO = CacheBuilder.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .build(new CacheLoader<>() {
                @Override
                public Optional<ExtensionVersionDTO> load(Iterable<ExtensionVersionDTO> versions) throws Exception {
                    return StreamSupport.stream(versions.spliterator(), false)
                            .max(Comparator.comparing(ExtensionVersionDTO::getSemanticVersion)
                                    .thenComparing(TargetPlatform::isUniversal)
                                    .thenComparing(ExtensionVersionDTO::getTargetPlatform));
                }
            });

    private static final LoadingCache<LatestVersionContext, Optional<ExtensionVersion>> CACHE = CacheBuilder.newBuilder()
                .expireAfterAccess(10, TimeUnit.MINUTES)
                .build(new CacheLoader<>() {
                    @Override
                    public Optional<ExtensionVersion> load(LatestVersionContext context) throws Exception {
                        var stream = StreamSupport.stream(context.versions.spliterator(), false);
                        for(var predicate : context.predicates) {
                            stream = stream.filter(predicate);
                        }

                        return stream.max(Comparator.comparing(ExtensionVersion::getSemanticVersion)
                                .thenComparing(TargetPlatform::isUniversal)
                                .thenComparing(ExtensionVersion::getTargetPlatform));
                    }
                });

    public static ExtensionVersionDTO getLatest(Iterable<ExtensionVersionDTO> versions) {
        if(versions == null || !versions.iterator().hasNext()) {
            return null;
        }

        return CACHE_DTO.getUnchecked(versions).orElse(null);
    }

    public static ExtensionVersion getLatest(Iterable<ExtensionVersion> versions, List<Predicate<ExtensionVersion>> predicates) {
        if(versions == null || !versions.iterator().hasNext()) {
            return null;
        }

        var context = new LatestVersionContext();
        context.versions = versions;
        context.predicates = predicates;
        return CACHE.getUnchecked(context).orElse(null);
    }

    public static void clearCache() {
        CACHE.invalidateAll();
        CACHE_DTO.invalidateAll();
    }
}
