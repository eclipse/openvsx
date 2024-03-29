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

import org.jobrunr.jobs.lambdas.JobRequest;
import org.jobrunr.jobs.lambdas.JobRequestHandler;

public class LimitStoredVersionsJobRequest implements JobRequest {

    private String namespace;
    private String extension;
    private boolean findAll;

    public LimitStoredVersionsJobRequest() {}

    public LimitStoredVersionsJobRequest(boolean findAll) {
        this.findAll = findAll;
    }

    public LimitStoredVersionsJobRequest(String namespace, String extension) {
        this.namespace = namespace;
        this.extension = extension;
    }

    @Override
    public Class<? extends JobRequestHandler> getJobRequestHandler() {
        return LimitStoredVersionsJobRequestHandler.class;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public boolean isFindAll() {
        return findAll;
    }

    public void setFindAll(boolean findAll) {
        this.findAll = findAll;
    }
}

