/********************************************************************************
 * Copyright (c) 2019 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import React, { ChangeEvent, FunctionComponent, useContext, useEffect, useState } from 'react';
import { Button, Dialog, DialogTitle, DialogContent, DialogContentText, Box, TextField, DialogActions, Typography } from '@mui/material';
import { ButtonWithProgress } from '../../components/button-with-progress';
import { CopyToClipboard } from '../../components/copy-to-clipboard';
import { PersonalAccessToken, isError } from '../../extension-registry-types';
import { MainContext } from '../../context';

const TOKEN_DESCRIPTION_SIZE = 255;

export const GenerateTokenDialog: FunctionComponent<GenerateTokenDialogProps> = props => {
    const [open, setOpen] = useState<boolean>(false);
    const [posted, setPosted] = useState<boolean>(false);
    const [description, setDescription] = useState<string>('');
    const [descriptionError, setDescriptionError] = useState<string>();
    const [token, setToken] = useState<PersonalAccessToken>();

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
        setDescription('');
        setToken(undefined);
    };

    const handleClose = () => setOpen(false);

    const handleDescriptionChange = (event: ChangeEvent<HTMLInputElement>) => {
        const description = event.target.value;
        let descriptionError: string | undefined;
        if (description.length > TOKEN_DESCRIPTION_SIZE) {
            descriptionError = `The description must not be longer than ${TOKEN_DESCRIPTION_SIZE} characters.`;
        }

        setDescription(description);
        setDescriptionError(descriptionError);
    };

    const handleGenerate = async () => {
        if (!context.user) {
            return;
        }
        setPosted(true);
        try {
            const token = await context.service.createAccessToken(abortController, context.user, description);
            if (isError(token)) {
                throw token;
            }
            setToken(token);
            props.handleTokenGenerated();
        } catch (err) {
            context.handleError(err);
        }
    };

    const handleEnter = (e: KeyboardEvent) => {
        if (e.code === 'Enter' && open && !token) {
            handleGenerate();
        }
    };

    return <>
        <Button variant='outlined' onClick={handleOpenDialog}>Generate new token</Button>
        <Dialog open={open} onClose={handleClose}>
            <DialogTitle>Generate new token</DialogTitle>
            <DialogContent>
                <DialogContentText>
                    Describe where you will use this token.
                </DialogContentText>
                <Box my={2}>
                    <TextField
                        disabled={Boolean(token)}
                        fullWidth
                        label='Token Description'
                        error={Boolean(descriptionError)}
                        helperText={descriptionError}
                        onChange={handleDescriptionChange} />
                </Box>
                <TextField
                    disabled={!token}
                    margin='dense'
                    label='Generated Token...'
                    fullWidth
                    multiline
                    variant='outlined'
                    rows={4}
                    value={token ? token.value : ''}
                />
                {
                    token ?
                    <Box>
                        <Typography sx={{ color: 'red', fontWeight: 'bold' }}>
                            Copy and paste this token to a safe place. It will not be displayed again.
                        </Typography>
                    </Box> : null
                }
            </DialogContent>
            <DialogActions>
                {
                    token ?
                    <CopyToClipboard tooltipProps={{ placement: 'left' }}>
                        {({ copy }) => (
                            <Button
                                autoFocus
                                variant='contained'
                                color='secondary'
                                onClick={() => {
                                    copy(token!.value);
                                    setTimeout(handleClose, 700);
                                }}
                            >
                                Copy
                            </Button>
                        )}
                    </CopyToClipboard> : null
                }
                <Button onClick={handleClose} color='secondary'>
                    {token ? 'Close' : 'Cancel'}
                </Button>
                {
                    !token ?
                    <ButtonWithProgress
                            autoFocus
                            sx={{ ml: 1 }}
                            error={Boolean(descriptionError)}
                            working={posted}
                            onClick={handleGenerate} >
                        Generate Token
                    </ButtonWithProgress> : null
                }
            </DialogActions>
        </Dialog>
    </>;
};

export interface GenerateTokenDialogProps {
    handleTokenGenerated: () => void;
}