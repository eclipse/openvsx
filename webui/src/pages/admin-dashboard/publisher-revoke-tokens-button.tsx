/********************************************************************************
 * Copyright (c) 2025 Precies. Software OU and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import { FunctionComponent, useContext } from 'react';
import { Box } from '@mui/material';
import { ButtonWithProgress } from '../../components/button-with-progress';
import { PublisherInfo } from '../../extension-registry-types';
import { MainContext } from '../../context';
import { UpdateContext } from './publisher-admin';
import { useRevokeAccessTokens } from './use-publisher-admin';

export const PublisherRevokeTokensButton: FunctionComponent<PublisherRevokeTokensButtonProps> = props => {
    const { handleError } = useContext(MainContext);
    const updateContext = useContext(UpdateContext);
    const { mutateAsync: revokeTokens, isPending: working } = useRevokeAccessTokens();

    const doRevoke = async () => {
        try {
            const user = props.publisherInfo.user;
            await revokeTokens({ provider: user.provider as string, login: user.loginName });
            updateContext.handleUpdate();
        } catch (err) {
            handleError(err);
        }
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