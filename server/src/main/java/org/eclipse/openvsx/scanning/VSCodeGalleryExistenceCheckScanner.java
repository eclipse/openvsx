/********************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx.scanning;

import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import org.eclipse.openvsx.adapter.ExtensionQueryParam;
import org.eclipse.openvsx.adapter.ExtensionQueryResult;
import org.eclipse.openvsx.adapter.IVSCodeService;
import org.eclipse.openvsx.entities.Extension;
import org.eclipse.openvsx.entities.ExtensionVersion;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.util.NamingUtil;
import org.eclipse.openvsx.util.UrlUtil;

/**
 * Scanner that blocks publishing to a namespace/extension identifier that already exists on the
 * upstream VS Code Marketplace, unless the publishing NS is verified (has owner, not only
 * contributors) namespace. Guards against namespace-squatting relative to the upstream
 * gallery identity.
 */
@Component
public class VSCodeGalleryExistenceCheckScanner implements Scanner {

    public static final String TYPE = "vscode-gallery-ownership";

    private final VSCodeGalleryExistenceCheckConfig config;
    private final RestTemplate restTemplate;
    private final RepositoryService repositories;
    private final EntityManager entityManager;
    private final ScannerRegistry scannerRegistry;

    public VSCodeGalleryExistenceCheckScanner(
            VSCodeGalleryExistenceCheckConfig config,
            RestTemplate restTemplate,
            RepositoryService repositories,
            EntityManager entityManager,
            ScannerRegistry scannerRegistry
    ) {
        this.config = config;
        this.restTemplate = restTemplate;
        this.repositories = repositories;
        this.entityManager = entityManager;
        this.scannerRegistry = scannerRegistry;
    }

    @PostConstruct
    void register() {
        if (config.isEnabled()) {
            scannerRegistry.registerScanner(this);
        }
    }

    @Override
    @NonNull
    public String getScannerType() {
        return TYPE;
    }

    @Override
    public boolean isRequired() {
        return config.isRequired();
    }

    @Override
    public boolean enforcesThreats() {
        return config.isEnforced();
    }

    @Override
    public boolean isAsync() {
        return false;
    }

    @Override
    public Scanner.@NonNull Invocation startScan(@NonNull Command command) throws ScannerException {
        var extVersion = entityManager.find(ExtensionVersion.class, command.extensionVersionId());
        if (extVersion == null) {
            throw new ScannerException("ExtensionVersion not found: " + command.extensionVersionId());
        }

        var extension = extVersion.getExtension();
        var namespace = extension.getNamespace();

        try {
            boolean upstreamExists = upstreamExists(extension);
            if (!upstreamExists) {
                return new Scanner.Invocation.Completed(Scanner.Result.clean());
            }
        } catch (RestClientException ex) {
            if (config.isEnforced()) {
                throw new ScannerException("Failed to perform " + TYPE, ex);
            } else {
                return new Scanner.Invocation.Completed(
                        Scanner.Result.clean(
                                "Failed to perform " + TYPE + " scan: " + ex.getMessage()));
            }
        }

        var publishedWith = extVersion.getPublishedWith();
        var user = publishedWith != null ? publishedWith.getUser() : null;
        if (user != null && repositories.isVerified(namespace, user)) {
            return new Scanner.Invocation.Completed(
                    Scanner.Result.clean(
                            "Extension exists on the VS Code Marketplace; namespace confirmed as verified."));
        }

        var threat = new Scanner.Threat(
                "vscode-gallery-namespace-conflict",
                "'" + NamingUtil.toExtensionId(extension) + "' already exists on the VS Code Marketplace, " +
                        "and the target namespace is not verified '" + namespace.getName() + "'.",
                "high");
        return new Scanner.Invocation.Completed(Scanner.Result.withThreats(List.of(threat)));
    }

    /**
     * Method reaching upstream; if returns {@code true} or {@code false} only if check was performed and result was
     * clear about it. In any other case method will throw.
     */
    private boolean upstreamExists(Extension extension) throws RestClientException {
        var requestUrl = UrlUtil.createApiUrl(config.getGalleryUrl(), "extensionquery");
        var requestData = new ExtensionQueryParam(
                List.of(
                        new ExtensionQueryParam.Filter(
                                List.of(
                                        new ExtensionQueryParam.Criterion(
                                                ExtensionQueryParam.Criterion.FILTER_TARGET,
                                                "Microsoft.VisualStudio.Code"),
                                        new ExtensionQueryParam.Criterion(
                                                ExtensionQueryParam.Criterion.FILTER_EXTENSION_NAME,
                                                NamingUtil.toExtensionId(extension))),
                                1,
                                1,
                                0,
                                0)),
                0);
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.ACCEPT, "application/json;api-version=" + IVSCodeService.GALLERY_API_VERSION);
        var result = restTemplate
                .postForObject(requestUrl, new HttpEntity<>(requestData, headers), ExtensionQueryResult.class);
        if (result != null && result.results() != null && !result.results().isEmpty()) {
            var item = result.results().getFirst();
            return item.extensions() != null && !item.extensions().isEmpty();
        }
        return false;
    }
}
