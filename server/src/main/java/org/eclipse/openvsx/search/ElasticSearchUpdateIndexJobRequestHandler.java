/** ******************************************************************************
 * Copyright (c) 2023 Precies. Software Ltd and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.search;

import org.eclipse.openvsx.migration.HandlerJobRequest;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ElasticSearchUpdateIndexJobRequestHandler implements JobRequestHandler<HandlerJobRequest> {

    @Autowired
    ElasticSearchService search;

    @Override
    @Job(name = "Task scheduled once per day to soft-update the search index.", retries = 0)
    public void run(HandlerJobRequest jobRequest) throws Exception {
        search.updateSearchIndex();
    }
}
