/** ******************************************************************************
 * Copyright (c) 2025 Precies. Software OU and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.mail;

import org.eclipse.openvsx.entities.UserData;
import org.jobrunr.scheduling.JobRequestScheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class MailService {

    private final boolean disabled;
    private final JobRequestScheduler scheduler;

    @Value("${ovsx.mail.revoked-access-tokens.subject:}")
    String revokedAccessTokensSubject;

    @Value("${ovsx.mail.revoked-access-tokens.template:}")
    String revokedAccessTokensTemplate;

    public MailService(@Autowired(required = false) JavaMailSender sender, JobRequestScheduler scheduler) {
        this.disabled = sender == null;
        this.scheduler = scheduler;
    }

    public void scheduleRevokedAccessTokensMail(UserData user) {
        if(disabled) {
            return;
        }

        var variables = Map.<String, Object>of("name", user.getFullName());
        var jobRequest = new SendMailJobRequest(
                user.getEmail(),
                revokedAccessTokensSubject,
                revokedAccessTokensTemplate,
                variables
        );

        scheduler.enqueue(jobRequest);
    }
}
