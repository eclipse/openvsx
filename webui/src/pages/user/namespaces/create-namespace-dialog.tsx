/** ******************************************************************************
 * Copyright (c) 2022 Precies. Software and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */

import { ChangeEvent, FunctionComponent, useContext, useEffect, useState, useRef } from 'react';
import { Button, Dialog, DialogTitle, DialogContent, Box, TextField, DialogActions } from '@mui/material';
import { ButtonWithProgress } from '../../../components/button-with-progress';
import { isError } from '../../../extension-registry-types';
import { MainContext } from '../../../context';

const NAMESPACE_NAME_SIZE = 255;

export const CreateNamespaceDialog: FunctionComponent<CreateNamespaceDialogProps> = props => {
    const [posted, setPosted] = useState<boolean>(false);
    const [name, setName] = useState<string>('');
    const [nameError, setNameError] = useState<string>();

    const context = useContext(MainContext);
    const abortController = useRef<AbortController>(new AbortController());

    useEffect(() => {
        return () => {
            abortController.current.abort();
        };
    }, []);

    useEffect(() => {
        if (props.open) {
            setPosted(false);
            setName('');
            setNameError(undefined);
        }
    }, [props.open]);

    useEffect(() => {
        document.addEventListener('keydown', handleEnter);
        return () => {
            document.removeEventListener('keydown', handleEnter);
        };
    });

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
            const response = await context.service.createNamespace(abortController.current, name);
            if (isError(response)) {
                throw response;
            }

            props.onClose();
            props.namespaceCreated(name);
        } catch (err) {
            context.handleError(err);
        }

        setPosted(false);
    };

    const handleEnter = (e: KeyboardEvent) => {
        if (props.open && e.code === 'Enter') {
            handleCreateNamespace();
        }
    };

    return (
        <Dialog open={props.open} onClose={props.onClose}>
            <DialogTitle>Create new namespace</DialogTitle>
            <DialogContent>
                <Box my={2}>
                    <TextField
                        fullWidth
                        label='Namespace Name'
                        error={Boolean(nameError)}
                        helperText={nameError}
                        onChange={handleNameChange}
                    />
                </Box>
            </DialogContent>
            <DialogActions>
                <Button onClick={props.onClose}>Cancel</Button>
                <ButtonWithProgress
                    autoFocus
                    sx={{ ml: 1 }}
                    error={Boolean(nameError) || !name}
                    working={posted}
                    onClick={handleCreateNamespace}>
                    Create Namespace
                </ButtonWithProgress>
            </DialogActions>
        </Dialog>
    );
};

export interface CreateNamespaceDialogProps {
    open: boolean;
    onClose: () => void;
    namespaceCreated: (name: string) => void;
}
