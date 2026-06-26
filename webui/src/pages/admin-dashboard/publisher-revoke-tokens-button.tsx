/********************************************************************************
 * Copyright (c) 2025 Precies. Software OU and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import { FunctionComponent, useContext, useState } from 'react';
import { Box, Button, Dialog, DialogActions, DialogContent, DialogContentText, DialogTitle } from '@mui/material';
import { ButtonWithProgress } from '../../components/button-with-progress';
import { PublisherInfo } from '../../extension-registry-types';
import { MainContext } from '../../context';
import { UpdateContext } from './publisher-admin';
import { useRevokeAccessTokens } from './use-publisher-admin';

export const PublisherRevokeTokensButton: FunctionComponent<PublisherRevokeTokensButtonProps> = props => {
    const { handleError } = useContext(MainContext);
    const updateContext = useContext(UpdateContext);
    const { mutateAsync: revokeTokens, isPending: working } = useRevokeAccessTokens();

    const [dialogOpen, setDialogOpen] = useState(false);

    const { user } = props.publisherInfo;
    const tokenCount = props.publisherInfo.activeAccessTokenNum;

    const doRevoke = async () => {
        try {
            await revokeTokens({ provider: user.provider as string, login: user.loginName });
            updateContext.handleUpdate();
            setDialogOpen(false);
        } catch (err) {
            handleError(err);
        }
    };

    return (
        <>
            <ButtonWithProgress working={false} color='error' onClick={() => setDialogOpen(true)}>
                Revoke Access Tokens
                <Box component='span' sx={{ ml: 0.75, opacity: 0.6 }}>
                    {tokenCount}
                </Box>
            </ButtonWithProgress>
            <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)}>
                <DialogTitle>Revoke Access Tokens</DialogTitle>
                <DialogContent>
                    <DialogContentText>
                        Deactivate {tokenCount} active access token{tokenCount === 1 ? '' : 's'} of {user.loginName}?
                        This cannot be undone.
                    </DialogContentText>
                </DialogContent>
                <DialogActions>
                    <Button variant='contained' color='primary' onClick={() => setDialogOpen(false)}>
                        Cancel
                    </Button>
                    <ButtonWithProgress autoFocus sx={{ ml: 1 }} color='error' working={working} onClick={doRevoke}>
                        Revoke Tokens
                    </ButtonWithProgress>
                </DialogActions>
            </Dialog>
        </>
    );
};

export interface PublisherRevokeTokensButtonProps {
    publisherInfo: PublisherInfo;
}
