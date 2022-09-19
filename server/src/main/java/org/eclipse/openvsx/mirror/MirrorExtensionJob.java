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
import org.eclipse.openvsx.entities.UserData;
import org.eclipse.openvsx.json.NamespaceJson;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.schedule.SchedulerService;
import org.eclipse.openvsx.util.NotFoundException;
import org.eclipse.openvsx.util.TimeUtil;
import org.eclipse.openvsx.util.VersionAlias;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.Collections;
import java.util.Comparator;
import java.util.stream.Collectors;

import static org.eclipse.openvsx.schedule.JobUtil.completed;
import static org.eclipse.openvsx.schedule.JobUtil.starting;

public class MirrorExtensionJob implements Job {

    protected final Logger logger = LoggerFactory.getLogger(MirrorExtensionJob.class);

    @Autowired
    AdminService admin;

    @Autowired
    IExtensionRegistry mirror;

    @Autowired
    RepositoryService repositories;

    @Autowired
    SchedulerService schedulerService;

    @Autowired
    DataMirrorService data;

    @Value("${ovsx.data.mirror.user-name}")
    String userName;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        starting(context, logger);
        var map = context.getMergedJobDataMap();
        var namespaceName = map.getString("namespace");
        var extensionName = map.getString("extension");
        var lastModified = map.getString("lastModified");
        var namespace = repositories.findNamespace(namespaceName);
        if(namespace == null) {
            var json = new NamespaceJson();
            json.name = namespaceName;
            admin.createNamespace(json);
            namespace = repositories.findNamespace(namespaceName);
        }

        var extension = data.getDeactivatedExtension(extensionName, namespaceName);
        var extVersions = extension != null
                ? extension.getVersions()
                : Collections.<ExtensionVersion>emptyList();

        JobKey prevPublishExtensionVersionJobKey = null;
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

                targetVersions.stream()
                        .filter(extVersion -> !versions.contains(extVersion.getVersion()))
                        .forEach(extVersion -> deleteExtensionVersion(extVersion, mirrorUser));

                var toAdd = versions.stream()
                        .filter(version -> targetVersions.stream().noneMatch(extVersion -> extVersion.getVersion().equals(version)))
                        .map(version -> mirror.getExtension(namespaceName, extensionName, /*targetPlatform,*/ version))
                        .sorted(Comparator.comparing(extensionJson -> TimeUtil.fromUTCString(extensionJson.timestamp)))
                        .collect(Collectors.toList());

                for (var extensionJson : toAdd) {
                    var mirrorExtensionVersionJobKey = schedulerService.mirrorExtensionVersion(extensionJson);
                    schedulerService.tryChainMirrorJobs(prevPublishExtensionVersionJobKey, mirrorExtensionVersionJobKey);

                    prevPublishExtensionVersionJobKey = schedulerService.generatePublishExtensionVersionJobKey(namespaceName, extensionName, extensionJson.version);
                }
            } catch (SchedulerException e) {
                throw new RuntimeException(e);
            } catch (NotFoundException e) {
                // combination of extension and target platform doesn't exist, try next
            }
//        } TODO uncomment when mirror supports target platforms

        try {
            var mirrorNamespaceVerifiedJobKey = schedulerService.mirrorNamespaceVerified(namespaceName, lastModified);
            schedulerService.tryChainMirrorJobs(prevPublishExtensionVersionJobKey, mirrorNamespaceVerifiedJobKey);
            var mirrorActivateExtensionJobKey = schedulerService.mirrorActivateExtension(namespaceName, extensionName, lastModified);
            if(extension == null) {
                var mirrorExtensionMetadataJobKey = schedulerService.mirrorExtensionMetadata(namespaceName, extensionName, lastModified);
                schedulerService.tryChainMirrorJobs(mirrorNamespaceVerifiedJobKey, mirrorExtensionMetadataJobKey);
                schedulerService.tryChainMirrorJobs(mirrorExtensionMetadataJobKey, mirrorActivateExtensionJobKey);
            } else {
                schedulerService.tryChainMirrorJobs(mirrorNamespaceVerifiedJobKey, mirrorActivateExtensionJobKey);
            }
        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        }

        completed(context, logger);
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
}
