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
import { PublisherInfo, isError } from '../../extension-registry-types';
import { ServiceContext } from '../../default/default-app';
import { ErrorHandlerContext } from '../../main';
import { UpdateContext } from './publisher-admin';

interface PublisherRevokeDialogProps {
    publisherInfo: PublisherInfo;
}

export const PublisherRevokeDialog: FunctionComponent<PublisherRevokeDialogProps> = props => {
    const service = useContext(ServiceContext);
    const errorContext = useContext(ErrorHandlerContext);

    const updateContext = useContext(UpdateContext);

    const [dialogOpen, setDialogOpen] = useState(false);
    const handleOpenRevokeDialog = () => {
        setDialogOpen(true);
    };
    const handleCancelRevokeDialog = () => {
        setDialogOpen(false);
    };
    const handleRemoveVersions = async () => {
        try {
            updateContext.setLoading(true);
            const result = await service.admin.revokePublisherAgreement(props.publisherInfo.user.provider!, props.publisherInfo.user.loginName);
            if (isError(result)) {
                throw (result.error);
            }
            updateContext.handleUpdate();
            updateContext.setLoading(false);
            setDialogOpen(false);
        } catch (err) {
            errorContext && errorContext.handleError(err);
        }
    };

    return <>
        <Button variant='contained' color='secondary' onClick={handleOpenRevokeDialog}>
            Revoke Publisher Agreement
        </Button>
        <Dialog
            open={dialogOpen}
            onClose={handleCancelRevokeDialog}>
            <DialogTitle >Revoke Publisher Agreement?</DialogTitle>
            <DialogContent>
                <DialogContentText component='div'>
                    <Typography>
                        This will deactivate {props.publisherInfo.user.loginName}s access
                        tokens and delete all {props.publisherInfo.extensions.length} extension versions
                        published by {props.publisherInfo.user.loginName}.
                    </Typography>
                </DialogContentText>
            </DialogContent>
            <DialogActions>
                <Button autoFocus onClick={handleCancelRevokeDialog} variant='contained' color='primary'>
                    Cancel
                </Button>
                <Button onClick={handleRemoveVersions} variant='contained' color='secondary' autoFocus>
                    Revoke Agreement
                </Button>
            </DialogActions>
        </Dialog>
    </>;
};