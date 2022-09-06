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

import org.eclipse.openvsx.AdminService;
import org.eclipse.openvsx.IExtensionRegistry;
import org.eclipse.openvsx.LocalRegistryService;
import org.eclipse.openvsx.UserService;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.stream.Collectors;

@Component
public class DeleteExtensionJobRequestHandler implements JobRequestHandler<DeleteExtensionJobRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteExtensionJobRequestHandler.class);

    @Autowired
    AdminService admin;

    @Autowired
    RepositoryService repositories;

    @Value("${ovsx.data.mirror.enabled:false}")
    boolean enabled;

    @Value("${ovsx.data.mirror.user-name:}")
    String userName;

    @Override
    @Job(name="Delete Extension", retries=10)
    public void run(DeleteExtensionJobRequest jobRequest) throws Exception {
        if(!enabled) {
            return;
        }

        LOGGER.info(">> Starting DeleteExtensionJob for {}.{}", jobRequest.getNamespace(), jobRequest.getExtension());
        var mirrorUser = repositories.findUserByLoginName(null, userName);
        admin.deleteExtension(jobRequest.getNamespace(), jobRequest.getExtension(), mirrorUser);
        LOGGER.info("<< Completed DeleteExtensionJob for {}.{}", jobRequest.getNamespace(), jobRequest.getExtension());
    }
}
