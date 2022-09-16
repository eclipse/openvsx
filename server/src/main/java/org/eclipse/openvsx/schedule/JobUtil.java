/** ******************************************************************************
 * Copyright (c) 2022 Precies. Software Ltd and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.schedule;

import org.quartz.JobExecutionContext;
import org.slf4j.Logger;

public class JobUtil {

    public static class Groups {
        public static final String MIRROR = "Mirror";
        public static final String PUBLISH = "Publish";
    }

    public static class Retry {
        public static final String RETRIES = "retries";
        public static final String MAX_RETRIES = "maxRetries";
    }

    private JobUtil(){}

    public static void starting(JobExecutionContext context, Logger logger) {
        logStatus(logger, ">> Starting", context.getJobDetail().getKey().getName());
    }

    public static void completed(JobExecutionContext context, Logger logger) {
        logStatus(logger, "<< Completed", context.getJobDetail().getKey().getName());
    }

    private static void logStatus(Logger logger, String status, String jobName) {
        logger.info("{} {}", status, jobName);
    }
}
