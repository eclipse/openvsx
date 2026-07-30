/** ******************************************************************************
 * Copyright (c) 2025 Precies. Software OU and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.accesstoken;

import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import org.eclipse.openvsx.migration.HandlerJobRequest;
import org.eclipse.openvsx.settings.SettingsService;

@Component
public class ExpirePersonalAccessTokensHandler implements JobRequestHandler<HandlerJobRequest<?>> {

    private final Logger logger = LoggerFactory.getLogger(ExpirePersonalAccessTokensHandler.class);

    private final SettingsService settings;
    private final AccessTokenService tokens;

    public ExpirePersonalAccessTokensHandler(SettingsService settings, AccessTokenService tokens) {
        this.settings = settings;
        this.tokens = tokens;
    }

    @Override
    @Job(name = "Expire access tokens", retries = 0)
    public void run(HandlerJobRequest<?> handlerJobRequest) throws Exception {
        if (settings.isReadOnly()) {
            return;
        }

        var count = tokens.expireAccessTokens();
        if (count > 0) {
            logger.info("Expired {} personal access token(s)", count);
        }
    }
}
