/********************************************************************************
 * Copyright (c) 2025 and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx.db;

/**
 * Thread-local context holder for determining which datasource (primary or replica)
 * should be used for the current transaction.
 * 
 * This is used by {@link RoutingDataSource} to route database queries to the
 * appropriate connection pool.
 */
public class DataSourceContextHolder {
    
    private static final ThreadLocal<DataSourceType> contextHolder = new ThreadLocal<>();
    
    /**
     * Set the datasource type for the current thread context.
     * 
     * @param dataSourceType the type of datasource to use (PRIMARY or REPLICA)
     */
    public static void setDataSourceType(DataSourceType dataSourceType) {
        contextHolder.set(dataSourceType);
    }
    
    /**
     * Get the current datasource type from thread context.
     * Defaults to PRIMARY if not explicitly set.
     * 
     * @return the datasource type (PRIMARY or REPLICA)
     */
    public static DataSourceType getDataSourceType() {
        DataSourceType type = contextHolder.get();
        return type != null ? type : DataSourceType.PRIMARY;
    }
    
    /**
     * Clear the datasource type from thread context.
     * Should be called after transaction completion to prevent memory leaks.
     */
    public static void clearDataSourceType() {
        contextHolder.remove();
    }
}
