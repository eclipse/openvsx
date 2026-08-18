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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import org.eclipse.openvsx.adapter.VSCodeIdService;
import org.eclipse.openvsx.entities.ExtensionVersion;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.util.NamingUtil;

/**
 * Scanner that blocks publishing to a namespace/extension identifier that already exists on the
 * upstream VS Code Marketplace, unless the publishing user is a local owner (not just a
 * contributor) of the namespace. Guards against namespace-squatting relative to the upstream
 * gallery identity.
 */
@Component
public class VSCodeGalleryOwnershipScanner implements Scanner {

    public static final String TYPE = "vscode-gallery-ownership";

    private final VSCodeIdService vsCodeIdService;
    private final RepositoryService repositories;
    private final EntityManager entityManager;
    private final ScannerRegistry scannerRegistry;

    @Value("${ovsx.scanning.gallery-ownership.enabled:false}")
    private boolean enabled;
    @Value("${ovsx.scanning.gallery-ownership.required:false}")
    private boolean required;
    @Value("${ovsx.scanning.gallery-ownership.enforced:true}")
    private boolean enforced;

    public VSCodeGalleryOwnershipScanner(
            VSCodeIdService vsCodeIdService,
            RepositoryService repositories,
            EntityManager entityManager,
            ScannerRegistry scannerRegistry
    ) {
        this.vsCodeIdService = vsCodeIdService;
        this.repositories = repositories;
        this.entityManager = entityManager;
        this.scannerRegistry = scannerRegistry;
    }

    @PostConstruct
    void register() {
        if (enabled) {
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
        return required;
    }

    @Override
    public boolean enforcesThreats() {
        return enforced;
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

        var upstream = vsCodeIdService.getUpstreamPublicIds(extension);
        boolean existsUpstream = upstream != null && upstream.namespace() != null && upstream.extension() != null;
        if (!existsUpstream) {
            return new Scanner.Invocation.Completed(Scanner.Result.clean());
        }

        var publishedWith = extVersion.getPublishedWith();
        var user = publishedWith != null ? publishedWith.getUser() : null;
        if (user != null && repositories.isNamespaceOwner(user, namespace)) {
            return new Scanner.Invocation.Completed(
                    Scanner.Result.clean(
                            "Extension exists on the VS Code Marketplace; publisher confirmed as namespace owner."));
        }

        var threat = new Scanner.Threat(
                "vscode-gallery-namespace-conflict",
                "'" + NamingUtil.toExtensionId(extension) + "' already exists on the VS Code Marketplace, " +
                        "and the publishing user is not an owner of namespace '" + namespace.getName() + "'.",
                "high");
        return new Scanner.Invocation.Completed(Scanner.Result.withThreats(List.of(threat)));
    }
}
