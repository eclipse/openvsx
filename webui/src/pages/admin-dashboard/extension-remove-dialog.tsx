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
import { Extension } from '../../extension-registry-types';
import { MainContext } from '../../context';

export const ExtensionRemoveDialog: FunctionComponent<ExtensionRemoveDialog.Props> = props => {
    const { service, handleError } = useContext(MainContext);

    const [dialogOpen, setDialogOpen] = useState(false);
    const [working, setWorking] = useState(false);

    const handleRemoveVersions = async () => {
        try {
            setWorking(true);
            if (props.removeAll) {
                await service.admin.deleteExtensions({ namespace: props.extension.namespace, extension: props.extension.name });
            } else {
                await service.admin.deleteExtensions({ namespace: props.extension.namespace, extension: props.extension.name, versions: props.versions });
            }
            props.onUpdate();
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
            onClick={() => setDialogOpen(true)}
            disabled={props.versions.length === 0} >
            {
                props.removeAll || props.versions.length === 0 ? 'Remove Extension'
                : props.versions.length > 1 ? 'Remove Versions' : 'Remove Version'
            }
        </Button>
        <Dialog
            open={dialogOpen}
            onClose={() => setDialogOpen(false)} >
            <DialogTitle >
                Remove {
                    props.removeAll ? 'all ' : ''
                }{
                    !(props.removeAll && props.versions.length <= 1) ? props.versions.length : ''
                } version{
                    props.removeAll || props.versions.length > 1 ? 's' : ''
                } of {props.extension.name}?
            </DialogTitle>
            <DialogContent>
                <DialogContentText component='div'>
                    {props.versions.map((version, key) => <Typography key={key} variant='body2'>{version}</Typography>)}
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
                    onClick={handleRemoveVersions} >
                    Remove
                </ButtonWithProgress>
            </DialogActions>
        </Dialog>
    </>;
};

export namespace ExtensionRemoveDialog {
    export interface Props {
        versions: string[];
        removeAll: boolean;
        extension: Extension;
        onUpdate: () => void;
    }
}
