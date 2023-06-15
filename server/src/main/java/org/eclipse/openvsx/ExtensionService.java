/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;

import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.openvsx.cache.CacheService;
import org.eclipse.openvsx.entities.*;
import org.eclipse.openvsx.publish.PublishExtensionVersionHandler;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.search.SearchUtilService;
import org.eclipse.openvsx.util.ErrorResultException;
import org.eclipse.openvsx.util.TempFile;
import org.eclipse.openvsx.util.TimeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class ExtensionService {

    private static final int MAX_CONTENT_SIZE = 512 * 1024 * 1024;

    @Autowired
    RepositoryService repositories;

    @Autowired
    SearchUtilService search;

    @Autowired
    CacheService cache;

    @Autowired
    PublishExtensionVersionHandler publishHandler;

    @Value("${ovsx.publishing.require-license:false}")
    boolean requireLicense;

    @Transactional
    public ExtensionVersion mirrorVersion(TempFile extensionFile, String signatureName, PersonalAccessToken token, String binaryName, String timestamp) {
        var download = doPublish(extensionFile, binaryName, token, TimeUtil.fromUTCString(timestamp), false);
        publishHandler.mirror(download, extensionFile, signatureName);
        return download.getExtension();
    }

    public ExtensionVersion publishVersion(InputStream content, PersonalAccessToken token) {
        var extensionFile = createExtensionFile(content);
        var download = doPublish(extensionFile, null, token, TimeUtil.getCurrentUTC(), true);
        publishHandler.publishAsync(download, extensionFile, this);
        return download.getExtension();
    }

    private FileResource doPublish(TempFile extensionFile, String binaryName, PersonalAccessToken token, LocalDateTime timestamp, boolean checkDependencies) {
        try (var processor = new ExtensionProcessor(extensionFile)) {
            var extVersion = publishHandler.createExtensionVersion(processor, token, timestamp, checkDependencies);
            if (requireLicense) {
                // Check the extension's license
                var license = processor.getLicense(extVersion);
                checkLicense(extVersion, license);
            }

            return processor.getBinary(extVersion, binaryName);
        }
    }

    private TempFile createExtensionFile(InputStream content) {
        try (var input = new BufferedInputStream(content)) {
            input.mark(0);
            var skipped = input.skip(MAX_CONTENT_SIZE  + 1);
            if (skipped > MAX_CONTENT_SIZE) {
                throw new ErrorResultException("The extension package exceeds the size limit of 512 MB.", HttpStatus.PAYLOAD_TOO_LARGE);
            }

            var extensionFile = new TempFile("extension_", ".vsix");
            try(var out = Files.newOutputStream(extensionFile.getPath())) {
                input.reset();
                input.transferTo(out);
            }

            return extensionFile;
        } catch (IOException e) {
            throw new ErrorResultException("Failed to read extension file", e);
        }
    }

    private void checkLicense(ExtensionVersion extVersion, FileResource license) {
        if (StringUtils.isEmpty(extVersion.getLicense()) && (license == null || !license.getType().equals(FileResource.LICENSE))) {
            throw new ErrorResultException("This extension cannot be accepted because it has no license.");
        }
    }

    /**
     * Update the given extension after a version has been published
     * or the {@code active} statuses of its versions have changed.
     */
    @Transactional(TxType.REQUIRED)
    public void updateExtension(Extension extension) {
        cache.evictNamespaceDetails(extension);
        cache.evictLatestExtensionVersion(extension);
        cache.evictExtensionJsons(extension);

        if (extension.getVersions().stream().anyMatch(ExtensionVersion::isActive)) {
            // There is at least one active version => activate the extension
            extension.setActive(true);
            search.updateSearchEntry(extension);
        } else if (extension.isActive()) {
            // All versions are deactivated => deactivate the extensions
            extension.setActive(false);
            search.removeSearchEntry(extension);
        }
    }

    /**
     * Reactivate all extension versions that have been published by the given user.
     */
    @Transactional
    public void reactivateExtensions(UserData user) {
        var accessTokens = repositories.findAccessTokens(user);
        var affectedExtensions = new LinkedHashSet<Extension>();
        for (var accessToken : accessTokens) {
            var versions = repositories.findVersionsByAccessToken(accessToken, false);
            for (var version : versions) {
                version.setActive(true);
                affectedExtensions.add(version.getExtension());
            }
        }
        for (var extension : affectedExtensions) {
            updateExtension(extension);
        }
    }
}