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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import org.eclipse.openvsx.entities.ExtensionVersion;
import org.eclipse.openvsx.entities.FileResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AzureBlobStorageService implements IStorageService {

    @Autowired
    StorageUtilService storageUtil;

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
	@Transactional(TxType.MANDATORY)
    public void uploadFile(FileResource resource) {
        var blobName = getBlobName(resource.getName(), resource.getExtension());
        if (Strings.isNullOrEmpty(serviceEndpoint)) {
            throw new IllegalStateException("Cannot upload file "
                    + blobName + ": missing Azure blob service endpoint");
        }

        uploadFile(resource.getContent(), resource.getName(), blobName);
        resource.setStorageType(FileResource.STORAGE_AZURE);
    }
    
    protected void uploadFile(byte[] content, String fileName, String blobName) {
        var blobClient = getContainerClient().getBlobClient(blobName);
        var headers = new BlobHttpHeaders();
        headers.setContentType(storageUtil.getFileType(fileName).toString());
        if (fileName.endsWith(".vsix")) {
            headers.setContentDisposition("attachment; filename=\"" + fileName + "\"");
        } else {
            var cacheControl = storageUtil.getCacheControl(fileName);
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
		var blobName = getBlobName(resource.getName(), resource.getExtension());
        if (Strings.isNullOrEmpty(serviceEndpoint)) {
            throw new IllegalStateException("Cannot remove file "
                    + blobName + ": missing Azure blob service endpoint");
        }
        getContainerClient().getBlobClient(blobName).delete();
	}

	@Override
	public URI getLocation(FileResource resource) {
		var blobName = getBlobName(resource.getName(), resource.getExtension());
        if (Strings.isNullOrEmpty(serviceEndpoint)) {
            throw new IllegalStateException("Cannot determine location of file "
                    + blobName + ": missing Azure blob service endpoint");
        }
        if (!serviceEndpoint.endsWith("/")) {
            throw new IllegalStateException("The Azure blob service endpoint URL must end with a slash.");
        }
        return URI.create(serviceEndpoint + blobContainer + "/" + blobName);
	}

    protected String getBlobName(String name, ExtensionVersion extVersion) {
        Preconditions.checkNotNull(name);
        try {
            var extension = extVersion.getExtension();
            var namespace = extension.getNamespace();
			return namespace.getName()
			        + "/" + extension.getName()
			        + "/" + URLEncoder.encode(extVersion.getVersion(), "UTF-8")
			        + "/" + name;
		} catch (UnsupportedEncodingException exc) {
			throw new RuntimeException(exc);
		}
    }
    
}