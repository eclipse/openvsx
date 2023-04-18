/** ******************************************************************************
 * Copyright (c) 2023 Precies. Software Ltd and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.migration;

import org.eclipse.openvsx.admin.RemoveFileJobRequest;
import org.eclipse.openvsx.entities.ExtensionVersion;
import org.eclipse.openvsx.entities.FileResource;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.util.TimeUtil;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.jobrunr.scheduling.JobRequestScheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.util.Streamable;
import org.springframework.stereotype.Component;

import static org.eclipse.openvsx.entities.FileResource.DOWNLOAD_SIG;
import static org.eclipse.openvsx.entities.FileResource.STORAGE_DB;
import static org.eclipse.openvsx.entities.SignatureKeyPair.*;

@Component
@ConditionalOnProperty(value = "ovsx.data.mirror.enabled", havingValue = "false", matchIfMissing = true)
public class GenerateKeyPairJobRequestHandler implements JobRequestHandler<HandlerJobRequest<?>> {

    @Autowired
    RepositoryService repositories;

    @Autowired
    JobRequestScheduler scheduler;

    @Autowired
    GenerateKeyPairJobService service;

    @Value("${ovsx.integrity.key-pair:}")
    String keyPairMode;

    @Override
    public void run(HandlerJobRequest<?> jobRequest) throws Exception {
        switch (keyPairMode) {
            case KEYPAIR_MODE_CREATE:
                createKeyPair();
                break;
            case KEYPAIR_MODE_RENEW:
                renewKeyPair();
                break;
            case KEYPAIR_MODE_DELETE:
                deleteKeyPairs();
                break;
        }
    }

    private void createKeyPair() {
        var activeKeyPair = repositories.findActiveKeyPair();
        Streamable<ExtensionVersion> extVersions;
        if(activeKeyPair == null) {
            service.generateKeyPair();
            extVersions = repositories.findVersions();
        } else {
            extVersions = repositories.findVersionsWithout(activeKeyPair);
        }

        extVersions.forEach(this::enqueueCreateSignatureJob);
    }

    private void renewKeyPair() {
        service.renewKeyPair();
        repositories.findVersions().forEach(this::enqueueCreateSignatureJob);
    }

    private void deleteKeyPairs() {
        repositories.findFilesByType(DOWNLOAD_SIG).forEach(this::enqueueDeleteSignatureJob);
        service.deleteSignaturesAndKeyPairs();
    }

    private void enqueueCreateSignatureJob(ExtensionVersion extVersion) {
        var handler = ExtensionVersionSignatureJobRequestHandler.class;
        var jobRequest = new MigrationJobRequest<>(handler, extVersion.getId());
        scheduler.schedule(TimeUtil.getCurrentUTC().plusSeconds(30), jobRequest);
    }

    private void enqueueDeleteSignatureJob(FileResource resource) {
        if(!resource.getStorageType().equals(STORAGE_DB)) {
            scheduler.schedule(TimeUtil.getCurrentUTC().plusSeconds(30), new RemoveFileJobRequest(resource));
        }
    }
}
