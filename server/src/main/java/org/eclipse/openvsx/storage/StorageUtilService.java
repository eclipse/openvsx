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

import java.net.URLConnection;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.transaction.Transactional;

import com.google.common.base.Strings;

import org.eclipse.openvsx.entities.ExtensionVersion;
import org.eclipse.openvsx.entities.FileResource;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.search.SearchService;
import org.eclipse.openvsx.util.UrlUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
public class StorageUtilService {

    @Autowired
    RepositoryService repositories;

    @Autowired
    SearchService search;

    public boolean shouldStoreExternally(FileResource resource) {
        return resource.getType().equals(FileResource.DOWNLOAD);
    }

    public void addFileUrls(ExtensionVersion extVersion, String serverUrl, Map<String, String> type2Url,
            String... types) {
        for (var type : types) {
            var fileUrl = getFileUrl(extVersion, serverUrl, type);
            if (fileUrl != null)
                type2Url.put(type, fileUrl);
        }
    }

    public String getFileUrl(ExtensionVersion extVersion, String serverUrl, String type) {
        var resource = repositories.findFileByType(extVersion, type);
        if (resource == null)
            return null;
        if (resource.getStorageType().equals(FileResource.STORAGE_DB) && !Strings.isNullOrEmpty(resource.getUrl())) {
            // Locally stored resources are expected to hold a relative URL starting with "/"
            if (serverUrl.endsWith("/"))
                return serverUrl + resource.getUrl().substring(1);
            else
                return serverUrl + resource.getUrl();
        }
        var extension = extVersion.getExtension();
        var namespace = extension.getNamespace();
        return UrlUtil.createApiUrl(serverUrl, "api", namespace.getName(), extension.getName(), extVersion.getVersion(),
                        "file", resource.getName());
    }

    @Transactional
    public void increaseDownloadCount(ExtensionVersion extVersion) {
        var extension = extVersion.getExtension();
        extension.setDownloadCount(extension.getDownloadCount() + 1);
        search.updateSearchEntry(extension);
    }

    public HttpHeaders getFileResponseHeaders(String fileName) {
        var headers = new HttpHeaders();
        MediaType fileType = getFileType(fileName);
        headers.setContentType(fileType);
        // Files are requested with a version string in the URL, so their content cannot change
        headers.setCacheControl(CacheControl.maxAge(30, TimeUnit.DAYS));
        if (fileName.endsWith(".vsix")) {
            headers.add("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        }
        return headers;
    }

    public MediaType getFileType(String fileName) {
        if (fileName.endsWith(".vsix")) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        var contentType = URLConnection.guessContentTypeFromName(fileName);
        if (contentType != null) {
            return MediaType.parseMediaType(contentType);
        }
        return MediaType.TEXT_PLAIN;
    }
    
}