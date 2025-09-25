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

import com.azure.core.http.policy.UserAgentPolicy;
import com.azure.core.util.polling.SyncPoller;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import com.azure.storage.blob.models.BlobCopyInfo;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.blob.models.CopyStatusType;
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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static org.eclipse.openvsx.cache.CacheService.CACHE_EXTENSION_FILES;
import static org.eclipse.openvsx.cache.CacheService.GENERATOR_FILES;

@Component
public class AzureBlobStorageService implements IStorageService {

    public static final String AZURE_USER_AGENT = "OpenVSX";

    private final FilesCacheKeyGenerator filesCacheKeyGenerator;

    @Value("${ovsx.storage.azure.service-endpoint:}")
    String serviceEndpoint;

    @Value("${ovsx.storage.azure.sas-token:}")
    String sasToken;

    @Value("${ovsx.storage.azure.blob-container:openvsx-resources}")
    String blobContainer;

    private BlobContainerClient containerClient;

    public AzureBlobStorageService(FilesCacheKeyGenerator filesCacheKeyGenerator) {
        this.filesCacheKeyGenerator = filesCacheKeyGenerator;
    }

	@Override
	public boolean isEnabled() {
		return !StringUtils.isEmpty(serviceEndpoint);
    }
    
    protected BlobContainerClient getContainerClient() {
        if (containerClient == null) {
            containerClient = new BlobContainerClientBuilder()
                    .endpoint(serviceEndpoint)
                    .sasToken(sasToken)
                    .containerName(blobContainer)
                    .addPolicy(new UserAgentPolicy(AZURE_USER_AGENT))
                    .buildClient();
        }
        return containerClient;
    }

    private String missingEndpointMessage(String name) {
        return missingEndpointMessage("Cannot determine location of file", name);
    }

    private String missingEndpointMessage(String action, String name) {
        return action + " " + name + ": missing Azure blob service endpoint";
    }

	@Override
    public void uploadFile(TempFile tempFile) {
        var resource = tempFile.getResource();
        var blobName = getBlobName(resource);
        uploadFile(tempFile, resource.getName(), blobName);
    }

    @Override
    public void uploadNamespaceLogo(TempFile logoFile) {
        var namespace = logoFile.getNamespace();
        var blobName = getBlobName(namespace);
        uploadFile(logoFile, namespace.getLogoName(), blobName);
    }

    protected void uploadFile(TempFile file, String fileName, String blobName) {
        if (StringUtils.isEmpty(serviceEndpoint)) {
            throw new IllegalStateException(missingEndpointMessage("Cannot upload file", blobName));
        }

        var blobClient = getContainerClient().getBlobClient(blobName);
        var headers = new BlobHttpHeaders();
        headers.setContentType(StorageUtil.getFileType(fileName).toString());
        if (fileName.endsWith(".vsix") || fileName.endsWith(".sigzip")) {
            headers.setContentDisposition("attachment; filename=\"" + fileName + "\"");
        } else {
            var cacheControl = StorageUtil.getCacheControl(fileName);
            headers.setCacheControl(cacheControl.getHeaderValue());
        }

        blobClient.uploadFromFile(file.getPath().toAbsolutePath().toString(), true);
        blobClient.setHttpHeaders(headers);
    }

	@Override
	public void removeFile(FileResource resource) {
		removeFile(getBlobName(resource));
	}

    @Override
    public void removeNamespaceLogo(Namespace namespace) {
        removeFile(getBlobName(namespace));
    }

    private void removeFile(String blobName) {
        if (StringUtils.isEmpty(serviceEndpoint)) {
            throw new IllegalStateException(missingEndpointMessage("Cannot remove file", blobName));
        }

        try {
            getContainerClient().getBlobClient(blobName).delete();
        } catch(BlobStorageException e) {
            if(e.getStatusCode() != HttpStatus.NOT_FOUND.value()) {
                // 404 indicates that the file is already deleted
                // so only throw an exception for other status codes
                throw e;
            }
        }
    }

    @Override
	public URI getLocation(FileResource resource) {
        var blobName = getBlobName(resource);
        if (StringUtils.isEmpty(serviceEndpoint)) {
            throw new IllegalStateException(missingEndpointMessage(blobName));
        }
        if (!serviceEndpoint.endsWith("/")) {
            throw new IllegalStateException("The Azure blob service endpoint URL must end with a slash.");
        }
        return URI.create(serviceEndpoint + blobContainer + "/" + blobName);
	}

    protected String getBlobName(FileResource resource) {
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
        var blobName = getBlobName(namespace);
        if (StringUtils.isEmpty(serviceEndpoint)) {
            throw new IllegalStateException(missingEndpointMessage(blobName));
        }
        if (!serviceEndpoint.endsWith("/")) {
            throw new IllegalStateException("The Azure blob service endpoint URL must end with a slash.");
        }
        return URI.create(serviceEndpoint + blobContainer + "/" + blobName);
    }

    @Override
    public TempFile downloadFile(FileResource resource) throws IOException {
        var blobName = getBlobName(resource);
        if (StringUtils.isEmpty(serviceEndpoint)) {
            throw new IllegalStateException(missingEndpointMessage(blobName));
        }

        var tempFile = new TempFile("temp_file_", "");
        getContainerClient().getBlobClient(blobName).downloadToFile(tempFile.getPath().toAbsolutePath().toString(), true);
        tempFile.setResource(resource);
        return tempFile;
    }

    protected String getBlobName(Namespace namespace) {
        return UrlUtil.createApiUrl("", namespace.getName(), "logo", namespace.getLogoName()).substring(1); // remove first '/'
    }

    @Override
    public void copyFiles(List<Pair<FileResource,FileResource>> pairs) {
        var copyOperations = new ArrayList<SyncPoller<BlobCopyInfo, Void>>();
        for(var pair : pairs) {
            var oldLocation = getLocation(pair.getFirst()).toString();
            var newBlobName = getBlobName(pair.getSecond());
            var poller = getContainerClient().getBlobClient(newBlobName)
                    .beginCopy(oldLocation, Duration.of(1, ChronoUnit.SECONDS));

            copyOperations.add(poller);
        }
        for(var poller : copyOperations) {
            var response = poller.waitForCompletion();
            if(response.getValue().getCopyStatus() != CopyStatusType.SUCCESS) {
                throw new IllegalStateException(response.getValue().getError());
            }
        }
    }

    @Override
    public void copyNamespaceLogo(Namespace oldNamespace, Namespace newNamespace) {
        var oldLocation = getNamespaceLogoLocation(oldNamespace).toString();
        var newBlobName = getBlobName(newNamespace);
        var poller = getContainerClient().getBlobClient(newBlobName)
                .beginCopy(oldLocation, Duration.of(1, ChronoUnit.SECONDS));

        var response = poller.waitForCompletion();
        if(response.getValue().getCopyStatus() != CopyStatusType.SUCCESS) {
            throw new IllegalStateException(response.getValue().getError());
        }
    }

    @Override
    @Cacheable(value = CACHE_EXTENSION_FILES, keyGenerator = GENERATOR_FILES, cacheManager = "fileCacheManager")
    public Path getCachedFile(FileResource resource) {
        var blobName = getBlobName(resource);
        if (StringUtils.isEmpty(serviceEndpoint)) {
            throw new IllegalStateException(missingEndpointMessage(blobName));
        }

        var path = filesCacheKeyGenerator.generateCachedExtensionPath(resource);
        FileUtil.writeSync(path, p -> getContainerClient().getBlobClient(blobName).downloadToFile(p.toAbsolutePath().toString()));
        return path;
    }
}