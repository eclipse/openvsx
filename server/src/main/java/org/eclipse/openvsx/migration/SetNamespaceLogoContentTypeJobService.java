/** ******************************************************************************
 * Copyright (c) 2023 Precies. Software Ltd and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.migration;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.eclipse.openvsx.entities.ExtensionVersion;
import org.eclipse.openvsx.entities.FileResource;
import org.eclipse.openvsx.entities.Namespace;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.storage.AzureBlobStorageService;
import org.eclipse.openvsx.storage.GoogleCloudStorageService;
import org.eclipse.openvsx.storage.IStorageService;
import org.eclipse.openvsx.util.TempFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

@Component
public class SetNamespaceLogoContentTypeJobService {

    @Autowired
    AzureBlobStorageService azureStorage;

    @Autowired
    GoogleCloudStorageService googleStorage;

    @Autowired
    RestTemplate backgroundRestTemplate;

    @Autowired
    EntityManager entityManager;

    public Namespace getNamespace(long entityId) {
        return entityManager.find(Namespace.class, entityId);
    }

    @Transactional
    public byte[] getLogoContent(Namespace namespace) {
        namespace = entityManager.merge(namespace);
        return namespace.getLogoStorageType().equals(FileResource.STORAGE_DB) ? namespace.getLogoBytes() : null;
    }

    @Retryable
    public TempFile getNamespaceLogoFile(Map.Entry<Namespace, byte[]> entry) throws IOException {
        var namespaceLogoFile = new TempFile("migration-namespace-logo_", "");

        var content = entry.getValue();
        if(content == null) {
            var namespace = entry.getKey();
            var storage = getStorage(namespace);
            var uri = storage.getNamespaceLogoLocation(namespace);
            backgroundRestTemplate.execute("{namespaceLogoLocation}", HttpMethod.GET, null, response -> {
                try(var out = Files.newOutputStream(namespaceLogoFile.getPath())) {
                    response.getBody().transferTo(out);
                }

                return namespaceLogoFile;
            }, Map.of("namespaceLogoLocation", uri.toString()));
        } else {
            Files.write(namespaceLogoFile.getPath(), content);
        }

        return namespaceLogoFile;
    }

    public IStorageService getStorage(Namespace namespace) {
        var storages = Map.of(
                FileResource.STORAGE_AZURE, azureStorage,
                FileResource.STORAGE_GOOGLE, googleStorage
        );

        return storages.get(namespace.getLogoStorageType());
    }

    @Transactional
    public void updateNamespace(Namespace namespace) {
        entityManager.merge(namespace);
    }

    @Retryable
    public void uploadNamespaceLogo(Namespace namespace, TempFile logoFile) {
        if(namespace.getLogoStorageType().equals(FileResource.STORAGE_DB)) {
            return;
        }

        var storage = getStorage(namespace);
        storage.uploadNamespaceLogo(namespace, logoFile);
        namespace.setLogoBytes(null);
    }
}
