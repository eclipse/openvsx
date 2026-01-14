/********************************************************************************
 * Copyright (c) 2026 and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import React, { ChangeEvent, FunctionComponent, useContext, useEffect, useRef, useState } from 'react';
import { Box, Button, Dialog, DialogActions, DialogContent, DialogTitle, TextField, Typography } from '@mui/material';
import { ButtonWithProgress } from '../../components/button-with-progress';
import { Namespace, isError, SuccessResult } from '../../extension-registry-types';
import { MainContext } from '../../context';
import { InfoDialog } from '../../components/info-dialog';

export const NamespaceDeleteDialog: FunctionComponent<NamespaceDeleteDialogProps> = props => {
    const { service, handleError } = useContext(MainContext);
    const abortController = useRef<AbortController>(new AbortController());
    
    const [working, setWorking] = useState(false);
    const [confirmText, setConfirmText] = useState('');
    const [infoDialogIsOpen, setInfoDialogIsOpen] = useState(false);
    const [infoDialogMessage, setInfoDialogMessage] = useState('');

    useEffect(() => {
        return () => {
            abortController.current.abort();
        };
    }, []);

    useEffect(() => {
        if (!props.open) {
            setConfirmText('');
        }
    }, [props.open]);

    const onConfirmTextChange = (event: ChangeEvent<HTMLInputElement>) => {
        setConfirmText(event.target.value);
    };

    const handleCloseInfoDialog = () => {
        setInfoDialogIsOpen(false);
        props.onClose();
        props.onDeleted?.();
    };

    const handleDeleteNamespace = async () => {
        if (!props.namespace) {
            return;
        }

        try {
            setWorking(true);
            props.setLoadingState(true);
            const result = await service.admin.deleteNamespace(abortController.current, props.namespace.name);
            if (isError(result)) {
                throw result;
            }

            const successResult = result as SuccessResult;
            props.setLoadingState(false);
            setWorking(false);
            setInfoDialogIsOpen(true);
            setInfoDialogMessage(successResult.success);
        } catch (err) {
            props.setLoadingState(false);
            setWorking(false);
            handleError(err);
        }
    };

    const isConfirmValid = confirmText === props.namespace?.name;

    return <>
        <Dialog open={props.open} onClose={props.onClose} maxWidth='md' fullWidth>
            <DialogTitle>Delete Namespace</DialogTitle>
            <DialogContent>
                <Box>
                    <Typography variant='body1' sx={{ mb: 2 }}>
                        Are you sure you want to delete the namespace <strong>{props.namespace?.name}</strong>?
                    </Typography>
                    <Typography variant='body2' color='error' sx={{ mb: 2 }}>
                        <strong>Warning:</strong> This action cannot be undone. The namespace can only be deleted if it contains no extensions.
                    </Typography>
                    <Typography variant='body2' sx={{ mb: 2 }}>
                        Type the namespace name <strong>{props.namespace?.name}</strong> to confirm:
                    </Typography>
                    <TextField
                        fullWidth
                        autoFocus
                        placeholder={props.namespace?.name}
                        value={confirmText}
                        onChange={onConfirmTextChange}
                        variant='outlined'
                    />
                </Box>
            </DialogContent>
            <DialogActions>
                <Button onClick={props.onClose} color='secondary'>
                    Cancel
                </Button>
                <ButtonWithProgress
                    working={working}
                    disabled={!isConfirmValid}
                    onClick={handleDeleteNamespace}
                    color='error'>
                    Delete Namespace
                </ButtonWithProgress>
            </DialogActions>
        </Dialog>
        <InfoDialog
            open={infoDialogIsOpen}
            onClose={handleCloseInfoDialog}
            message={infoDialogMessage} />
    </>;
};

export interface NamespaceDeleteDialogProps {
    open: boolean;
    onClose: () => void;
    namespace?: Namespace;
    setLoadingState: (loading: boolean) => void;
    onDeleted?: () => void;
}
