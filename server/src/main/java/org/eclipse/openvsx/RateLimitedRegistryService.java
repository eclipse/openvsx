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
import org.springframework.http.ResponseEntity;

public class RateLimitedRegistryService implements IExtensionRegistry {

    private IExtensionRegistry registry;
    private RateLimiter rateLimiter;

    public RateLimitedRegistryService(IExtensionRegistry registry, double requestsPerSecond) {
        this.registry = registry;
        this.rateLimiter = RateLimiter.create(requestsPerSecond);
    }

    @Override
    public NamespaceJson getNamespace(String namespace) {
        rateLimiter.acquire();
        return registry.getNamespace(namespace);
    }

    @Override
    public ExtensionJson getExtension(String namespace, String extensionName, String targetPlatform) {
        rateLimiter.acquire();
        return registry.getExtension(namespace, extensionName, targetPlatform);
    }

    @Override
    public ExtensionJson getExtension(String namespace, String extensionName, String targetPlatform, String version) {
        rateLimiter.acquire();
        return registry.getExtension(namespace, extensionName, targetPlatform, version);
    }

    @Override
    public ResponseEntity<byte[]> getFile(String namespace, String extensionName, String targetPlatform, String version, String fileName) {
        rateLimiter.acquire();
        return registry.getFile(namespace, extensionName, targetPlatform, version, fileName);
    }

    @Override
    public ReviewListJson getReviews(String namespace, String extension) {
        rateLimiter.acquire();
        return registry.getReviews(namespace, extension);
    }

    @Override
    public SearchResultJson search(ISearchService.Options options) {
        rateLimiter.acquire();
        return registry.search(options);
    }

    @Override
    public QueryResultJson query(QueryParamJson param) {
        rateLimiter.acquire();
        return registry.query(param);
    }
}