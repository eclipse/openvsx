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
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.openvsx.entities.FileResource;
import org.eclipse.openvsx.entities.Namespace;
import org.eclipse.openvsx.util.TargetPlatform;
import org.eclipse.openvsx.util.TempFile;
import org.eclipse.openvsx.util.UrlUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import org.apache.commons.lang3.StringUtils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.List;

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
        return !StringUtils.isEmpty(bucketId);
    }

    protected Storage getStorage() {
        if (storage == null) {
            StorageOptions options;
            if (StringUtils.isEmpty(projectId)) {
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
        if (StringUtils.isEmpty(bucketId)) {
            throw new IllegalStateException("Cannot upload file "
                    + objectId + ": missing Google bucket id");
        }

        uploadFile(resource.getContent(), resource.getName(), objectId);
    }

    @Override
    public void uploadNamespaceLogo(Namespace namespace) {
        var objectId = getObjectId(namespace);
        if (StringUtils.isEmpty(bucketId)) {
            throw new IllegalStateException("Cannot upload file "
                    + objectId + ": missing Google bucket id");
        }

        uploadFile(namespace.getLogoBytes(), namespace.getLogoName(), objectId);
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
    public void uploadFile(FileResource resource, TempFile file) {
        var objectId = getObjectId(resource);
        if (StringUtils.isEmpty(bucketId)) {
            throw new IllegalStateException("Cannot upload file "
                    + objectId + ": missing Google bucket id");
        }

        uploadFile(file, resource.getName(), objectId);
    }

    protected void uploadFile(TempFile file, String fileName, String objectId) {
        var blobInfoBuilder = BlobInfo.newBuilder(BlobId.of(bucketId, objectId))
                .setContentType(StorageUtil.getFileType(fileName).toString());
        if (fileName.endsWith(".vsix")) {
            blobInfoBuilder.setContentDisposition("attachment; filename=\"" + fileName + "\"");
        } else {
            var cacheControl = StorageUtil.getCacheControl(fileName);
            blobInfoBuilder.setCacheControl(cacheControl.getHeaderValue());
        }
        try (
                var in = Files.newByteChannel(file.getPath());
                var out = getStorage().writer(blobInfoBuilder.build())
        ) {
            var buffer = ByteBuffer.allocateDirect(1024 * 1024);
            while (in.read(buffer) > 0) {
                buffer.flip();
                out.write(buffer);
                buffer.clear();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void removeFile(FileResource resource) {
        removeFile(getObjectId(resource));
    }

    @Override
    public void removeNamespaceLogo(Namespace namespace) {
        removeFile(getObjectId(namespace));
    }

    private void removeFile(String objectId) {
        if (StringUtils.isEmpty(bucketId)) {
            throw new IllegalStateException("Cannot remove file "
                    + objectId + ": missing Google bucket id");
        }

        getStorage().delete(BlobId.of(bucketId, objectId));
    }

    @Override
    public URI getLocation(FileResource resource) {
        if (StringUtils.isEmpty(bucketId)) {
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
        if(!extVersion.isUniversalTargetPlatform()) {
			segments = ArrayUtils.add(segments, extVersion.getTargetPlatform());
        }

		segments = ArrayUtils.add(segments, extVersion.getVersion());
        segments = ArrayUtils.addAll(segments, resource.getName().split("/"));
        return UrlUtil.createApiUrl("", segments).substring(1); // remove first '/'
    }

    @Override
    public URI getNamespaceLogoLocation(Namespace namespace) {
        if (StringUtils.isEmpty(bucketId)) {
            throw new IllegalStateException("Cannot determine location of file "
                    + namespace.getLogoName() + ": missing Google bucket id");
        }
        return URI.create(BASE_URL + bucketId + "/" + getObjectId(namespace));
    }

    protected String getObjectId(Namespace namespace) {
        return UrlUtil.createApiUrl("", namespace.getName(), "logo", namespace.getLogoName()).substring(1); // remove first '/'
    }

    @Override
    public TempFile downloadNamespaceLogo(Namespace namespace) throws IOException {
        var logoFile = new TempFile("namespace-logo", ".png");
        try (
                var reader = getStorage().reader(BlobId.of(bucketId, getObjectId(namespace)));
                var output = new FileOutputStream(logoFile.getPath().toFile())
        ) {
            output.getChannel().transferFrom(reader, 0, Long.MAX_VALUE);
        }

        return logoFile;
    }

    @Override
    public void copyFiles(List<Pair<FileResource,FileResource>> pairs) {
        for(var pair : pairs) {
            var source = getObjectId(pair.getFirst());
            var target = getObjectId(pair.getSecond());
            var request = new Storage.CopyRequest.Builder()
                    .setSource(BlobId.of(bucketId, source))
                    .setTarget(BlobId.of(bucketId, target))
                    .build();

            getStorage().copy(request).getResult();
        }
    }
}