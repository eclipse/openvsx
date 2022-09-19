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

import org.eclipse.openvsx.LocalRegistryService;
import org.eclipse.openvsx.UserService;
import org.eclipse.openvsx.entities.Namespace;
import org.eclipse.openvsx.entities.NamespaceMembership;
import org.eclipse.openvsx.entities.UserData;
import org.eclipse.openvsx.json.ExtensionJson;
import org.eclipse.openvsx.json.UserJson;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;

import static org.eclipse.openvsx.schedule.JobUtil.completed;
import static org.eclipse.openvsx.schedule.JobUtil.starting;

public class MirrorExtensionVersionJob implements Job {

    protected final Logger logger = LoggerFactory.getLogger(MirrorExtensionVersionJob.class);

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    DataMirrorService data;

    @Autowired
    RepositoryService repositories;

    @Autowired
    LocalRegistryService local;

    @Autowired
    UserService users;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        starting(context, logger);
        var map = context.getMergedJobDataMap();
        var download = map.getString("download");
        var userJson = new UserJson();
        userJson.provider = map.getString("userProvider");
        userJson.loginName = map.getString("userLoginName");
        userJson.fullName = map.getString("userFullName");
        userJson.avatarUrl = map.getString("userAvatarUrl");
        userJson.homepage = map.getString("userHomepage");
        var namespaceName = map.getString("namespace");
        var vsixPackage = restTemplate.getForObject(URI.create(download), byte[].class);

        var user = data.getOrAddUser(userJson);
        var namespace = repositories.findNamespace(namespaceName);
        getOrAddNamespaceMembership(user, namespace);

        ExtensionJson extJson;
        var description = "MirrorExtensionVersion";
        var accessTokenValue = data.getOrAddAccessTokenValue(user, description);
        try(var input = new ByteArrayInputStream(vsixPackage)) {
            extJson = local.publish(input, accessTokenValue);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        data.updateTimestamps(extJson.namespace, extJson.name, extJson.targetPlatform, extJson.version, extJson.timestamp);
        completed(context, logger);
    }

    private void getOrAddNamespaceMembership(UserData user, Namespace namespace) {
        var membership = repositories.findMembership(user, namespace);
        if (membership == null) {
            users.addNamespaceMember(namespace, user, NamespaceMembership.ROLE_CONTRIBUTOR);
        }
    }
}
