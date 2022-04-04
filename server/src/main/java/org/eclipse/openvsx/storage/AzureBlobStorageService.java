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

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.models.BlobStorageException;
import com.google.common.base.Strings;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.openvsx.entities.FileResource;
import org.eclipse.openvsx.util.TargetPlatform;
import org.eclipse.openvsx.util.UrlUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;

@Component
public class AzureBlobStorageService implements IStorageService {

    @Value("${ovsx.storage.azure.service-endpoint:}")
    String serviceEndpoint;

    @Value("${ovsx.storage.azure.sas-token:}")
    String sasToken;

    @Value("${ovsx.storage.azure.blob-container:openvsx-resources}")
    String blobContainer;

    private BlobContainerClient containerClient;

	@Override
	public boolean isEnabled() {
		return !Strings.isNullOrEmpty(serviceEndpoint);
    }
    
    protected BlobContainerClient getContainerClient() {
        if (containerClient == null) {
            containerClient = new BlobContainerClientBuilder()
                    .endpoint(serviceEndpoint)
                    .sasToken(sasToken)
                    .containerName(blobContainer)
                    .buildClient();
        }
        return containerClient;
    }

	@Override
    public void uploadFile(FileResource resource) {
        var blobName = getBlobName(resource);
        if (Strings.isNullOrEmpty(serviceEndpoint)) {
            throw new IllegalStateException("Cannot upload file "
                    + blobName + ": missing Azure blob service endpoint");
        }

        uploadFile(resource.getContent(), resource.getName(), blobName);
    }
    
    protected void uploadFile(byte[] content, String fileName, String blobName) {
        var blobClient = getContainerClient().getBlobClient(blobName);
        var headers = new BlobHttpHeaders();
        headers.setContentType(StorageUtil.getFileType(fileName).toString());
        if (fileName.endsWith(".vsix")) {
            headers.setContentDisposition("attachment; filename=\"" + fileName + "\"");
        } else {
            var cacheControl = StorageUtil.getCacheControl(fileName);
            headers.setCacheControl(cacheControl.getHeaderValue());
        }
        try (var dataStream = new ByteArrayInputStream(content)) {
            blobClient.upload(dataStream, content.length, true);
            blobClient.setHttpHeaders(headers);
        } catch (IOException exc) {
            throw new RuntimeException(exc);
        }
    }

	@Override
	public void removeFile(FileResource resource) {
		var blobName = getBlobName(resource);
        if (Strings.isNullOrEmpty(serviceEndpoint)) {
            throw new IllegalStateException("Cannot remove file "
                    + blobName + ": missing Azure blob service endpoint");
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
        if (Strings.isNullOrEmpty(serviceEndpoint)) {
            throw new IllegalStateException("Cannot determine location of file "
                    + blobName + ": missing Azure blob service endpoint");
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
		if(!TargetPlatform.isUniversal(extVersion)) {
		    segments = ArrayUtils.add(segments, extVersion.getTargetPlatform());
        }

	    segments = ArrayUtils.add(segments, extVersion.getVersion());
        segments = ArrayUtils.addAll(segments, resource.getName().split("/"));
        return UrlUtil.createApiUrl("", segments).substring(1); // remove first '/'
    }
    
}