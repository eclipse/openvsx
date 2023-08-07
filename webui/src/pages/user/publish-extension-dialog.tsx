/** ******************************************************************************
 * Copyright (c) 2022 Precies. Software and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */

import React, { FunctionComponent, useContext, useEffect, useState } from 'react';
import { Button, Dialog, DialogTitle, DialogContent, DialogActions, Typography, Box, Paper } from '@mui/material';
import { CheckCircleOutline } from '@mui/icons-material';
import Dropzone from 'react-dropzone';
import { ButtonWithProgress } from '../../components/button-with-progress';
import { ErrorResult, isError } from '../../extension-registry-types';
import { MainContext } from '../../context';
import { styled, Theme } from '@mui/material/styles';

const getColor = (isFocused: boolean, isDragAccept: boolean, isDragReject: boolean) => {
    if (isDragAccept) {
        return 'success.main';
    } else if (isDragReject) {
        return 'error.main';
    } else if (isFocused) {
        return 'secondary.main';
    } else {
        return 'text.primary';
    }
};

const DropzoneDiv = styled('div')(({ theme }: { theme: Theme }) => ({
    flex: 1,
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    padding: theme.spacing(3),
    borderWidth: 2,
    borderRadius: 2,
    borderStyle: 'dashed',
    backgroundColor: theme.palette.background.default,
    color: theme.palette.text.primary,
    outline: 'none',
    transition: 'border .24s ease-in-out'
}));

export const PublishExtensionDialog: FunctionComponent<PublishExtensionDialogProps> = props => {
    const [open, setOpen] = useState<boolean>(false);
    const [publishing, setPublishing] = useState<boolean>(false);
    const [fileToPublish, setFileToPublish] = useState<File>();
    const [oldFileToPublish, setOldFileToPublish] = useState<File>();

    const context = useContext(MainContext);
    const abortController = new AbortController();

    useEffect(() => {
        document.addEventListener('keydown', handleEnter);
        return () => {
            abortController.abort();
            document.removeEventListener('keydown', handleEnter);
        };
    }, []);

    const toMegaBytes = (bytes: number): string => {
        const megaBytes = bytes / (1024.0 * 1024.0);
        return megaBytes.toLocaleString('en-US', {
            minimumFractionDigits: 2,
            maximumFractionDigits: 2,
        });
    };

    const handleOpenDialog = () => setOpen(true);

    const handleCancel = () => {
        if (publishing) {
            abortController.abort();
        }

        setOpen(false);
        setPublishing(false);
        setFileToPublish(undefined);
        setOldFileToPublish(undefined);
    };

    const handleUndo = () => {
        setFileToPublish(oldFileToPublish);
        setOldFileToPublish(undefined);
    };

    const handleDrop = <T extends File>(acceptedFiles: T[]) => {
        if (fileToPublish) {
            setOldFileToPublish(fileToPublish);
        }

        setFileToPublish(acceptedFiles[0]);
    };

    const handleFileDialogOpen = () => setOldFileToPublish(undefined);

    const handlePublish = async () => {
        if (!context.user || !fileToPublish) {
            return;
        }

        setPublishing(true);
        let published = false;
        let retryPublish = false;
        try {
            published = await tryPublishExtension(fileToPublish);
        } catch (err) {
            try {
                await tryResolveNamespaceError(err);
                retryPublish = true;
            } catch (namespaceError) {
                context.handleError(namespaceError);
            }
        }
        if (retryPublish) {
            try {
                published = await tryPublishExtension(fileToPublish);
            } catch (err) {
                context.handleError(err);
            }
        }
        if (published) {
            props.extensionPublished();
            setOpen(false);
            setFileToPublish(undefined);
            setOldFileToPublish(undefined);
        }

        setPublishing(false);
    };

    const handleEnter = (e: KeyboardEvent) => {
        if (e.code ===  'Enter') {
            handlePublish();
        }
    };

    const tryPublishExtension = async (fileToPublish: File): Promise<boolean> => {
        let published = false;
        const publishResponse = await context.service.publishExtension(abortController, fileToPublish);
        if (isError(publishResponse)) {
            throw publishResponse;
        }

        published = true;
        return published;
    };

    const tryResolveNamespaceError = async (publishResponse: Readonly<ErrorResult>) => {
        const namespaceError = 'Unknown publisher: ';
        if (!publishResponse.error.startsWith(namespaceError)) {
            throw publishResponse;
        }
        const namespace = publishResponse.error.substring(namespaceError.length, publishResponse.error.indexOf('\n', namespaceError.length));
        if (!namespace || namespace === 'undefined') {
            const result: Readonly<ErrorResult> = {
                error: `Invalid namespace: ${namespace}`
            };
            throw result;
        }
        const namespaceResponse = await context.service.createNamespace(abortController, namespace);
        if (isError(namespaceResponse)) {
            throw namespaceResponse;
        }
    };

    const successColor = context.pageSettings.themeType === 'dark' ? '#fff' : '#000';
        return <>
            <Button variant='outlined' onClick={handleOpenDialog}>Publish extension</Button>
            <Dialog open={open} onClose={handleCancel}>
                <DialogTitle>Publish extension</DialogTitle>
                <DialogContent>
                    {
                        oldFileToPublish
                            ? <Paper
                                sx={{
                                    mb: 2,
                                    p: 2,
                                    flex: 1,
                                    display: 'flex',
                                    flexDirection: 'row',
                                    alignItems: 'center',
                                    color: successColor,
                                    bgcolor: `success.${context.pageSettings.themeType}`
                                }}
                              >
                                <CheckCircleOutline fontSize='large' />
                                <Typography variant='body1' sx={{ ml: 1 }}>Changed extension package.</Typography>
                                <Button onClick={handleUndo} sx={{ ml: 'auto' }}>Undo</Button>
                            </Paper>
                            : null
                    }
                    <Dropzone onDrop={handleDrop} onFileDialogOpen={handleFileDialogOpen} maxFiles={1} maxSize={512 * 1024 * 1024}>
                    {({ getRootProps, getInputProps, isFocused, isDragAccept, isDragReject }) => (
                        <section>
                            <DropzoneDiv
                                {...getRootProps({ isFocused, isDragAccept, isDragReject })}
                                style={{ borderColor: getColor(isFocused, isDragAccept, isDragReject) }}
                            >
                                <input {...getInputProps({ accept: 'application/vsix,.vsix', multiple: false })} />
                                <p>Drag &amp; drop an extension here, or click to select an extension</p>
                                <p>(Only 1 *.vsix package at a time is accepted)</p>
                            </DropzoneDiv>
                            {
                                fileToPublish
                                    ? <Box mt={1}>
                                        <Typography key={fileToPublish.name} variant='body2' sx={{ fontWeight: 'bold' }}>
                                            {fileToPublish.name} ({toMegaBytes(fileToPublish.size)} MB)
                                        </Typography>
                                    </Box>
                                    : null
                            }
                        </section>
                    )}
                    </Dropzone>
                </DialogContent>
                <DialogActions>
                    <Button onClick={handleCancel} color='secondary'>
                        Cancel
                    </Button>
                    <ButtonWithProgress
                            autoFocus
                            sx={{ ml: 1 }}
                            title="After you click 'Publish', this extension will be available on the Marketplace"
                            error={!fileToPublish}
                            working={publishing}
                            onClick={handlePublish} >
                        Publish
                    </ButtonWithProgress>
                </DialogActions>
            </Dialog>
        </>;
};

export interface PublishExtensionDialogProps {
    extensionPublished: () => void;
}