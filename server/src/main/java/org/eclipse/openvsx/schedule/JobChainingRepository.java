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

import org.jooq.DSLContext;
import org.quartz.JobKey;

import static org.eclipse.openvsx.jooq.Tables.QRTZ_JOB_CHAINS;
import static org.quartz.impl.jdbcjobstore.Constants.*;

public class JobChainingRepository {

    private DSLContext dsl;

    public JobChainingRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public void insert(String schedulerName, String name, JobKey firstJob, JobKey nextJob) {
        dsl.insertInto(QRTZ_JOB_CHAINS)
                .columns(
                        QRTZ_JOB_CHAINS.SCHED_NAME,
                        QRTZ_JOB_CHAINS.CHAIN_NAME,
                        QRTZ_JOB_CHAINS.JOB1_NAME,
                        QRTZ_JOB_CHAINS.JOB1_GROUP,
                        QRTZ_JOB_CHAINS.JOB2_NAME,
                        QRTZ_JOB_CHAINS.JOB2_GROUP,
                        QRTZ_JOB_CHAINS.STATE
                ).values(
                        schedulerName,
                        name,
                        firstJob.getName(),
                        firstJob.getGroup(),
                        nextJob.getName(),
                        nextJob.getGroup(),
                        STATE_WAITING
                ).execute();
    }

    public JobKey findNext(String schedulerName, String name, JobKey firstJob) {
        var record = dsl.select(QRTZ_JOB_CHAINS.JOB2_NAME, QRTZ_JOB_CHAINS.JOB2_GROUP)
                .from(QRTZ_JOB_CHAINS)
                .where(QRTZ_JOB_CHAINS.SCHED_NAME.eq(schedulerName))
                .and(QRTZ_JOB_CHAINS.CHAIN_NAME.eq(name))
                .and(QRTZ_JOB_CHAINS.JOB1_NAME.eq(firstJob.getName()))
                .and(QRTZ_JOB_CHAINS.JOB1_GROUP.eq(firstJob.getGroup()))
                .and(QRTZ_JOB_CHAINS.STATE.eq(STATE_WAITING))
                .fetchOne();

        return record != null
                ? new JobKey(record.get(QRTZ_JOB_CHAINS.JOB2_NAME), record.get(QRTZ_JOB_CHAINS.JOB2_GROUP))
                : null;
    }

    public void complete(String schedulerName, String name, JobKey firstJob, JobKey nextJob) {
        dsl.update(QRTZ_JOB_CHAINS)
                .set(QRTZ_JOB_CHAINS.STATE, STATE_COMPLETE)
                .where(QRTZ_JOB_CHAINS.SCHED_NAME.eq(schedulerName))
                .and(QRTZ_JOB_CHAINS.CHAIN_NAME.eq(name))
                .and(QRTZ_JOB_CHAINS.JOB1_NAME.eq(firstJob.getName()))
                .and(QRTZ_JOB_CHAINS.JOB1_GROUP.eq(firstJob.getGroup()))
                .and(QRTZ_JOB_CHAINS.JOB2_NAME.eq(nextJob.getName()))
                .and(QRTZ_JOB_CHAINS.JOB2_GROUP.eq(nextJob.getGroup()))
                .execute();
    }

    public boolean exists(String schedulerName, String name, JobKey firstJob, JobKey nextJob) {
        var record = dsl.selectOne()
                .from(QRTZ_JOB_CHAINS)
                .where(QRTZ_JOB_CHAINS.SCHED_NAME.eq(schedulerName))
                .and(QRTZ_JOB_CHAINS.CHAIN_NAME.eq(name))
                .and(QRTZ_JOB_CHAINS.JOB1_NAME.eq(firstJob.getName()))
                .and(QRTZ_JOB_CHAINS.JOB1_GROUP.eq(firstJob.getGroup()))
                .and(QRTZ_JOB_CHAINS.JOB2_NAME.eq(nextJob.getName()))
                .and(QRTZ_JOB_CHAINS.JOB2_GROUP.eq(nextJob.getGroup()))
                .fetchOne();

        return record != null;
    }
}
