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

import { FunctionComponent } from 'react';
import { Alert, AlertTitle, Box } from '@mui/material';
import { Link as RouteLink } from 'react-router';
import ArrowForwardIcon from '@mui/icons-material/ArrowForward';
import { UserSettingsRoutes } from '../user-settings-routes';
import { useTrustedPublishingStatus } from './use-trusted-publishers';

export const TrustedPublishingPromo: FunctionComponent = () => {
    // self-gating so no host page can show the promo while the feature is off
    const { data: trustedPublishingStatus } = useTrustedPublishingStatus();
    if (!trustedPublishingStatus?.enabled) {
        return null;
    }
    return (
        <Alert severity='info' sx={{ mt: 2 }}>
            <AlertTitle sx={{ fontWeight: 600 }}>Skip the token &mdash; publish with a trusted publisher</AlertTitle>
            You can now let a CI/CD workflow publish your extensions without ever creating a long-lived token. The
            workflow authenticates with a short-lived OIDC token at publish time, so there&rsquo;s no secret to leak,
            rotate, or accidentally revoke.
            <Box sx={{ mt: 1.5 }}>
                <RouteLink to={UserSettingsRoutes.TRUSTED_PUBLISHERS}>
                    Set up a trusted publisher
                    <ArrowForwardIcon sx={{ fontSize: '1.125rem' }} />
                </RouteLink>
            </Box>
        </Alert>
    );
};
