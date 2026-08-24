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

// The type parameter is deliberately bounded by the non-specific JobRequestHandler<?> rather than
// JobRequestHandler<MigrationJobRequest<?>>: that self-referential bound (T's bound mentions this
// class parametrized with itself again) sends Jackson's generic type resolution into infinite
// recursion -- a StackOverflowError wrapped as a DatabindException -- while serializing the
// `handler` field for JobRunr's queue storage. See MigrationJobRequestTest for a standalone repro.
public class MigrationJobRequest<T extends JobRequestHandler<?>> implements JobRequest {

    private Class<T> handler;
    private long entityId;

    public MigrationJobRequest() {
    }

    public MigrationJobRequest(Class<T> handler, long entityId) {
        this.handler = handler;
        this.entityId = entityId;
    }

    @Override
    public Class<T> getJobRequestHandler() {
        return handler;
    }

    public void setJobRequestHandler(Class<T> handler) {
        this.handler = handler;
    }

    public long getEntityId() {
        return entityId;
    }

    public void setEntityId(long entityId) {
        this.entityId = entityId;
    }
}
