/*
 * This file is generated by jOOQ.
 */
package org.eclipse.openvsx.jooq.tables.records;


import org.eclipse.openvsx.jooq.tables.QrtzFiredTriggers;
import org.jooq.Field;
import org.jooq.Record13;
import org.jooq.Record2;
import org.jooq.Row13;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class QrtzFiredTriggersRecord extends UpdatableRecordImpl<QrtzFiredTriggersRecord> implements Record13<String, String, String, String, String, Long, Long, Integer, String, String, String, Boolean, Boolean> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>public.qrtz_fired_triggers.sched_name</code>.
     */
    public void setSchedName(String value) {
        set(0, value);
    }

    /**
     * Getter for <code>public.qrtz_fired_triggers.sched_name</code>.
     */
    public String getSchedName() {
        return (String) get(0);
    }

    /**
     * Setter for <code>public.qrtz_fired_triggers.entry_id</code>.
     */
    public void setEntryId(String value) {
        set(1, value);
    }

    /**
     * Getter for <code>public.qrtz_fired_triggers.entry_id</code>.
     */
    public String getEntryId() {
        return (String) get(1);
    }

    /**
     * Setter for <code>public.qrtz_fired_triggers.trigger_name</code>.
     */
    public void setTriggerName(String value) {
        set(2, value);
    }

    /**
     * Getter for <code>public.qrtz_fired_triggers.trigger_name</code>.
     */
    public String getTriggerName() {
        return (String) get(2);
    }

    /**
     * Setter for <code>public.qrtz_fired_triggers.trigger_group</code>.
     */
    public void setTriggerGroup(String value) {
        set(3, value);
    }

    /**
     * Getter for <code>public.qrtz_fired_triggers.trigger_group</code>.
     */
    public String getTriggerGroup() {
        return (String) get(3);
    }

    /**
     * Setter for <code>public.qrtz_fired_triggers.instance_name</code>.
     */
    public void setInstanceName(String value) {
        set(4, value);
    }

    /**
     * Getter for <code>public.qrtz_fired_triggers.instance_name</code>.
     */
    public String getInstanceName() {
        return (String) get(4);
    }

    /**
     * Setter for <code>public.qrtz_fired_triggers.fired_time</code>.
     */
    public void setFiredTime(Long value) {
        set(5, value);
    }

    /**
     * Getter for <code>public.qrtz_fired_triggers.fired_time</code>.
     */
    public Long getFiredTime() {
        return (Long) get(5);
    }

    /**
     * Setter for <code>public.qrtz_fired_triggers.sched_time</code>.
     */
    public void setSchedTime(Long value) {
        set(6, value);
    }

    /**
     * Getter for <code>public.qrtz_fired_triggers.sched_time</code>.
     */
    public Long getSchedTime() {
        return (Long) get(6);
    }

    /**
     * Setter for <code>public.qrtz_fired_triggers.priority</code>.
     */
    public void setPriority(Integer value) {
        set(7, value);
    }

    /**
     * Getter for <code>public.qrtz_fired_triggers.priority</code>.
     */
    public Integer getPriority() {
        return (Integer) get(7);
    }

    /**
     * Setter for <code>public.qrtz_fired_triggers.state</code>.
     */
    public void setState(String value) {
        set(8, value);
    }

    /**
     * Getter for <code>public.qrtz_fired_triggers.state</code>.
     */
    public String getState() {
        return (String) get(8);
    }

    /**
     * Setter for <code>public.qrtz_fired_triggers.job_name</code>.
     */
    public void setJobName(String value) {
        set(9, value);
    }

    /**
     * Getter for <code>public.qrtz_fired_triggers.job_name</code>.
     */
    public String getJobName() {
        return (String) get(9);
    }

    /**
     * Setter for <code>public.qrtz_fired_triggers.job_group</code>.
     */
    public void setJobGroup(String value) {
        set(10, value);
    }

    /**
     * Getter for <code>public.qrtz_fired_triggers.job_group</code>.
     */
    public String getJobGroup() {
        return (String) get(10);
    }

    /**
     * Setter for <code>public.qrtz_fired_triggers.is_nonconcurrent</code>.
     */
    public void setIsNonconcurrent(Boolean value) {
        set(11, value);
    }

    /**
     * Getter for <code>public.qrtz_fired_triggers.is_nonconcurrent</code>.
     */
    public Boolean getIsNonconcurrent() {
        return (Boolean) get(11);
    }

    /**
     * Setter for <code>public.qrtz_fired_triggers.requests_recovery</code>.
     */
    public void setRequestsRecovery(Boolean value) {
        set(12, value);
    }

    /**
     * Getter for <code>public.qrtz_fired_triggers.requests_recovery</code>.
     */
    public Boolean getRequestsRecovery() {
        return (Boolean) get(12);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record2<String, String> key() {
        return (Record2) super.key();
    }

    // -------------------------------------------------------------------------
    // Record13 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row13<String, String, String, String, String, Long, Long, Integer, String, String, String, Boolean, Boolean> fieldsRow() {
        return (Row13) super.fieldsRow();
    }

    @Override
    public Row13<String, String, String, String, String, Long, Long, Integer, String, String, String, Boolean, Boolean> valuesRow() {
        return (Row13) super.valuesRow();
    }

    @Override
    public Field<String> field1() {
        return QrtzFiredTriggers.QRTZ_FIRED_TRIGGERS.SCHED_NAME;
    }

    @Override
    public Field<String> field2() {
        return QrtzFiredTriggers.QRTZ_FIRED_TRIGGERS.ENTRY_ID;
    }

    @Override
    public Field<String> field3() {
        return QrtzFiredTriggers.QRTZ_FIRED_TRIGGERS.TRIGGER_NAME;
    }

    @Override
    public Field<String> field4() {
        return QrtzFiredTriggers.QRTZ_FIRED_TRIGGERS.TRIGGER_GROUP;
    }

    @Override
    public Field<String> field5() {
        return QrtzFiredTriggers.QRTZ_FIRED_TRIGGERS.INSTANCE_NAME;
    }

    @Override
    public Field<Long> field6() {
        return QrtzFiredTriggers.QRTZ_FIRED_TRIGGERS.FIRED_TIME;
    }

    @Override
    public Field<Long> field7() {
        return QrtzFiredTriggers.QRTZ_FIRED_TRIGGERS.SCHED_TIME;
    }

    @Override
    public Field<Integer> field8() {
        return QrtzFiredTriggers.QRTZ_FIRED_TRIGGERS.PRIORITY;
    }

    @Override
    public Field<String> field9() {
        return QrtzFiredTriggers.QRTZ_FIRED_TRIGGERS.STATE;
    }

    @Override
    public Field<String> field10() {
        return QrtzFiredTriggers.QRTZ_FIRED_TRIGGERS.JOB_NAME;
    }

    @Override
    public Field<String> field11() {
        return QrtzFiredTriggers.QRTZ_FIRED_TRIGGERS.JOB_GROUP;
    }

    @Override
    public Field<Boolean> field12() {
        return QrtzFiredTriggers.QRTZ_FIRED_TRIGGERS.IS_NONCONCURRENT;
    }

    @Override
    public Field<Boolean> field13() {
        return QrtzFiredTriggers.QRTZ_FIRED_TRIGGERS.REQUESTS_RECOVERY;
    }

    @Override
    public String component1() {
        return getSchedName();
    }

    @Override
    public String component2() {
        return getEntryId();
    }

    @Override
    public String component3() {
        return getTriggerName();
    }

    @Override
    public String component4() {
        return getTriggerGroup();
    }

    @Override
    public String component5() {
        return getInstanceName();
    }

    @Override
    public Long component6() {
        return getFiredTime();
    }

    @Override
    public Long component7() {
        return getSchedTime();
    }

    @Override
    public Integer component8() {
        return getPriority();
    }

    @Override
    public String component9() {
        return getState();
    }

    @Override
    public String component10() {
        return getJobName();
    }

    @Override
    public String component11() {
        return getJobGroup();
    }

    @Override
    public Boolean component12() {
        return getIsNonconcurrent();
    }

    @Override
    public Boolean component13() {
        return getRequestsRecovery();
    }

    @Override
    public String value1() {
        return getSchedName();
    }

    @Override
    public String value2() {
        return getEntryId();
    }

    @Override
    public String value3() {
        return getTriggerName();
    }

    @Override
    public String value4() {
        return getTriggerGroup();
    }

    @Override
    public String value5() {
        return getInstanceName();
    }

    @Override
    public Long value6() {
        return getFiredTime();
    }

    @Override
    public Long value7() {
        return getSchedTime();
    }

    @Override
    public Integer value8() {
        return getPriority();
    }

    @Override
    public String value9() {
        return getState();
    }

    @Override
    public String value10() {
        return getJobName();
    }

    @Override
    public String value11() {
        return getJobGroup();
    }

    @Override
    public Boolean value12() {
        return getIsNonconcurrent();
    }

    @Override
    public Boolean value13() {
        return getRequestsRecovery();
    }

    @Override
    public QrtzFiredTriggersRecord value1(String value) {
        setSchedName(value);
        return this;
    }

    @Override
    public QrtzFiredTriggersRecord value2(String value) {
        setEntryId(value);
        return this;
    }

    @Override
    public QrtzFiredTriggersRecord value3(String value) {
        setTriggerName(value);
        return this;
    }

    @Override
    public QrtzFiredTriggersRecord value4(String value) {
        setTriggerGroup(value);
        return this;
    }

    @Override
    public QrtzFiredTriggersRecord value5(String value) {
        setInstanceName(value);
        return this;
    }

    @Override
    public QrtzFiredTriggersRecord value6(Long value) {
        setFiredTime(value);
        return this;
    }

    @Override
    public QrtzFiredTriggersRecord value7(Long value) {
        setSchedTime(value);
        return this;
    }

    @Override
    public QrtzFiredTriggersRecord value8(Integer value) {
        setPriority(value);
        return this;
    }

    @Override
    public QrtzFiredTriggersRecord value9(String value) {
        setState(value);
        return this;
    }

    @Override
    public QrtzFiredTriggersRecord value10(String value) {
        setJobName(value);
        return this;
    }

    @Override
    public QrtzFiredTriggersRecord value11(String value) {
        setJobGroup(value);
        return this;
    }

    @Override
    public QrtzFiredTriggersRecord value12(Boolean value) {
        setIsNonconcurrent(value);
        return this;
    }

    @Override
    public QrtzFiredTriggersRecord value13(Boolean value) {
        setRequestsRecovery(value);
        return this;
    }

    @Override
    public QrtzFiredTriggersRecord values(String value1, String value2, String value3, String value4, String value5, Long value6, Long value7, Integer value8, String value9, String value10, String value11, Boolean value12, Boolean value13) {
        value1(value1);
        value2(value2);
        value3(value3);
        value4(value4);
        value5(value5);
        value6(value6);
        value7(value7);
        value8(value8);
        value9(value9);
        value10(value10);
        value11(value11);
        value12(value12);
        value13(value13);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached QrtzFiredTriggersRecord
     */
    public QrtzFiredTriggersRecord() {
        super(QrtzFiredTriggers.QRTZ_FIRED_TRIGGERS);
    }

    /**
     * Create a detached, initialised QrtzFiredTriggersRecord
     */
    public QrtzFiredTriggersRecord(String schedName, String entryId, String triggerName, String triggerGroup, String instanceName, Long firedTime, Long schedTime, Integer priority, String state, String jobName, String jobGroup, Boolean isNonconcurrent, Boolean requestsRecovery) {
        super(QrtzFiredTriggers.QRTZ_FIRED_TRIGGERS);

        setSchedName(schedName);
        setEntryId(entryId);
        setTriggerName(triggerName);
        setTriggerGroup(triggerGroup);
        setInstanceName(instanceName);
        setFiredTime(firedTime);
        setSchedTime(schedTime);
        setPriority(priority);
        setState(state);
        setJobName(jobName);
        setJobGroup(jobGroup);
        setIsNonconcurrent(isNonconcurrent);
        setRequestsRecovery(requestsRecovery);
    }
}