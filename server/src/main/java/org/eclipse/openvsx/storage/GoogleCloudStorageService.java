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

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.common.base.Strings;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.openvsx.entities.FileResource;
import org.eclipse.openvsx.util.TargetPlatform;
import org.eclipse.openvsx.util.UrlUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;

@Component
public class GoogleCloudStorageService implements IStorageService {

    private static final String BASE_URL = "https://storage.googleapis.com/";

    @Value("${ovsx.storage.gcp.project-id:}")
    String projectId;

    @Value("${ovsx.storage.gcp.bucket-id:}")
    String bucketId;

    private Storage storage;

    @Override
    public boolean isEnabled() {
        return !Strings.isNullOrEmpty(bucketId);
    }

    protected Storage getStorage() {
        if (storage == null) {
            StorageOptions options;
            if (Strings.isNullOrEmpty(projectId)) {
                options = StorageOptions.getDefaultInstance();
            } else {
                options = StorageOptions.newBuilder()
                        .setProjectId(projectId)
                        .build();
            }
            storage = options.getService();
        }
        return storage;
    }

    @Override
    public void uploadFile(FileResource resource) {
        var objectId = getObjectId(resource);
        if (Strings.isNullOrEmpty(bucketId)) {
            throw new IllegalStateException("Cannot upload file "
                    + objectId + ": missing Google bucket id");
        }

        uploadFile(resource.getContent(), resource.getName(), objectId);
    }

    protected void uploadFile(byte[] content, String fileName, String objectId) {
        var blobInfoBuilder = BlobInfo.newBuilder(BlobId.of(bucketId, objectId))
                .setContentType(StorageUtil.getFileType(fileName).toString());
        if (fileName.endsWith(".vsix")) {
            blobInfoBuilder.setContentDisposition("attachment; filename=\"" + fileName + "\"");
        } else {
            var cacheControl = StorageUtil.getCacheControl(fileName);
            blobInfoBuilder.setCacheControl(cacheControl.getHeaderValue());
        }
        getStorage().create(blobInfoBuilder.build(), content);
    }

    @Override
    public void removeFile(FileResource resource) {
        var objectId = getObjectId(resource);
        if (Strings.isNullOrEmpty(bucketId)) {
            throw new IllegalStateException("Cannot remove file "
                    + objectId + ": missing Google bucket id");
        }
        getStorage().delete(BlobId.of(bucketId, objectId));
    }

    @Override
    public URI getLocation(FileResource resource) {
        if (Strings.isNullOrEmpty(bucketId)) {
            throw new IllegalStateException("Cannot determine location of file "
                    + resource.getName() + ": missing Google bucket id");
        }
        return URI.create(BASE_URL + bucketId + "/" + getObjectId(resource));
    }

    protected String getObjectId(FileResource resource) {
        var extVersion = resource.getExtension();
        var extension = extVersion.getExtension();
        var namespace = extension.getNamespace();
        var segments = new String[]{namespace.getName(), extension.getName()};
        if(!TargetPlatform.isUniversal(extVersion)) {
			segments = ArrayUtils.add(segments, extVersion.getTargetPlatform());
        }

		segments = ArrayUtils.add(segments, extVersion.getVersion());
        segments = ArrayUtils.addAll(segments, resource.getName().split("/"));
        return UrlUtil.createApiUrl("", segments).substring(1); // remove first '/'
    }

}