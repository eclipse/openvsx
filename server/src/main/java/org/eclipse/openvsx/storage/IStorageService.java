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

import org.eclipse.openvsx.entities.FileResource;

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
     * Remove a file from the external storage.
     */
    void removeFile(FileResource resource);

    /**
     * Returns the public access location of a resource.
     */
    URI getLocation(FileResource resource);
    
}