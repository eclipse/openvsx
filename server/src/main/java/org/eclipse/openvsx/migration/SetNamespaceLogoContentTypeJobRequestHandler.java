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

import org.apache.tika.Tika;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.util.AbstractMap;

@Component
@ConditionalOnProperty(value = "ovsx.data.mirror.enabled", havingValue = "false", matchIfMissing = true)
public class SetNamespaceLogoContentTypeJobRequestHandler implements JobRequestHandler<MigrationJobRequest> {

    protected final Logger logger = LoggerFactory.getLogger(SetNamespaceLogoContentTypeJobRequestHandler.class);

    @Autowired
    SetNamespaceLogoContentTypeJobService service;

    @Override
    @Job(name = "Set content type for namespace logo", retries = 3)
    public void run(MigrationJobRequest jobRequest) throws Exception {
        var namespace = service.getNamespace(jobRequest.getEntityId());
        logger.info("Set logo content type for: {}", namespace.getName());

        var logoBytes = service.getLogoContent(namespace);
        try(
                var logoFile = service.getNamespaceLogoFile(new AbstractMap.SimpleEntry<>(namespace, logoBytes));
                var in = Files.newInputStream(logoFile.getPath())
        ) {
            var tika = new Tika();
            var contentType = tika.detect(in, namespace.getLogoName());
            namespace.setLogoContentType(contentType);
            service.uploadNamespaceLogo(namespace, logoFile);
            service.updateNamespace(namespace);
        }
    }
}
