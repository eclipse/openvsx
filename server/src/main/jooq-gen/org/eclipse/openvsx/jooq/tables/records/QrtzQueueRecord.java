/*
 * This file is generated by jOOQ.
 */
package org.eclipse.openvsx.jooq.tables.records;


import org.eclipse.openvsx.jooq.tables.QrtzQueue;
import org.jooq.Field;
import org.jooq.Record4;
import org.jooq.Record6;
import org.jooq.Row6;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class QrtzQueueRecord extends UpdatableRecordImpl<QrtzQueueRecord> implements Record6<String, String, String, String, Integer, String> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>public.qrtz_queue.sched_name</code>.
     */
    public void setSchedName(String value) {
        set(0, value);
    }

    /**
     * Getter for <code>public.qrtz_queue.sched_name</code>.
     */
    public String getSchedName() {
        return (String) get(0);
    }

    /**
     * Setter for <code>public.qrtz_queue.queue_name</code>.
     */
    public void setQueueName(String value) {
        set(1, value);
    }

    /**
     * Getter for <code>public.qrtz_queue.queue_name</code>.
     */
    public String getQueueName() {
        return (String) get(1);
    }

    /**
     * Setter for <code>public.qrtz_queue.job_name</code>.
     */
    public void setJobName(String value) {
        set(2, value);
    }

    /**
     * Getter for <code>public.qrtz_queue.job_name</code>.
     */
    public String getJobName() {
        return (String) get(2);
    }

    /**
     * Setter for <code>public.qrtz_queue.job_group</code>.
     */
    public void setJobGroup(String value) {
        set(3, value);
    }

    /**
     * Getter for <code>public.qrtz_queue.job_group</code>.
     */
    public String getJobGroup() {
        return (String) get(3);
    }

    /**
     * Setter for <code>public.qrtz_queue.priority</code>.
     */
    public void setPriority(Integer value) {
        set(4, value);
    }

    /**
     * Getter for <code>public.qrtz_queue.priority</code>.
     */
    public Integer getPriority() {
        return (Integer) get(4);
    }

    /**
     * Setter for <code>public.qrtz_queue.state</code>.
     */
    public void setState(String value) {
        set(5, value);
    }

    /**
     * Getter for <code>public.qrtz_queue.state</code>.
     */
    public String getState() {
        return (String) get(5);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record4<String, String, String, String> key() {
        return (Record4) super.key();
    }

    // -------------------------------------------------------------------------
    // Record6 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row6<String, String, String, String, Integer, String> fieldsRow() {
        return (Row6) super.fieldsRow();
    }

    @Override
    public Row6<String, String, String, String, Integer, String> valuesRow() {
        return (Row6) super.valuesRow();
    }

    @Override
    public Field<String> field1() {
        return QrtzQueue.QRTZ_QUEUE.SCHED_NAME;
    }

    @Override
    public Field<String> field2() {
        return QrtzQueue.QRTZ_QUEUE.QUEUE_NAME;
    }

    @Override
    public Field<String> field3() {
        return QrtzQueue.QRTZ_QUEUE.JOB_NAME;
    }

    @Override
    public Field<String> field4() {
        return QrtzQueue.QRTZ_QUEUE.JOB_GROUP;
    }

    @Override
    public Field<Integer> field5() {
        return QrtzQueue.QRTZ_QUEUE.PRIORITY;
    }

    @Override
    public Field<String> field6() {
        return QrtzQueue.QRTZ_QUEUE.STATE;
    }

    @Override
    public String component1() {
        return getSchedName();
    }

    @Override
    public String component2() {
        return getQueueName();
    }

    @Override
    public String component3() {
        return getJobName();
    }

    @Override
    public String component4() {
        return getJobGroup();
    }

    @Override
    public Integer component5() {
        return getPriority();
    }

    @Override
    public String component6() {
        return getState();
    }

    @Override
    public String value1() {
        return getSchedName();
    }

    @Override
    public String value2() {
        return getQueueName();
    }

    @Override
    public String value3() {
        return getJobName();
    }

    @Override
    public String value4() {
        return getJobGroup();
    }

    @Override
    public Integer value5() {
        return getPriority();
    }

    @Override
    public String value6() {
        return getState();
    }

    @Override
    public QrtzQueueRecord value1(String value) {
        setSchedName(value);
        return this;
    }

    @Override
    public QrtzQueueRecord value2(String value) {
        setQueueName(value);
        return this;
    }

    @Override
    public QrtzQueueRecord value3(String value) {
        setJobName(value);
        return this;
    }

    @Override
    public QrtzQueueRecord value4(String value) {
        setJobGroup(value);
        return this;
    }

    @Override
    public QrtzQueueRecord value5(Integer value) {
        setPriority(value);
        return this;
    }

    @Override
    public QrtzQueueRecord value6(String value) {
        setState(value);
        return this;
    }

    @Override
    public QrtzQueueRecord values(String value1, String value2, String value3, String value4, Integer value5, String value6) {
        value1(value1);
        value2(value2);
        value3(value3);
        value4(value4);
        value5(value5);
        value6(value6);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached QrtzQueueRecord
     */
    public QrtzQueueRecord() {
        super(QrtzQueue.QRTZ_QUEUE);
    }

    /**
     * Create a detached, initialised QrtzQueueRecord
     */
    public QrtzQueueRecord(String schedName, String queueName, String jobName, String jobGroup, Integer priority, String state) {
        super(QrtzQueue.QRTZ_QUEUE);

        setSchedName(schedName);
        setQueueName(queueName);
        setJobName(jobName);
        setJobGroup(jobGroup);
        setPriority(priority);
        setState(state);
    }
}