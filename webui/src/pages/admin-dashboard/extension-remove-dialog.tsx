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
import { Extension } from '../../extension-registry-types';
import { MainContext } from '../../context';

interface ExtensionRemoveDialogProps {
    versions: string[];
    removeAll: boolean;
    extension: Extension;
    onUpdate: () => void;
}

export const ExtensionRemoveDialog: FunctionComponent<ExtensionRemoveDialogProps> = props => {
    const { service, handleError } = useContext(MainContext);

    const [dialogOpen, setDialogOpen] = useState(false);
    const handleOpenRemoveDialog = () => {
        setDialogOpen(true);
    };
    const handleCancelRemoveDialog = () => {
        setDialogOpen(false);
    };
    const handleRemoveVersions = async () => {
        try {
            if (props.removeAll) {
                await service.admin.deleteExtension({ namespace: props.extension.namespace, extension: props.extension.name });
            } else {
                const prms = props.versions.map(version => service.admin.deleteExtension({ namespace: props.extension.namespace, extension: props.extension.name, version }));
                await Promise.all(prms);
            }
            props.onUpdate();
            setDialogOpen(false);
        } catch (err) {
            handleError(err);
        }
    };

    return <>
        <Button variant='contained' color='secondary' onClick={handleOpenRemoveDialog} disabled={!props.versions.length}>
            Remove version{props.versions.length > 1 ? 's' : ''}
        </Button>
        <Dialog
            open={dialogOpen}
            onClose={handleCancelRemoveDialog}>
            <DialogTitle >Remove {props.versions.length} version{props.versions.length > 1 ? 's' : ''} of {props.extension.name}?</DialogTitle>
            <DialogContent>
                <DialogContentText component='div'>
                    {props.versions.map((version, key) => <Typography key={key} variant='body2'>{version}</Typography>)}
                </DialogContentText>
            </DialogContent>
            <DialogActions>
                <Button autoFocus onClick={handleCancelRemoveDialog} variant='contained' color='primary'>
                    Cancel
                </Button>
                <Button onClick={handleRemoveVersions} variant='contained' color='secondary' autoFocus>
                    Remove
                </Button>
            </DialogActions>
        </Dialog>
    </>;
};