/*
 * This file is generated by jOOQ.
 */
package org.eclipse.openvsx.jooq.tables;


import java.util.Arrays;
import java.util.List;

import org.eclipse.openvsx.jooq.Keys;
import org.eclipse.openvsx.jooq.Public;
import org.eclipse.openvsx.jooq.tables.records.QrtzSchedulerStateRecord;
import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Row4;
import org.jooq.Schema;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.TableOptions;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class QrtzSchedulerState extends TableImpl<QrtzSchedulerStateRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>public.qrtz_scheduler_state</code>
     */
    public static final QrtzSchedulerState QRTZ_SCHEDULER_STATE = new QrtzSchedulerState();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<QrtzSchedulerStateRecord> getRecordType() {
        return QrtzSchedulerStateRecord.class;
    }

    /**
     * The column <code>public.qrtz_scheduler_state.sched_name</code>.
     */
    public final TableField<QrtzSchedulerStateRecord, String> SCHED_NAME = createField(DSL.name("sched_name"), SQLDataType.VARCHAR(120).nullable(false), this, "");

    /**
     * The column <code>public.qrtz_scheduler_state.instance_name</code>.
     */
    public final TableField<QrtzSchedulerStateRecord, String> INSTANCE_NAME = createField(DSL.name("instance_name"), SQLDataType.VARCHAR(200).nullable(false), this, "");

    /**
     * The column <code>public.qrtz_scheduler_state.last_checkin_time</code>.
     */
    public final TableField<QrtzSchedulerStateRecord, Long> LAST_CHECKIN_TIME = createField(DSL.name("last_checkin_time"), SQLDataType.BIGINT.nullable(false), this, "");

    /**
     * The column <code>public.qrtz_scheduler_state.checkin_interval</code>.
     */
    public final TableField<QrtzSchedulerStateRecord, Long> CHECKIN_INTERVAL = createField(DSL.name("checkin_interval"), SQLDataType.BIGINT.nullable(false), this, "");

    private QrtzSchedulerState(Name alias, Table<QrtzSchedulerStateRecord> aliased) {
        this(alias, aliased, null);
    }

    private QrtzSchedulerState(Name alias, Table<QrtzSchedulerStateRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>public.qrtz_scheduler_state</code> table reference
     */
    public QrtzSchedulerState(String alias) {
        this(DSL.name(alias), QRTZ_SCHEDULER_STATE);
    }

    /**
     * Create an aliased <code>public.qrtz_scheduler_state</code> table reference
     */
    public QrtzSchedulerState(Name alias) {
        this(alias, QRTZ_SCHEDULER_STATE);
    }

    /**
     * Create a <code>public.qrtz_scheduler_state</code> table reference
     */
    public QrtzSchedulerState() {
        this(DSL.name("qrtz_scheduler_state"), null);
    }

    public <O extends Record> QrtzSchedulerState(Table<O> child, ForeignKey<O, QrtzSchedulerStateRecord> key) {
        super(child, key, QRTZ_SCHEDULER_STATE);
    }

    @Override
    public Schema getSchema() {
        return Public.PUBLIC;
    }

    @Override
    public UniqueKey<QrtzSchedulerStateRecord> getPrimaryKey() {
        return Keys.QRTZ_SCHEDULER_STATE_PKEY;
    }

    @Override
    public List<UniqueKey<QrtzSchedulerStateRecord>> getKeys() {
        return Arrays.<UniqueKey<QrtzSchedulerStateRecord>>asList(Keys.QRTZ_SCHEDULER_STATE_PKEY);
    }

    @Override
    public QrtzSchedulerState as(String alias) {
        return new QrtzSchedulerState(DSL.name(alias), this);
    }

    @Override
    public QrtzSchedulerState as(Name alias) {
        return new QrtzSchedulerState(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public QrtzSchedulerState rename(String name) {
        return new QrtzSchedulerState(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public QrtzSchedulerState rename(Name name) {
        return new QrtzSchedulerState(name, null);
    }

    // -------------------------------------------------------------------------
    // Row4 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row4<String, String, Long, Long> fieldsRow() {
        return (Row4) super.fieldsRow();
    }
}