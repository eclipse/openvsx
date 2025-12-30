/********************************************************************************
 * Copyright (c) 2025 Precies. Software OU and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import React, { FunctionComponent, useState } from 'react';
import { Box } from '@mui/material';
import { ButtonWithProgress } from '../../components/button-with-progress';
import { PublisherInfo } from '../../extension-registry-types';
import { useAdminRevokeAccessTokensMutation } from '../../store/api';

export const PublisherRevokeTokensButton: FunctionComponent<PublisherRevokeTokensButtonProps> = props => {
    const [working, setWorking] = useState(false);
    const [revokeAccessTokens] = useAdminRevokeAccessTokensMutation();

    const doRevoke = async () => {
        setWorking(true);
        const user = props.publisherInfo.user;
        await revokeAccessTokens({ provider: user.provider as string, login: user.loginName });
        setWorking(false);
    };

    return <Box sx={{ mt: 2, display: 'flex', justifyContent: 'flex-end' }}>
        <ButtonWithProgress
            autoFocus
            working={working}
            onClick={doRevoke} >
            Revoke Access Tokens
        </ButtonWithProgress>
    </Box>;
};

export interface PublisherRevokeTokensButtonProps {
    publisherInfo: PublisherInfo;
}