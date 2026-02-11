/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx.storage;

import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.openvsx.entities.FileResource;
import org.eclipse.openvsx.entities.Namespace;
import org.eclipse.openvsx.util.TempFile;
import org.eclipse.openvsx.util.UrlUtil;
import org.springframework.data.util.Pair;

import jakarta.annotation.Nullable;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;

public interface IStorageService {

    /**
     * Indicates whether this storage service is enabled by application config.
     */
    boolean isEnabled();

    /**
     * Upload a file to the external storage.
     */
    void uploadFile(TempFile tempFile);

    /**
     * Remove a file from the external storage.
     */
    void removeFile(FileResource resource);

    /**
     * Returns the public access location of a resource.
     */
    URI getLocation(FileResource resource);

    /**
     * Upload a namespace logo to the external storage.
     */
    void uploadNamespaceLogo(TempFile logoFile);

    /**
     * Remove a namespace logo from the external storage.
     */
    void removeNamespaceLogo(Namespace namespace);

    /**
     * Returns the public access location of a namespace logo.
     */
    URI getNamespaceLogoLocation(Namespace namespace);

    TempFile downloadFile(FileResource resource) throws IOException;

    void copyFiles(List<Pair<FileResource, FileResource>> pairs);

    void copyNamespaceLogo(Namespace oldNamespace, Namespace newNamespace);

    @Nullable Path getCachedFile(FileResource resource);

    default String getObjectKey(FileResource resource) {
        var extVersion = resource.getExtension();
        var extension = extVersion.getExtension();
        var namespace = extension.getNamespace();
        var segments = new String[]{namespace.getName(), extension.getName()};
        if(!extVersion.isUniversalTargetPlatform()) {
            segments = ArrayUtils.add(segments, extVersion.getTargetPlatform());
        }

        segments = ArrayUtils.add(segments, extVersion.getVersion());
        segments = ArrayUtils.addAll(segments, resource.getName().split("/"));
        var url = UrlUtil.createApiUrl("", segments);
        return url != null ? url.substring(1) : null; // remove first '/'
    }

    default String getObjectKey(Namespace namespace) {
        var url = UrlUtil.createApiUrl("", namespace.getName(), "logo", namespace.getLogoName());
        return url != null ? url.substring(1) : null; // remove first '/'
    }
}