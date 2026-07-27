/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import { FunctionComponent, useState, useContext } from 'react';
import {
    Button,
    Dialog,
    DialogTitle,
    DialogContent,
    DialogContentText,
    DialogActions,
    Typography,
    Link
} from '@mui/material';
import { createAbsoluteURL } from '../../utils';
import { ButtonWithProgress } from '../../components/button-with-progress';
import { PublisherInfo } from '../../extension-registry-types';
import { MainContext } from '../../context';
import { UpdateContext } from './publisher-admin';
import { useRevokePublisherContributions } from './use-publisher-admin';

export const PublisherRevokeContributionsButton: FunctionComponent<PublisherRevokeContributionsButtonProps> = props => {
    const { user, service, handleError } = useContext(MainContext);
    const updateContext = useContext(UpdateContext);
    const { mutateAsync: revokeContributions, isPending: working } = useRevokePublisherContributions();

    const [dialogOpen, setDialogOpen] = useState(false);

    if (
        props.publisherInfo.user.publisherAgreement &&
        !user?.additionalLogins?.find(login => login.provider === 'eclipse')
    ) {
        // If a publisher agreement is required, the admin must be logged in with Eclipse to revoke it
        return (
            <Link href={createAbsoluteURL([service.serverUrl, 'oauth2', 'authorization', 'eclipse'])}>
                <Button variant='contained' color='secondary'>
                    Log in with Eclipse
                </Button>
            </Link>
        );
    }

    const doRevoke = async () => {
        try {
            const user = props.publisherInfo.user;
            await revokeContributions({ provider: user.provider as string, login: user.loginName });
            updateContext.handleUpdate();
            setDialogOpen(false);
        } catch (err) {
            handleError(err);
        }
    };

    const tokenCount = props.publisherInfo.activeAccessTokenNum;
    const extensionCount = props.publisherInfo.extensions.filter(e => e.active).length;
    const hasAgreement = props.publisherInfo.user.publisherAgreement?.status !== 'none';

    return (
        <>
            <Button variant='outlined' color='error' onClick={() => setDialogOpen(true)}>
                Revoke publisher contributions
            </Button>
            <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)}>
                <DialogTitle>Revoke publisher contributions</DialogTitle>
                <DialogContent>
                    <DialogContentText component='div'>
                        <Typography component='div'>
                            {!tokenCount && !extensionCount && !hasAgreement ? (
                                <>
                                    Publisher {props.publisherInfo.user.loginName} currently has no contributions to
                                    revoke. Send the request anyway?
                                </>
                            ) : (
                                <>
                                    The following actions will be executed:
                                    <ul>
                                        {tokenCount > 0 ? (
                                            <li>
                                                Deactivate {tokenCount} access token{tokenCount > 1 ? 's' : ''} of{' '}
                                                {props.publisherInfo.user.loginName}
                                            </li>
                                        ) : null}
                                        {extensionCount > 0 ? (
                                            <li>
                                                Deactivate {extensionCount} published extension version
                                                {extensionCount > 1 ? 's' : ''}
                                            </li>
                                        ) : null}
                                        {hasAgreement ? (
                                            <li>
                                                Revoke the Publisher Agreement of {props.publisherInfo.user.loginName}
                                            </li>
                                        ) : null}
                                    </ul>
                                </>
                            )}
                        </Typography>
                    </DialogContentText>
                </DialogContent>
                <DialogActions>
                    <Button variant='contained' color='primary' onClick={() => setDialogOpen(false)}>
                        Cancel
                    </Button>
                    <ButtonWithProgress autoFocus sx={{ ml: 1 }} color='error' working={working} onClick={doRevoke}>
                        Revoke contributions
                    </ButtonWithProgress>
                </DialogActions>
            </Dialog>
        </>
    );
};

export interface PublisherRevokeContributionsButtonProps {
    publisherInfo: PublisherInfo;
}
