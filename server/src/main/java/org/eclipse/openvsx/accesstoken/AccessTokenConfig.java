/******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/
package org.eclipse.openvsx.accesstoken;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class AccessTokenConfig {
    /**
     * The token prefix to use when generating a new access token.
     * <p>
     * Property: {@code ovsx.access-token.prefix}
     * Old Property: {@code ovsx.token-prefix}
     * Default: {@code ''}
     */
    @Value("#{'${ovsx.access-token.prefix:${ovsx.token-prefix:}}'}")
    String prefix;

    /**
     * The expiration period for personal access tokens.
     * <p>
     * If {@code 0} is provided, the access tokens do not expire.
     * <p>
     * Property: {@code ovsx.access-token.expiration}
     * Default: {@code P90D}, expires in 90 days
     */
    @Value("${ovsx.access-token.expiration:P90D}")
    Duration expiration;

    /**
     * The duration before the expiration of an access token
     * to send out a notification email to users.
     * <p>
     * Property: {@code ovsx.access-token.notification}
     * Default: {@code P7D}, 7 days prior to expiration
     */
    @Value("${ovsx.access-token.notification:P7D}")
    Duration notification;

    /**
     * The maximum number of expiring token notifications to handle
     * within one job execution.
     * <p>
     * Property: {@code ovsx.access-token.max-token-notifications}
     * Default: {@code 100}
     */
    @Value("${ovsx.access-token.max-token-notifications:100}")
    int maxTokenNotifications;

    /**
     * The cron schedule for the job to disable expired
     * access tokens.
     * <p>
     * Property: {@code ovsx.access-token.expiration-schedule}
     * Default: every 15 min
     */
    @Value("${ovsx.access-token.expiration-schedule:0 */15 * * * *}")
    String expirationSchedule;

    /**
     * The cron schedule for the job to send out notifications
     * for soon to be expired access tokens.
     * <p>
     * Property: {@code ovsx.access-token.notification-schedule}
     * Default: every 15 min
     */
    @Value("${ovsx.access-token.notification-schedule:30 */15 * * * *}")
    String notificationSchedule;
}
