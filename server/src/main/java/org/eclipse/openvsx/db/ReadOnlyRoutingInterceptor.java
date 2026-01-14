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

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * AOP interceptor that automatically routes database operations to the appropriate
 * datasource based on the @Transactional annotation.
 * 
 * - Methods annotated with @Transactional(readOnly=true) are routed to replica database
 * - All other operations (writes or readOnly=false) are routed to primary database
 * 
 * This interceptor runs before the transaction is started (Order = 0) to ensure
 * the correct datasource is selected before any database operations begin.
 */
@Aspect
@Component
@Order(0) // Execute before transaction starts
public class ReadOnlyRoutingInterceptor {
    
    protected final Logger logger = LoggerFactory.getLogger(ReadOnlyRoutingInterceptor.class);
    
    /**
     * Intercept all methods annotated with @Transactional and route to appropriate datasource.
     * 
     * @param joinPoint the method execution join point
     * @param transactional the transactional annotation
     * @return the result of the intercepted method
     * @throws Throwable if the intercepted method throws an exception
     */
    @Around("@annotation(transactional)")
    public Object routeDataSource(ProceedingJoinPoint joinPoint, Transactional transactional) throws Throwable {
        DataSourceType dataSourceType = transactional.readOnly() 
            ? DataSourceType.REPLICA 
            : DataSourceType.PRIMARY;
        
        // Set the datasource type in thread-local context
        DataSourceContextHolder.setDataSourceType(dataSourceType);
        
        if (logger.isDebugEnabled()) {
            logger.debug("Routing {} to {} datasource", 
                joinPoint.getSignature().toShortString(), 
                dataSourceType);
        }
        
        try {
            return joinPoint.proceed();
        } finally {
            // Always clear the context to prevent memory leaks
            DataSourceContextHolder.clearDataSourceType();
        }
    }
}
