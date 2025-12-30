/********************************************************************************
 * Copyright (c) 2025 Precies. Software OU and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import React, { FunctionComponent, useContext, useRef, useState } from 'react';
import {
    Box, Grid, Button, IconButton, Slider, Stack, Dialog, DialogActions, DialogTitle,
    DialogContent
} from '@mui/material';
import DeleteIcon from '@mui/icons-material/Delete';
import EditIcon from '@mui/icons-material/Edit';
import RotateLeftIcon from '@mui/icons-material/RotateLeft';
import RotateRightIcon from '@mui/icons-material/RotateRight';
import ZoomInIcon from '@mui/icons-material/ZoomIn';
import ZoomOutIcon from '@mui/icons-material/ZoomOut';
import { MainContext } from '../../context';
import { Namespace } from '../../extension-registry-types';
import Dropzone from 'react-dropzone';
import AvatarEditor, { Position } from 'react-avatar-editor';
import _ from 'lodash';
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
    gridRow: 1,
    gridColumn: 1,
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
    transition: 'border .24s ease-in-out',
    '&:hover + $avatarButtons': {
        display: 'flex'
    }
}));

export const UserNamespaceDetailsLogo: FunctionComponent<UserNamespaceDetailsLogoProps> = props => {
    const editor = useRef<AvatarEditor>(null);

    const context = useContext(MainContext);
    const [dropzoneFile, setDropzoneFile] = useState<File>();
    const [logoPreview, setLogoPreview] = useState<string>();
    const [editing, setEditing] = useState<boolean>(false);
    const [editorScale, setEditorScale] = useState<number>(1);
    const [editorScaleAdjusted, setEditorScaleAdjusted] = useState<number>(1);
    const [editorRotation, setEditorRotation] = useState<number>(0);
    const [editorPosition, setEditorPosition] = useState<Position>();
    const [prevEditorScale, setPrevEditorScale] = useState<number>(1);
    const [prevEditorRotation, setPrevEditorRotation] = useState<number>(0);
    const [prevEditorPosition, setPrevEditorPosition] = useState<Position>();

    const resetLogoPreview = () => {
        if (logoPreview) {
            URL.revokeObjectURL(logoPreview);
        }
        setLogoPreview(undefined);
    };

    const handleDrop = <T extends File>(acceptedFiles: T[]) => {
        const file = acceptedFiles[0];
        if (file.type !== 'image/png' && file.type != 'image/jpeg') {
            context.handleError(new Error(`Unsupported file type '${file.type}'`));
            return;
        }

        setDropzoneFile(file);
        setEditing(true);
        setEditorScale(1);
        setEditorScaleAdjusted(1);
        setEditorRotation(0);
        setEditorPosition(undefined);
    };

    const handleFileDialogOpen = () => {
        setDropzoneFile(undefined);
        resetLogoPreview();
    };

    const rotateLeft = () => setEditorRotation(editorRotation - 90);
    const rotateRight = () => setEditorRotation(editorRotation + 90);

    const handleEditorScaleChange = (event: Event, value: number | number[]) => {
        setEditorScale((typeof value === 'number') ? value : value[0]);
        setEditorScaleAdjusted(adjustScale(editorScale));
    };

    const handleCancelEditLogo = () => {
        setEditorScale(prevEditorScale);
        setEditorScaleAdjusted(adjustScale(prevEditorScale));
        setEditorRotation(prevEditorRotation);
        setEditorPosition(prevEditorPosition);
        setEditing(false);
    };

    const handleApplyLogo = () => {
        const avatarEditor = editor.current as AvatarEditor;
        const canvasScaled = avatarEditor.getImageScaledToCanvas();
        canvasScaled.toBlob(async (blob) => {
            if (blob) {
                if (logoPreview) {
                    URL.revokeObjectURL(logoPreview);
                }
                setLogoPreview(URL.createObjectURL(blob));
                props.onLogoChange({ file: blob, name: dropzoneFile!.name });
            }
        });
        setEditing(false);
    };

    const adjustScale = (x: number) => {
        return x < 1 ? (0.5 + (x / 2)) : x;
    };

    const percentageLabelFormat = (value: number) => {
        return `${Math.round(value * 100)}%`;
    };

    const deleteLogo = () => {
        resetLogoPreview();
        props.onLogoChange(undefined);
    };

    const editLogo = () => {
        setPrevEditorScale(editorScale);
        setPrevEditorRotation(editorRotation);
        setPrevEditorPosition(editorPosition);
        setEditing(true);
    };

    const handleEditorPositionChange = (editorPosition: Position) => setEditorPosition(editorPosition);

    const isDropzoneDisabled = (): boolean => {
        return logoPreview !== undefined || dropzoneFile?.name != null;
    };

    return <>
        <Dialog
            open={editing}
            onClose={() => setEditing(false)} >
            <DialogTitle >
                Edit namespace logo
            </DialogTitle>
            <DialogContent sx={{ overflowY: 'unset' }}>
                <Grid container spacing={2}>
                    <Grid item xs={12} sx={{ display: 'flex' }}>
                        <AvatarEditor
                            style={{ margin: '0 auto' }}
                            ref={editor}
                            image={dropzoneFile ?? ''}
                            width={120}
                            height={120}
                            border={8}
                            color={[200, 200, 200, 0.6]}
                            scale={editorScaleAdjusted}
                            rotate={editorRotation}
                            position={editorPosition}
                            onPositionChange={handleEditorPositionChange}
                        />
                    </Grid>
                    <Grid item xs={12}>
                        <Grid container spacing={2}>
                            <Grid item><ZoomOutIcon /></Grid>
                            <Grid item xs>
                                <Slider
                                    min={0}
                                    max={2}
                                    step={0.01}
                                    scale={adjustScale}
                                    color='secondary'
                                    valueLabelDisplay='auto'
                                    valueLabelFormat={percentageLabelFormat}
                                    value={editorScale}
                                    onChange={handleEditorScaleChange} />
                            </Grid>
                            <Grid item><ZoomInIcon /></Grid>
                        </Grid>
                    </Grid>
                    <Grid item xs={12} sx={{ display: 'flex' }}>
                        <Stack direction='row' spacing={2} sx={{ margin: '0 auto' }}>
                            <IconButton onClick={rotateLeft} title='Rotate image counter-clockwise'>
                                <RotateLeftIcon />
                            </IconButton>
                            <IconButton onClick={rotateRight} title='Rotate image clockwise'>
                                <RotateRightIcon />
                            </IconButton>
                        </Stack>
                    </Grid>
                </Grid>
            </DialogContent>
            <DialogActions>
                <Button
                    variant='contained'
                    color='primary'
                    onClick={handleCancelEditLogo} >
                    Cancel
                </Button>
                <Button
                    autoFocus
                    onClick={handleApplyLogo} >
                    Apply logo
                </Button>
            </DialogActions>
        </Dialog>
        <Dropzone
            onDrop={handleDrop}
            onFileDialogOpen={handleFileDialogOpen}
            noClick={isDropzoneDisabled()}
            noKeyboard={isDropzoneDisabled()}
            noDrag={isDropzoneDisabled()}
            maxFiles={1}
            maxSize={4 * 1024 * 1024}>
            {({ getRootProps, getInputProps, isFocused, isDragAccept, isDragReject }) => (
                <Box component='section' sx={{ display: 'grid' }}>
                    <DropzoneDiv
                        {...getRootProps({ isFocused, isDragAccept, isDragReject })}
                        style={{ borderColor: getColor(isFocused, isDragAccept, isDragReject) }}
                    >
                        <input {...getInputProps({ accept: 'image/jpeg,image/png', multiple: false })} />
                        <img src={logoPreview ?? dropzoneFile?.name ?? context.pageSettings.urls.extensionDefaultIcon} />
                    </DropzoneDiv>
                    {logoPreview || dropzoneFile?.name ?
                        <Box
                            component='div'
                            sx={{
                                gridRow: 1,
                                gridColumn: 1,
                                mb: -6,
                                display: 'flex',
                                opacity: 0,
                                height: '100%',
                                justifyContent: 'flex-end',
                                '&:hover': {
                                    opacity: 1
                                }
                            }}
                        >
                            {logoPreview ?
                                <IconButton onClick={editLogo} title='Edit logo' sx={{ height: 'fit-content' }}>
                                    <EditIcon />
                                </IconButton>
                                : null
                            }
                            <IconButton onClick={deleteLogo} title='Delete logo' sx={{ height: 'fit-content' }}>
                                <DeleteIcon />
                            </IconButton>
                        </Box>
                        : null
                    }
                </Box>
            )}
        </Dropzone>
    </>;
};

export interface UserNamespaceDetailsLogoProps {
    namespace: Namespace;
    onLogoChange: (logo?: NamespaceDetailsLogo) => void
}

export interface NamespaceDetailsLogo {
    file: Blob
    name: string
}