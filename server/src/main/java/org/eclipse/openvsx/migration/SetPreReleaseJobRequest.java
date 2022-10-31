/** ******************************************************************************
 * Copyright (c) 2022 Precies. Software Ltd and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.migration;

import org.jobrunr.jobs.lambdas.JobRequest;
import org.jobrunr.jobs.lambdas.JobRequestHandler;

public class SetPreReleaseJobRequest implements JobRequest {

    private long itemId;

    public SetPreReleaseJobRequest() { }

    public SetPreReleaseJobRequest(long itemId) {
        this.itemId = itemId;
    }

    @Override
    public Class<? extends JobRequestHandler> getJobRequestHandler() {
        return SetPreReleaseJobRequestHandler.class;
    }

    public long getItemId() {
        return itemId;
    }

    public void setItemId(long itemId) {
        this.itemId = itemId;
    }
}
