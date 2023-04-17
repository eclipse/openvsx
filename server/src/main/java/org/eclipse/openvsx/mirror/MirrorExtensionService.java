/** ******************************************************************************
 * Copyright (c) 2022 Precies. Software Ltd and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.mirror;

import org.eclipse.openvsx.ExtensionService;
import org.eclipse.openvsx.UpstreamRegistryService;
import org.eclipse.openvsx.UserService;
import org.eclipse.openvsx.entities.UserData;
import org.eclipse.openvsx.json.ExtensionJson;
import org.eclipse.openvsx.json.UserJson;
import org.eclipse.openvsx.publish.ExtensionVersionIntegrityService;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.util.*;
import org.jobrunr.jobs.context.JobContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.eclipse.openvsx.entities.FileResource.DOWNLOAD_SIG;
import static org.eclipse.openvsx.entities.FileResource.PUBLIC_KEY;

@Component
public class MirrorExtensionService {

    protected final Logger logger = LoggerFactory.getLogger(MirrorExtensionService.class);

    @Autowired(required = false)
    DataMirrorService data;

    @Autowired
    RepositoryService repositories;

    @Autowired
    UpstreamRegistryService upstream;

    @Autowired
    RestTemplate backgroundRestTemplate;

    @Autowired
    RestTemplate backgroundNonRedirectingRestTemplate;

    @Autowired
    UserService users;

    @Autowired
    ExtensionService extensions;

    @Autowired
    ExtensionVersionIntegrityService integrityService;

    /**
     * It applies delta from previous execution.
     */
    public void mirrorExtension(String namespaceName, String extensionName, UserData mirrorUser, LocalDate lastModified, JobContext jobContext) {
        var latest = upstream.getExtension(namespaceName, extensionName, null);
        if (shouldMirrorExtensionVersions(namespaceName, extensionName, lastModified, latest)) {
            mirrorExtensionVersions(namespaceName, extensionName, mirrorUser, jobContext);
        } else {
            jobContext.logger().info("all versions are up to date " + NamingUtil.toExtensionId(namespaceName, extensionName));
        }

        var extensionId = logger.isDebugEnabled() ? NamingUtil.toExtensionId(namespaceName, extensionName) : null;
        logger.debug("activating extension: {}", extensionId);
        data.activateExtension(namespaceName, extensionName);

        logger.debug("updating extension metadata: {}", extensionId);
        data.updateMetadata(namespaceName, extensionName, latest);
        
        logger.debug("updating namespace metadata: {}", namespaceName);
        data.mirrorNamespaceMetadata(namespaceName);
    }
    
    private boolean shouldMirrorExtensionVersions(String namespaceName, String extensionName, LocalDate lastModified, ExtensionJson latest) {
        if (lastModified == null) {
            return true;
        }

        var extension = repositories.findExtension(extensionName, namespaceName);
        if (extension == null) {
            return true;
        }

        var lastUpdated = extension.getLastUpdatedDate();
        return lastUpdated.toLocalDate().isBefore(lastModified) || lastUpdated.isBefore(TimeUtil.fromUTCString(latest.timestamp));
    }

    private void mirrorExtensionVersions(String namespaceName, String extensionName, UserData mirrorUser, JobContext jobContext) {
        data.ensureNamespace(namespaceName);

        var toAdd = new ArrayList<ExtensionJson>();
        for(var targetPlatform : TargetPlatform.TARGET_PLATFORM_NAMES) {
            Set<String> versions;
            try {
                var json = upstream.getExtension(namespaceName, extensionName, targetPlatform);
                versions = json.allVersions.keySet();
                VersionAlias.ALIAS_NAMES.forEach(versions::remove);
            } catch (NotFoundException e) {
                // combination of extension and target platform doesn't exist, try next
                continue;
            }

            var targetVersions = data.getExtensionTargetVersions(namespaceName, extensionName, targetPlatform);

            targetVersions.stream()
                    .filter(extVersion -> !versions.contains(extVersion.getVersion()))
                    .forEach(extVersion -> data.deleteExtensionVersion(extVersion, mirrorUser));

            toAdd.addAll(
                versions.stream()
                    .filter(version -> targetVersions.stream().noneMatch(extVersion -> extVersion.getVersion().equals(version)))
                    .map(version -> upstream.getExtension(namespaceName, extensionName, targetPlatform, version))
                    .collect(Collectors.toList())
            );
        }
        toAdd.sort(Comparator.comparing(extensionJson -> TimeUtil.fromUTCString(extensionJson.timestamp)));
        
        for(var i = 0; i < toAdd.size(); i++) {
            var json = toAdd.get(i);
            jobContext.logger().info("mirroring " + NamingUtil.toLogFormat(json) + " (" + (i+1) + "/" +  toAdd.size() + ")");
            try {
                mirrorExtensionVersion(json);
                data.getMirroredVersions().increment();
            } catch (Throwable t) {
                data.getFailedVersions().increment();
                throw t;
            }
        }
    }

    private void mirrorExtensionVersion(ExtensionJson json) throws RuntimeException {
        var download = json.files.get("download");
        var userJson = new UserJson();
        userJson.provider = json.publishedBy.provider;
        userJson.loginName = json.publishedBy.loginName;
        userJson.fullName = json.publishedBy.fullName;
        userJson.avatarUrl = json.publishedBy.avatarUrl;
        userJson.homepage = json.publishedBy.homepage;
        var namespaceName = json.namespace;

        var vsixResourceHeaders = backgroundNonRedirectingRestTemplate.headForHeaders("{resolveVsixLocation}", Map.of("resolveVsixLocation", download));
        var vsixLocation = vsixResourceHeaders.getLocation();
        if (vsixLocation == null) {
            throw new RuntimeException("Failed to parse location header from redirected vsix url");
        }

        var tokens = vsixLocation.getPath().split("/");
        var filename = tokens[tokens.length-1];
        if (!filename.endsWith(".vsix")) {
            throw new RuntimeException("Invalid vsix filename from redirected vsix url");
        }

        String signatureName = null;
        try (var extensionFile = downloadToFile(download, "extension_", ".vsix")) {
            if(json.files.containsKey(DOWNLOAD_SIG)) {
                try(
                        var signatureFile = downloadToFile(json.files.get(DOWNLOAD_SIG), "extension_", ".sigzip");
                        var publicKeyFile = downloadToFile(json.files.get(PUBLIC_KEY), "public_", ".pem")
                ) {
                    var verified = integrityService.verifyExtensionVersion(extensionFile, signatureFile, publicKeyFile);
                    if (!verified) {
                        throw new RuntimeException("Unverified vsix package");
                    }
                }

                var signaturePathParams = URI.create(json.files.get("signature")).getPath().split("/");
                signatureName = signaturePathParams[signaturePathParams.length - 1];
            }

            var user = data.getOrAddUser(userJson);
            var namespace = repositories.findNamespace(namespaceName);
            data.ensureNamespaceMembership(user, namespace);

            var description = "MirrorExtensionVersion";
            var accessTokenValue = data.getOrAddAccessTokenValue(user, description);

            var token = users.useAccessToken(accessTokenValue);
            extensions.mirrorVersion(extensionFile, signatureName, token, filename, json.timestamp);
            logger.debug("completed mirroring of extension version: {}", NamingUtil.toLogFormat(json));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private TempFile downloadToFile(String url, String prefix, String suffix) throws IOException {
        var file = new TempFile(prefix, suffix);
        backgroundRestTemplate.execute("{url}", HttpMethod.GET, null, response -> {
            try(var out = Files.newOutputStream(file.getPath())) {
                response.getBody().transferTo(out);
            }

            return file;
        }, Map.of("url", url));

        return file;
    }
}
