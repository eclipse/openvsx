/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import React, { FunctionComponent, useState, useContext, useEffect } from 'react';
import { Button, Dialog, DialogTitle, DialogContent, DialogContentText, DialogActions, Typography } from '@mui/material';
import { ButtonWithProgress } from '../../components/button-with-progress';
import { Extension, TargetPlatformVersion } from '../../extension-registry-types';
import { MainContext } from '../../context';
import { getTargetPlatformDisplayName } from '../../utils';

export const ExtensionRemoveDialog: FunctionComponent<ExtensionRemoveDialogProps> = props => {
    const { service, handleError } = useContext(MainContext);

    const abortController = new AbortController();
    useEffect(() => {
        return () => {
            abortController.abort();
        };
    }, []);

    const [dialogOpen, setDialogOpen] = useState(false);
    const [working, setWorking] = useState(false);

    const WILDCARD = '*';
    const removeAll = () => {
        return props.targetPlatformVersions.find(targetPlatformVersion => targetPlatformVersion.targetPlatform === WILDCARD && targetPlatformVersion.version === WILDCARD);
    };

    const removeVersions = () => {
        return props.targetPlatformVersions.length > 1;
    };

    const handleRemoveVersions = async () => {
        try {
            setWorking(true);
            let targetPlatformVersions = undefined;
            if (!removeAll()) {
                targetPlatformVersions = props.targetPlatformVersions
                    .filter(t => t.targetPlatform !== WILDCARD && t.version !== WILDCARD)
                    .map(t => {
                        return { targetPlatform: t.targetPlatform, version: t.version };
                    });
            }

            await service.admin.deleteExtensions(abortController, { namespace: props.extension.namespace, extension: props.extension.name, targetPlatformVersions: targetPlatformVersions });

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
            disabled={props.targetPlatformVersions.length === 0} >
            {
                removeAll() ? 'Remove Extension' : removeVersions() ? 'Remove Versions' : 'Remove Version'
            }
        </Button>
        <Dialog
            open={dialogOpen}
            onClose={() => setDialogOpen(false)} >
            <DialogTitle >
                Remove {
                    removeAll() ? 'all ' : ''
                }{
                    !(removeAll() && removeVersions()) ? props.targetPlatformVersions.filter((targetVersion) => targetVersion.version !== WILDCARD && targetVersion.targetPlatform !== WILDCARD).length : ''
                } version{
                    removeAll() || removeVersions() ? 's' : ''
                } of {props.extension.name}?
            </DialogTitle>
            <DialogContent>
                <DialogContentText component='div'>
                    {
                        props.targetPlatformVersions
                            .filter((targetPlatformVersion) => targetPlatformVersion.version !== WILDCARD && targetPlatformVersion.targetPlatform !== WILDCARD)
                            .map((targetPlatformVersion, key) => <Typography key={key} variant='body2'>{targetPlatformVersion.version} ({getTargetPlatformDisplayName(targetPlatformVersion.targetPlatform)})</Typography>)}
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
                    sx={{ ml: 1 }}
                    autoFocus
                    working={working}
                    onClick={handleRemoveVersions} >
                    Remove
                </ButtonWithProgress>
            </DialogActions>
        </Dialog>
    </>;
};

export interface ExtensionRemoveDialogProps {
    targetPlatformVersions: TargetPlatformVersion[];
    extension: Extension;
    onUpdate: () => void;
}