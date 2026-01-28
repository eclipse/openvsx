/** ******************************************************************************
 * Copyright (c) 2025 Precies. Software OU and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.access_token;

import org.eclipse.openvsx.entities.PersonalAccessToken;
import org.eclipse.openvsx.mail.MailService;
import org.eclipse.openvsx.migration.HandlerJobRequest;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.util.TimeUtil;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.springframework.stereotype.Component;

@Component
public class NotifyPersonalAccessTokenExpiryJobRequestHandler implements JobRequestHandler<HandlerJobRequest> {

    private final MailService mail;
    private final RepositoryService repositories;

    public NotifyPersonalAccessTokenExpiryJobRequestHandler(MailService mail, RepositoryService repositories) {
        this.mail = mail;
        this.repositories = repositories;
    }

    @Override
    public void run(HandlerJobRequest handlerJobRequest) throws Exception {
        var timestamp = TimeUtil.getCurrentUTC().minusDays(PersonalAccessToken.EXPIRY_DAYS - 7);
        var tokens = repositories.findAccessTokensCreatedBefore(timestamp);
        tokens.forEach(mail::scheduleAccessTokenExpiryNotification);
    }
}
