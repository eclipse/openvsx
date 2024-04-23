/** ******************************************************************************
 * Copyright (c) 2023 Precies. Software Ltd and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.adapter;

import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.springframework.stereotype.Component;

@Component
public class VSCodeIdNewExtensionJobRequestHandler implements JobRequestHandler<VSCodeIdNewExtensionJobRequest> {

    private final VSCodeIdUpdateService service;

    public VSCodeIdNewExtensionJobRequestHandler(VSCodeIdUpdateService service) {
        this.service = service;
    }

    @Override
    public void run(VSCodeIdNewExtensionJobRequest jobRequest) throws Exception {
        var namespaceName = jobRequest.getNamespace();
        var extensionName = jobRequest.getExtension();
        service.update(namespaceName, extensionName);
    }
}
