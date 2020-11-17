/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import React, { FunctionComponent, useState, useContext } from 'react';
import { Button, Dialog, DialogTitle, DialogContent, DialogContentText, DialogActions, Typography } from '@material-ui/core';
import { ButtonWithProgress } from '../../components/button-with-progress';
import { PublisherInfo, isError } from '../../extension-registry-types';
import { MainContext } from '../../context';
import { UpdateContext } from './publisher-admin';

export const PublisherRevokeDialog: FunctionComponent<PublisherRevokeDialog.Props> = props => {
    const { service, handleError } = useContext(MainContext);
    const updateContext = useContext(UpdateContext);

    const [dialogOpen, setDialogOpen] = useState(false);
    const [working, setWorking] = useState(false);

    const doRevoke = async () => {
        try {
            setWorking(true);
            const user = props.publisherInfo.user;
            const result = await service.admin.revokePublisherContributions(user.provider!, user.loginName);
            if (isError(result)) {
                throw result;
            }
            updateContext.handleUpdate();
            setDialogOpen(false);
        } catch (err) {
            handleError(err);
        } finally {
            setWorking(false);
        }
    };

    return <>
        <Button
            variant='contained'
            color='secondary'
            onClick={() => setDialogOpen(true)} >
            Revoke Publisher Contributions
        </Button>
        <Dialog
            open={dialogOpen}
            onClose={() => setDialogOpen(false)}>
            <DialogTitle >Revoke Publisher Contributions?</DialogTitle>
            <DialogContent>
                <DialogContentText component='div'>
                    <Typography>
                        This will deactivate the access tokens of {props.publisherInfo.user.loginName} and
                        delete all {props.publisherInfo.extensions.length} extension versions
                        published by {props.publisherInfo.user.loginName}.
                    </Typography>
                </DialogContentText>
            </DialogContent>
            <DialogActions>
                <Button
                    variant='contained'
                    color='primary'
                    onClick={() => setDialogOpen(false)} >
                    Cancel
                </Button>
                <ButtonWithProgress
                    autoFocus
                    working={working}
                    onClick={doRevoke} >
                    Revoke Contributions
                </ButtonWithProgress>
            </DialogActions>
        </Dialog>
    </>;
};

export namespace PublisherRevokeDialog {
    export interface Props {
        publisherInfo: PublisherInfo;
    }
}
