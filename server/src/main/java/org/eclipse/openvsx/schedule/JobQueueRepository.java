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

import org.jooq.*;
import org.quartz.JobKey;

import java.util.List;

import static org.eclipse.openvsx.jooq.Tables.QRTZ_QUEUE;
import static org.quartz.impl.jdbcjobstore.Constants.STATE_WAITING;

public class JobQueueRepository {
    
    private DSLContext dsl;
    
    public JobQueueRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public void insert(String schedulerName, String name, JobKey jobKey, int priority) {
        dsl.insertInto(QRTZ_QUEUE)
                .columns(QRTZ_QUEUE.SCHED_NAME, QRTZ_QUEUE.QUEUE_NAME, QRTZ_QUEUE.JOB_NAME, QRTZ_QUEUE.JOB_GROUP, QRTZ_QUEUE.PRIORITY, QRTZ_QUEUE.STATE)
                .values(schedulerName, name, jobKey.getName(), jobKey.getGroup(), priority, STATE_WAITING)
                .execute();
    }

    public List<JobKey> findNext(String schedulerName, String name, int limit) {
        return dsl.select(QRTZ_QUEUE.JOB_NAME, QRTZ_QUEUE.JOB_GROUP)
                .from(QRTZ_QUEUE)
                .where(QRTZ_QUEUE.SCHED_NAME.eq(schedulerName))
                .and(QRTZ_QUEUE.QUEUE_NAME.eq(name))
                .and(QRTZ_QUEUE.STATE.eq(STATE_WAITING))
                .orderBy(QRTZ_QUEUE.PRIORITY)
                .limit(limit)
                .fetch()
                .map(record -> new JobKey(record.get(QRTZ_QUEUE.JOB_NAME), record.get(QRTZ_QUEUE.JOB_GROUP)));
    }

    public void updateState(String schedulerName, String name, JobKey jobKey, String state) {
        dsl.update(QRTZ_QUEUE)
                .set(QRTZ_QUEUE.STATE, state)
                .where(QRTZ_QUEUE.SCHED_NAME.eq(schedulerName))
                .and(QRTZ_QUEUE.QUEUE_NAME.eq(name))
                .and(QRTZ_QUEUE.JOB_NAME.eq(jobKey.getName()))
                .and(QRTZ_QUEUE.JOB_GROUP.eq(jobKey.getGroup()))
                .execute();
    }

    public boolean exists(String schedulerName, String name, JobKey jobKey) {
        var record = dsl.selectOne()
                .from(QRTZ_QUEUE)
                .where(QRTZ_QUEUE.SCHED_NAME.eq(schedulerName))
                .and(QRTZ_QUEUE.QUEUE_NAME.eq(name))
                .and(QRTZ_QUEUE.JOB_NAME.eq(jobKey.getName()))
                .and(QRTZ_QUEUE.JOB_GROUP.eq(jobKey.getGroup()))
                .fetchOne();

        return record != null;
    }
}
