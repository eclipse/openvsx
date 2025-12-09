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

import io.micrometer.common.util.StringUtils;
import org.eclipse.openvsx.admin.RemoveFileJobRequest;
import org.eclipse.openvsx.entities.ExtensionVersion;
import org.eclipse.openvsx.entities.FileResource;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.jobrunr.scheduling.JobRequestScheduler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;

import static org.eclipse.openvsx.entities.FileResource.DOWNLOAD_SIG;
import static org.eclipse.openvsx.entities.SignatureKeyPair.*;

@Component
@ConditionalOnProperty(value = "ovsx.data.mirror.enabled", havingValue = "false", matchIfMissing = true)
public class GenerateKeyPairJobRequestHandler implements JobRequestHandler<HandlerJobRequest<?>> {

    private final RepositoryService repositories;
    private final JobRequestScheduler scheduler;
    private final GenerateKeyPairJobService service;

    @Value("${ovsx.integrity.key-pair:}")
    String keyPairMode;

    public GenerateKeyPairJobRequestHandler(
            RepositoryService repositories,
            JobRequestScheduler scheduler,
            GenerateKeyPairJobService service
    ) {
        this.repositories = repositories;
        this.scheduler = scheduler;
        this.service = service;
    }

    @Override
    public void run(HandlerJobRequest<?> jobRequest) throws Exception {
        switch (keyPairMode) {
            case KEYPAIR_MODE_CREATE:
                var activeKeyPair = repositories.findActiveKeyPair();
                if(activeKeyPair == null) {
                    renewKeyPair();
                } else {
                    repositories.findVersionsWithout(activeKeyPair).forEach(this::enqueueCreateSignatureJob);
                }
                break;
            case KEYPAIR_MODE_RENEW:
                renewKeyPair();
                break;
            case KEYPAIR_MODE_DELETE:
                deleteKeyPairs();
                break;
            default:
                if(StringUtils.isNotEmpty(keyPairMode)) {
                    var values = String.join(",", KEYPAIR_MODE_CREATE, KEYPAIR_MODE_RENEW, KEYPAIR_MODE_DELETE);
                    throw new IllegalArgumentException("Unsupported value '" + keyPairMode + "' for 'ovsx.integrity.key-pair' defined. Supported values are: " + values);
                }
        }
    }

    private void renewKeyPair() throws IOException {
        var keyPair = service.generateKeyPair();
        service.updateKeyPair(keyPair);
        repositories.findVersions().forEach(this::enqueueCreateSignatureJob);
    }

    private void deleteKeyPairs() {
        repositories.findFilesByType(DOWNLOAD_SIG).forEach(this::enqueueDeleteSignatureJob);
        service.deleteSignaturesAndKeyPairs();
    }

    private void enqueueCreateSignatureJob(ExtensionVersion extVersion) {
        var handler = ExtensionVersionSignatureJobRequestHandler.class;
        scheduler.enqueue(new MigrationJobRequest<>(handler, extVersion.getId()));
    }

    private void enqueueDeleteSignatureJob(FileResource resource) {
        scheduler.enqueue(new RemoveFileJobRequest(resource));
    }
}
