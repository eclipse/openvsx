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

import com.google.cloud.storage.*;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.openvsx.cache.FilesCacheKeyGenerator;
import org.eclipse.openvsx.entities.FileResource;
import org.eclipse.openvsx.entities.Namespace;
import org.eclipse.openvsx.util.FileUtil;
import org.eclipse.openvsx.util.TempFile;
import org.eclipse.openvsx.util.UrlUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerErrorException;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.eclipse.openvsx.cache.CacheService.CACHE_EXTENSION_FILES;
import static org.eclipse.openvsx.cache.CacheService.GENERATOR_FILES;

@Component
public class GoogleCloudStorageService implements IStorageService {

    private static final String BASE_URL = "https://storage.googleapis.com/";

    private final FilesCacheKeyGenerator filesCacheKeyGenerator;

    @Value("${ovsx.storage.gcp.project-id:}")
    String projectId;

    @Value("${ovsx.storage.gcp.bucket-id:}")
    String bucketId;

    private Storage storage;

    public GoogleCloudStorageService(FilesCacheKeyGenerator filesCacheKeyGenerator) {
        this.filesCacheKeyGenerator = filesCacheKeyGenerator;
    }

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
    public void uploadFile(TempFile tempFile) {
        var resource = tempFile.getResource();
        var objectId = getObjectId(resource);
        if (StringUtils.isEmpty(bucketId)) {
            throw new IllegalStateException(missingBucketIdMessage("Cannot upload file", resource.getName()));
        }

        uploadFile(tempFile, resource.getName(), objectId);
    }

    @Override
    public void uploadNamespaceLogo(TempFile logoFile) {
        var namespace = logoFile.getNamespace();
        var objectId = getObjectId(namespace);
        if (StringUtils.isEmpty(bucketId)) {
            throw new IllegalStateException(missingBucketIdMessage("Cannot upload file", objectId));
        }

        uploadFile(logoFile, namespace.getLogoName(), objectId);
    }

    protected void uploadFile(TempFile file, String fileName, String objectId) {
        var blobInfoBuilder = BlobInfo.newBuilder(BlobId.of(bucketId, objectId))
                .setContentType(StorageUtil.getFileType(fileName).toString());
        if (fileName.endsWith(".vsix") || fileName.endsWith(".sigzip")) {
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
            throw new ServerErrorException("Failed to upload file", e);
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
            throw new IllegalStateException(missingBucketIdMessage("Cannot remove file", objectId));
        }

        getStorage().delete(BlobId.of(bucketId, objectId));
    }

    @Override
    public URI getLocation(FileResource resource) {
        if (StringUtils.isEmpty(bucketId)) {
            throw new IllegalStateException(missingBucketIdMessage(resource.getName()));
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
            throw new IllegalStateException(missingBucketIdMessage(namespace.getLogoName()));
        }
        return URI.create(BASE_URL + bucketId + "/" + getObjectId(namespace));
    }

    @Override
    public TempFile downloadFile(FileResource resource) throws IOException {
        if (StringUtils.isEmpty(bucketId)) {
            throw new IllegalStateException(missingBucketIdMessage(resource.getName()));
        }

        var tempFile = new TempFile("temp_file_", "");
        var objectId = getObjectId(resource);
        getStorage().downloadTo(BlobId.of(bucketId, objectId), tempFile.getPath());
        tempFile.setResource(resource);
        return tempFile;
    }

    protected String getObjectId(Namespace namespace) {
        return UrlUtil.createApiUrl("", namespace.getName(), "logo", namespace.getLogoName()).substring(1); // remove first '/'
    }

    private String missingBucketIdMessage(String name) {
        return missingBucketIdMessage("Cannot determine location of file", name);
    }

    private String missingBucketIdMessage(String action, String name) {
        return action + " " + name + ": missing Google bucket id";
    }

    @Override
    public void copyFiles(List<Pair<FileResource,FileResource>> pairs) {
        pairs.forEach(pair -> copy(getObjectId(pair.getFirst()), getObjectId(pair.getSecond())));
    }

    @Override
    public void copyNamespaceLogo(Namespace oldNamespace, Namespace newNamespace) {
        copy(getObjectId(oldNamespace), getObjectId(newNamespace));
    }

    private void copy(String source, String target) {
        var request = new Storage.CopyRequest.Builder()
                .setSource(BlobId.of(bucketId, source))
                .setTarget(BlobId.of(bucketId, target))
                .build();

        getStorage().copy(request).getResult();
    }

    @Override
    @Cacheable(value = CACHE_EXTENSION_FILES, keyGenerator = GENERATOR_FILES, cacheManager = "fileCacheManager")
    public Path getCachedFile(FileResource resource) {
        if (StringUtils.isEmpty(bucketId)) {
            throw new IllegalStateException(missingBucketIdMessage(resource.getName()));
        }

        var objectId = getObjectId(resource);
        var path = filesCacheKeyGenerator.generateCachedExtensionPath(resource);
        FileUtil.writeSync(path, p -> getStorage().downloadTo(BlobId.of(bucketId, objectId), p));
        return path;
    }
}
