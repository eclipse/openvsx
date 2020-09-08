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

import javax.transaction.Transactional;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import org.eclipse.openvsx.entities.ExtensionVersion;
import org.eclipse.openvsx.entities.FileResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class GoogleCloudStorageService {

    private static final String BASE_URL = "https://storage.googleapis.com/";

    @Autowired
    StorageUtilService storageUtil;

    @Value("${ovsx.storage.gcp.project-id:}")
    String projectId;

    @Value("${ovsx.storage.gcp.bucket-id:}")
    String bucketId;

    private Storage storage;

    public boolean isEnabled() {
        return !Strings.isNullOrEmpty(projectId) && !Strings.isNullOrEmpty(bucketId);
    }

    protected Storage getStorage() {
        if (storage == null) {
            storage = StorageOptions.newBuilder()
                    .setProjectId(projectId)
                    .build().getService();
        }
        return storage;
    }

    @Transactional(Transactional.TxType.MANDATORY)
    public void uploadFile(FileResource resource) {
        var objectId = getObjectId(resource.getName(), resource.getExtension());
        uploadFile(resource.getContent(), objectId);
        resource.setUrl(BASE_URL + bucketId + "/" + objectId);
        resource.setStorageType(FileResource.STORAGE_GOOGLE);
    }

    protected void uploadFile(byte[] content, String objectId) {
        var blobInfoBuilder = BlobInfo.newBuilder(BlobId.of(bucketId, objectId))
                .setContentType(storageUtil.getFileType(objectId).toString());
        if (objectId.endsWith(".vsix")) {
            blobInfoBuilder.setContentDisposition("attachment; filename=\"" + objectId + "\"");
        }
        getStorage().create(blobInfoBuilder.build(), content);
    }

    public void removeFile(String name, ExtensionVersion extVersion) {
        var objectId = getObjectId(name, extVersion);
        getStorage().delete(BlobId.of(bucketId, objectId));
    }

    public URI getFileURI(String name, ExtensionVersion extVersion) {
        return URI.create(BASE_URL + bucketId + "/" + getObjectId(name, extVersion));
    }

    protected String getObjectId(String name, ExtensionVersion extVersion) {
        Preconditions.checkNotNull(name);
        var extension = extVersion.getExtension();
        var namespace = extension.getNamespace();
        return namespace.getName()
                + "/" + extension.getName()
                + "/" + extVersion.getVersion()
                + "/" + name;
    }

}