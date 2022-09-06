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

import org.eclipse.openvsx.*;
import org.eclipse.openvsx.entities.ExtensionVersion;
import org.eclipse.openvsx.entities.Namespace;
import org.eclipse.openvsx.entities.NamespaceMembership;
import org.eclipse.openvsx.entities.UserData;
import org.eclipse.openvsx.json.ExtensionJson;
import org.eclipse.openvsx.json.NamespaceJson;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.util.NotFoundException;
import org.eclipse.openvsx.util.TargetPlatform;
import org.eclipse.openvsx.util.TimeUtil;
import org.eclipse.openvsx.util.VersionAlias;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.transaction.Transactional;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.stream.Collectors;

@Component
public class MirrorExtensionJobRequestHandler implements JobRequestHandler<MirrorExtensionJobRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MirrorExtensionJobRequestHandler.class);

    @Autowired
    LocalRegistryService local;

    @Autowired
    AdminService admin;

    @Autowired
    UserService users;

    @Autowired
    DataMirrorService data;

    @Qualifier("mirror")
    @Autowired(required = false)
    IExtensionRegistry mirror;

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    RepositoryService repositories;

    @Autowired
    MirrorExtensionMetadataJobRequestHandler metadataJob;

    @Value("${ovsx.data.mirror.enabled:false}")
    boolean enabled;

    @Value("${ovsx.data.mirror.user-name:}")
    String userName;

    @Override
    @Transactional
    @Job(name="Mirror Extension", retries=10)
    public void run(MirrorExtensionJobRequest jobRequest) throws Exception {
        if(!enabled) {
            return;
        }

        LOGGER.info(">> Starting MirrorExtensionJob for {}.{}", jobRequest.getNamespace(), jobRequest.getExtension());
        var namespaceName = jobRequest.getNamespace();
        var namespace = repositories.findNamespace(namespaceName);
        if(namespace == null) {
            var json = new NamespaceJson();
            json.name = namespaceName;
            admin.createNamespace(json);
            namespace = repositories.findNamespace(namespaceName);
        }

        var extensionName = jobRequest.getExtension();
        var extension = repositories.findExtension(extensionName, namespaceName);
        var extVersions = extension != null
                ? extension.getVersions()
                : Collections.<ExtensionVersion>emptyList();

        var mirrorUser = repositories.findUserByLoginName(null, userName);
//        for(var targetPlatform : TargetPlatform.TARGET_PLATFORM_NAMES) { TODO uncomment when mirror supports target platforms
            String targetPlatform = null;
            var targetVersions = extVersions.stream()
//                    .filter(extVersion -> extVersion.getTargetPlatform().equals(targetPlatform)) TODO uncomment when mirror supports target platforms
                    .collect(Collectors.toList());

            try {
                var json = mirror.getExtension(namespaceName, extensionName, targetPlatform);
                var versions = json.allVersions.keySet();
                VersionAlias.ALIAS_NAMES.forEach(versions::remove);

                var toAdd = versions.stream()
                        .filter(version -> targetVersions.stream().noneMatch(extVersion -> extVersion.getVersion().equals(version)))
                        .map(version -> mirror.getExtension(namespaceName, extensionName, /*targetPlatform,*/ version))
                        .sorted(Comparator.comparing(extensionJson -> TimeUtil.fromUTCString(extensionJson.timestamp)))
                        .collect(Collectors.toList());
                for(var extensionJson : toAdd) {
                    LOGGER.info("MirrorExtensionVersion {}.{}-{}@{}", namespace.getName(), extensionName, extensionJson.version, targetPlatform);
                    addExtensionVersion(namespace, extensionJson);
                }

                targetVersions.stream()
                        .filter(extVersion ->  !versions.contains(extVersion.getVersion()))
                        .forEach(extVersion -> deleteExtensionVersion(extVersion, mirrorUser));
            } catch (NotFoundException e) {
                // combination of extension and target platform doesn't exist, try next
            }
//        } TODO uncomment when mirror supports target platforms

        mirrorNamespaceVerified(namespace);
        if(extension == null) {
            // extension didn't exist before, get initial metadata
            metadataJob.run(new MirrorExtensionMetadataJobRequest(namespaceName, extensionName));
        }

        LOGGER.info("<< Completed MirrorExtensionJob for {}.{}", jobRequest.getNamespace(), jobRequest.getExtension());
    }

    private void addExtensionVersion(Namespace namespace, ExtensionJson json) throws IOException {
        var vsixPackage = restTemplate.getForObject(URI.create(json.files.get("download")), byte[].class);

        var user = data.getOrAddUser(json.publishedBy);
        getOrAddNamespaceMembership(user, namespace);

        var description = "MirrorExtensionVersion";
        var accessTokenValue = data.getOrAddAccessTokenValue(user, description);
        try(var input = new ByteArrayInputStream(vsixPackage)) {
            local.publish(input, accessTokenValue);
        }

        var targetPlatform = TargetPlatform.NAME_UNIVERSAL; // TODO remove when mirror supports target platform
        data.updateTimestamps(namespace.getName(), json.name, targetPlatform, json.version, json.timestamp);
    }

    private void getOrAddNamespaceMembership(UserData user, Namespace namespace) {
        var membership = repositories.findMembership(user, namespace);
        if(membership == null) {
            users.addNamespaceMember(namespace, user, NamespaceMembership.ROLE_CONTRIBUTOR);
        }
    }

    private void deleteExtensionVersion(ExtensionVersion extVersion, UserData user) {
        var extension = extVersion.getExtension();
        admin.deleteExtension(
                extension.getNamespace().getName(),
                extension.getName(),
                extVersion.getTargetPlatform(),
                extVersion.getVersion(),
                user
        );
    }

    private void mirrorNamespaceVerified(Namespace namespace) {
        var remoteVerified = mirror.getNamespace(namespace.getName()).verified;
        var localVerified = local.getNamespace(namespace.getName()).verified;
        if(!localVerified && remoteVerified) {
            // verify the namespace by adding an owner to it
            var memberships = repositories.findMemberships(namespace);
            users.addNamespaceMember(namespace, memberships.toList().get(0).getUser(), NamespaceMembership.ROLE_OWNER);
        }
        if(localVerified && !remoteVerified) {
            // unverify namespace by changing owner(s) back to contributor
            repositories.findMemberships(namespace, NamespaceMembership.ROLE_OWNER)
                    .forEach(membership -> users.addNamespaceMember(namespace, membership.getUser(), NamespaceMembership.ROLE_CONTRIBUTOR));
        }
    }
}
