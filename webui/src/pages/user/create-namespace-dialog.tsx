/** ******************************************************************************
 * Copyright (c) 2022 Precies. Software and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */

import React, { ChangeEvent, FunctionComponent, useContext, useEffect, useState } from 'react';
import { Button, Dialog, DialogTitle, DialogContent, Box, TextField, DialogActions } from '@mui/material';
import { ButtonWithProgress } from '../../components/button-with-progress';
import { isError } from '../../extension-registry-types';
import { MainContext } from '../../context';

const NAMESPACE_NAME_SIZE = 255;

export const CreateNamespaceDialog: FunctionComponent<CreateNamespaceDialogProps> = props => {
    const [open, setOpen] = useState<boolean>(false);
    const [posted, setPosted] = useState<boolean>(false);
    const [name, setName] = useState<string>('');
    const [nameError, setNameError] = useState<string>();

    const context = useContext(MainContext);
    const abortController = new AbortController();

    useEffect(() => {
        document.addEventListener('keydown', handleEnter);
        return () => {
            abortController.abort();
            document.removeEventListener('keydown', handleEnter);
        };
    }, []);

    const handleOpenDialog = () => {
        setOpen(true);
        setPosted(false);
    };

    const handleCancel = () => {
        setOpen(false);
        setName('');
    };

    const handleNameChange = (event: ChangeEvent<HTMLInputElement>) => {
        const name = event.target.value;
        let nameError: string | undefined;
        if (name.length > NAMESPACE_NAME_SIZE) {
            nameError = `The namespace name must not be longer than ${NAMESPACE_NAME_SIZE} characters.`;
        }

        setName(name);
        setNameError(nameError);
    };

    const handleCreateNamespace = async () => {
        if (!context.user) {
            return;
        }

        setPosted(true);
        try {
            const response = await context.service.createNamespace(abortController, name);
            if (isError(response)) {
                throw response;
            }

            setOpen(false);
            props.namespaceCreated();
        } catch (err) {
            context.handleError(err);
        }

        setPosted(false);
    };

    const handleEnter = (e: KeyboardEvent) => {
        if (open && e.code ===  'Enter') {
            handleCreateNamespace();
        }
    };

    return <>
        <Button variant='outlined' onClick={handleOpenDialog}>Create namespace</Button>
        <Dialog open={open} onClose={handleCancel}>
            <DialogTitle>Create new namespace</DialogTitle>
            <DialogContent>
                <Box my={2}>
                    <TextField
                        fullWidth
                        label='Namespace Name'
                        error={Boolean(nameError)}
                        helperText={nameError}
                        onChange={handleNameChange} />
                </Box>
            </DialogContent>
            <DialogActions>
                <Button onClick={handleCancel} color='secondary'>
                    Cancel
                </Button>
            <ButtonWithProgress
                    autoFocus
                    sx={{ ml: 1 }}
                    error={Boolean(nameError) || !name}
                    working={posted}
                    onClick={handleCreateNamespace} >
                Create Namespace
            </ButtonWithProgress>
            </DialogActions>
        </Dialog>
    </>;
};

export interface CreateNamespaceDialogProps {
    namespaceCreated: () => void;
}