/** ******************************************************************************
 * Copyright (c) 2022 Precies. Software Ltd and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx;

import com.google.common.util.concurrent.RateLimiter;
import org.eclipse.openvsx.json.*;
import org.eclipse.openvsx.search.ISearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;

public class RateLimitedRegistryService implements IExtensionRegistry {

    // TODO remove when done testing
    private static final Logger LOGGER = LoggerFactory.getLogger(RateLimitedRegistryService.class);

    private IExtensionRegistry registry;
    private RateLimiter rateLimiter;

    public RateLimitedRegistryService(IExtensionRegistry registry, double requestsPerSecond) {
        this.registry = registry;
        this.rateLimiter = RateLimiter.create(requestsPerSecond);
    }

    @Override
    public NamespaceJson getNamespace(String namespace) {
        rateLimiter.acquire();
        LOGGER.debug("getNamespace | {}", namespace);
        return registry.getNamespace(namespace);
    }

    @Override
    public ExtensionJson getExtension(String namespace, String extensionName, String targetPlatform) {
        rateLimiter.acquire();
        LOGGER.debug("getExtension | {}.{}@{}", namespace, extensionName, targetPlatform);
        return registry.getExtension(namespace, extensionName, targetPlatform);
    }

    @Override
    public ExtensionJson getExtension(String namespace, String extensionName, String targetPlatform, String version) {
        rateLimiter.acquire();
        LOGGER.debug("getExtension | {}.{}-{}@{}", namespace, extensionName, version, targetPlatform);
        return registry.getExtension(namespace, extensionName, targetPlatform, version);
    }

    @Override
    public ResponseEntity<byte[]> getFile(String namespace, String extensionName, String targetPlatform, String version, String fileName) {
        rateLimiter.acquire();
        LOGGER.debug("getFile | {}.{}-{}@{} {}", namespace, extensionName, version, targetPlatform, fileName);
        return registry.getFile(namespace, extensionName, targetPlatform, version, fileName);
    }

    @Override
    public ReviewListJson getReviews(String namespace, String extension) {
        rateLimiter.acquire();
        LOGGER.debug("getReviews | {}.{}", namespace, extension);
        return registry.getReviews(namespace, extension);
    }

    @Override
    public SearchResultJson search(ISearchService.Options options) {
        rateLimiter.acquire();
        LOGGER.debug("search | {}", options);
        return registry.search(options);
    }

    @Override
    public QueryResultJson query(QueryParamJson param) {
        rateLimiter.acquire();
        LOGGER.debug("query | {}", param);
        return registry.query(param);
    }
}