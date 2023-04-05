/** ******************************************************************************
 * Copyright (c) 2023 Precies. Software Ltd and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.admin;

import org.eclipse.openvsx.storage.StorageUtilService;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RemoveFileJobRequestHandler implements JobRequestHandler<RemoveFileJobRequest> {

    @Autowired
    StorageUtilService storageUtil;

    @Override
    @Job(name = "Remove file in storage", retries = 10)
    public void run(RemoveFileJobRequest jobRequest) throws Exception {
        storageUtil.removeFile(jobRequest.getResource());
    }
}
