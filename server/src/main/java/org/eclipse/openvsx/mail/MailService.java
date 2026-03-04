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

import org.eclipse.openvsx.entities.PersonalAccessToken;
import org.eclipse.openvsx.entities.UserData;
import org.jobrunr.scheduling.JobRequestScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.util.Map;


@Component
public class MailService {
    private final Logger logger = LoggerFactory.getLogger(MailService.class);

    private final boolean disabled;
    private final JobRequestScheduler scheduler;

    @Value("${ovsx.mail.revoked-access-tokens.subject:}")
    String revokedAccessTokensSubject;

    @Value("${ovsx.mail.revoked-access-tokens.template:}")
    String revokedAccessTokensTemplate;

    @Value("${ovsx.mail.access-token-expiry.subject:}")
    String accessTokenExpirySubject;

    @Value("${ovsx.mail.access-token-expiry.template:}")
    String accessTokenExpiryTemplate;

    public MailService(@Autowired(required = false) JavaMailSender sender, JobRequestScheduler scheduler) {
        this.disabled = sender == null;
        this.scheduler = scheduler;
    }

    public void scheduleAccessTokenExpiryNotification(PersonalAccessToken token) {
        if (disabled) {
            return;
        }

        var user = token.getUser();
        var email = user.getEmail();

        if (email == null) {
            logger.warn("Could not send mail to user '{}' due to expired access token notification: email not known", user.getLoginName());
            return;
        }

        // the fullName might be null
        var name = user.getFullName() == null ? user.getLoginName() : user.getFullName();
        // the token description might be null as well
        var tokenName = token.getDescription() != null ? token.getDescription() : "";

        var variables = Map.<String, Object>of(
                "name", name,
                "tokenName", tokenName,
                "expiryDate", token.getExpiresTimestamp()
        );
        var jobRequest = new SendMailJobRequest(
                email,
                accessTokenExpirySubject,
                accessTokenExpiryTemplate,
                variables
        );

        scheduler.enqueue(jobRequest);
        logger.debug("Scheduled notification email for expiring token {} to {}", tokenName, email);
    }

    public void scheduleRevokedAccessTokensMail(UserData user) {
        if (disabled) {
            return;
        }

        if (user.getEmail() == null) {
            logger.warn("Could not send mail to user '{}' due to revoked access token: email not known", user.getLoginName());
            return;
        }

        // the fullName might be null
        var name = user.getFullName() == null ? user.getLoginName() : user.getFullName();
        var variables = Map.<String, Object>of("name", name);
        var jobRequest = new SendMailJobRequest(
                user.getEmail(),
                revokedAccessTokensSubject,
                revokedAccessTokensTemplate,
                variables
        );

        scheduler.enqueue(jobRequest);
    }
}
