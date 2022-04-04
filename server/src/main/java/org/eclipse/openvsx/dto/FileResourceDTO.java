/** ******************************************************************************
 * Copyright (c) 2021 Precies. Software and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.dto;

import org.eclipse.openvsx.entities.FileResource;

import java.util.Objects;

public class FileResourceDTO {

    private final long extensionVersionId;
    private ExtensionVersionDTO extensionVersion;

    private final long id;
    private final String name;
    private final String type;
    private String storageType;
    private byte[] content;

    public FileResourceDTO(long id, long extensionVersionId, String name, String type, String storageType, byte[] content) {
        this(id, extensionVersionId, name, type);
        this.storageType = storageType;
        this.content = content;
    }

    public FileResourceDTO(long id, long extensionVersionId, String name, String type) {
        this.id = id;
        this.extensionVersionId = extensionVersionId;
        this.name = name;
        this.type = type;
    }

    public long getExtensionVersionId() {
        return extensionVersionId;
    }

    public ExtensionVersionDTO getExtensionVersion() {
        return extensionVersion;
    }

    public void setExtensionVersion(ExtensionVersionDTO extensionVersion) {
        if(extensionVersion.getId() == extensionVersionId) {
            this.extensionVersion = extensionVersion;
        } else {
            throw new IllegalArgumentException("extensionVersion must have the same id as extensionVersionId");
        }
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getStorageType() {
        return storageType;
    }

    public byte[] getContent() {
        return content;
    }

    public boolean isWebResource() {
        return type.equals(FileResource.RESOURCE) && name.startsWith("extension/");
    }
}
