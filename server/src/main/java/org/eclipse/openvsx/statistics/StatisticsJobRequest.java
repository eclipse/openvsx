/** ******************************************************************************
 * Copyright (c) 2023 Precies. Software Ltd and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.statistics;

import org.jobrunr.jobs.lambdas.JobRequest;
import org.jobrunr.jobs.lambdas.JobRequestHandler;

public class StatisticsJobRequest<T extends JobRequestHandler<StatisticsJobRequest>> implements JobRequest {

    private Class<T> handler;
    private int year;
    private int month;

    public StatisticsJobRequest() {}

    public StatisticsJobRequest(Class<T> handler,int year, int month) {
        this.handler = handler;
        this.year = year;
        this.month = month;
    }

    public Class<T> getHandler() {
        return handler;
    }

    public void setHandler(Class<T> handler) {
        this.handler = handler;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    @Override
    public Class<T> getJobRequestHandler() {
        return this.handler;
    }
}