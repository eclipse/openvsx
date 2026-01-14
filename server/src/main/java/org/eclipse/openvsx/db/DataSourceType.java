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
 * Enum to identify the type of database connection (primary or replica).
 * Used for routing read/write queries to appropriate datasources.
 */
public enum DataSourceType {
    /**
     * Primary (write) database - handles all write operations and read operations
     * when no replica is available or when explicitly requested.
     */
    PRIMARY,
    
    /**
     * Replica (read-only) database - handles read-only operations for improved
     * horizontal scalability.
     */
    REPLICA
}
