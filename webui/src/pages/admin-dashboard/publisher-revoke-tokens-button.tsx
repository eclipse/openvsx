/********************************************************************************
 * Copyright (c) 2025 Precies. Software OU and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import { FunctionComponent, useState, useContext, useEffect, useRef } from 'react';
import { Box } from '@mui/material';
import { ButtonWithProgress } from '../../components/button-with-progress';
import { PublisherInfo, isError } from '../../extension-registry-types';
import { MainContext } from '../../context';
import { UpdateContext } from './publisher-admin';

export const PublisherRevokeTokensButton: FunctionComponent<PublisherRevokeTokensButtonProps> = props => {
    const { service, handleError } = useContext(MainContext);
    const updateContext = useContext(UpdateContext);

    const [working, setWorking] = useState(false);
    const abortController = useRef<AbortController>(new AbortController());
    useEffect(() => {
        return () => {
            abortController.current.abort();
        };
    }, []);

    const doRevoke = async () => {
        try {
            setWorking(true);
            const user = props.publisherInfo.user;
            const result = await service.admin.revokeAccessTokens(abortController.current, user.provider as string, user.loginName);
            if (isError(result)) {
                throw result;
            }
            updateContext.handleUpdate();
        } catch (err) {
            handleError(err);
        } finally {
            setWorking(false);
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