/** ******************************************************************************
 * Copyright (c) 2024 Precies. Software Ltd and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.storage;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.openvsx.admin.AdminService;
import org.eclipse.openvsx.entities.ExtensionVersion;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LimitStoredVersionsJobRequestHandler implements JobRequestHandler<LimitStoredVersionsJobRequest> {

    protected final Logger logger = LoggerFactory.getLogger(LimitStoredVersionsJobRequestHandler.class);

    @Autowired
    AdminService admins;

    @Autowired
    RepositoryService repositories;

    @Autowired
    StoredVersionsLimiter service;

    @Override
    public void run(LimitStoredVersionsJobRequest jobRequest) throws Exception {
        if(!service.isEnabled()) {
            return;
        }

        var extVersions = findExtensionVersions(jobRequest);
        if(extVersions.isEmpty()) {
            return;
        }

        var user = admins.createSystemUser(service.getUserName());
        for(var extVersion : extVersions) {
            var extension = extVersion.getExtension();
            var namespace = extension.getNamespace();
            var result = admins.deleteExtension(namespace.getName(), extension.getName(), extVersion.getTargetPlatform(), extVersion.getVersion(), user);
            if(StringUtils.isNotEmpty(result.error)) {
                logger.error(result.error);
            }
            if(StringUtils.isNotEmpty(result.warning)) {
                logger.warn(result.warning);
            }
            if(StringUtils.isNotEmpty(result.success)) {
                logger.info(result.success);
            }
        }
    }

    private List<ExtensionVersion> findExtensionVersions(LimitStoredVersionsJobRequest jobRequest) {
        var limit = service.getLimit();
        if(jobRequest.isFindAll()) {
            return repositories.findAllExcessExtensionVersions(limit);
        } else {
            var namespace = jobRequest.getNamespace();
            var extension = jobRequest.getExtension();
            return repositories.findExcessExtensionVersions(namespace, extension, limit);
        }
    }
}
