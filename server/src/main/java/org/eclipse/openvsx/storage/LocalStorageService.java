/** ******************************************************************************
 * Copyright (c) 2024 Precies. Software OU and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.storage;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.eclipse.openvsx.entities.FileResource;
import org.eclipse.openvsx.entities.Namespace;
import org.eclipse.openvsx.util.TempFile;
import org.eclipse.openvsx.util.UrlUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;

@Component
public class LocalStorageService {

    private final EntityManager entityManager;

    public LocalStorageService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public URI getLocation(FileResource resource) {
        return URI.create(UrlUtil.createApiFileUrl(UrlUtil.getBaseUrl(), resource.getExtension(), resource.getName()));
    }

    public URI getNamespaceLogoLocation(Namespace namespace) {
        return URI.create(UrlUtil.createApiUrl(UrlUtil.getBaseUrl(), "api", namespace.getName(), "logo", namespace.getLogoName()));
    }

    public TempFile downloadNamespaceLogo(Namespace namespace) throws IOException {
        var logoFile = createNamespaceLogoFile();
        Files.write(logoFile.getPath(), namespace.getLogoBytes());
        return logoFile;
    }

    public TempFile createNamespaceLogoFile() throws IOException {
        return new TempFile("namespace-logo", ".png");
    }

    @Transactional
    public ResponseEntity<byte[]> getFileResponse(FileResource resource) {
        resource = entityManager.find(FileResource.class, resource.getId());
        var headers = getFileResponseHeaders(resource.getName());
        return new ResponseEntity<>(resource.getContent(), headers, HttpStatus.OK);
    }

    @Transactional
    public ResponseEntity<byte[]> getNamespaceLogo(Namespace namespace) {
        namespace = entityManager.merge(namespace);
        var headers = getFileResponseHeaders(namespace.getLogoName());
        return new ResponseEntity<>(namespace.getLogoBytes(), headers, HttpStatus.OK);
    }

    public HttpHeaders getFileResponseHeaders(String fileName) {
        var headers = new HttpHeaders();
        headers.setContentType(StorageUtil.getFileType(fileName));
        if (fileName.endsWith(".vsix")) {
            headers.add("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        } else {
            headers.setCacheControl(StorageUtil.getCacheControl(fileName));
        }
        return headers;
    }
}
