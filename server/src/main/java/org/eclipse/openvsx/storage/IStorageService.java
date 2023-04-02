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

import java.net.URI;
import java.nio.file.Path;

import org.eclipse.openvsx.entities.FileResource;
import org.eclipse.openvsx.entities.Namespace;

public interface IStorageService {

    /**
     * Indicates whether this storage service is enabled by application config.
     */
    boolean isEnabled();

    /**
     * Upload a file to the external storage.
     */
    void uploadFile(FileResource resource);

    /**
     * Upload a file to the external storage.
     */
    void uploadFile(FileResource resource, Path filePath);

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
    void uploadNamespaceLogo(Namespace namespace);

    /**
     * Remove a namespace logo from the external storage.
     */
    void removeNamespaceLogo(Namespace namespace);

    /**
     * Returns the public access location of a namespace logo.
     */
    URI getNamespaceLogoLocation(Namespace namespace);

    Path downloadNamespaceLogo(Namespace namespace);
}