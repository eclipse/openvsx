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

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * Custom DataSource router that extends Spring's AbstractRoutingDataSource.
 * This class routes database queries to either the primary (write) datasource
 * or replica (read) datasource based on the current thread context.
 * 
 * The routing decision is made by checking the {@link DataSourceContextHolder}
 * which is set by transaction interceptors based on the @Transactional annotation.
 */
public class RoutingDataSource extends AbstractRoutingDataSource {
    
    /**
     * Determine which datasource should be used for the current database operation.
     * 
     * @return the datasource type key (PRIMARY or REPLICA)
     */
    @Override
    protected Object determineCurrentLookupKey() {
        return DataSourceContextHolder.getDataSourceType();
    }
}
