/** ******************************************************************************
 * Copyright (c) 2022 Precies. Software and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */

import * as React from 'react';
import {
    Button, Theme, createStyles, WithStyles, withStyles, Dialog, DialogTitle,
    DialogContent, DialogActions, Typography, Box, Paper
} from '@material-ui/core';
import { CheckCircleOutline } from '@material-ui/icons';
import Dropzone from 'react-dropzone';
import { ButtonWithProgress } from '../../components/button-with-progress';
import { ErrorResult, isError } from '../../extension-registry-types';
import { MainContext } from '../../context';
import clsx from 'clsx';

const publishDialogStyle = (theme: Theme) => createStyles({
    boldText: {
        fontWeight: 'bold'
    },
    banner: {
        margin: `0 0 ${theme.spacing(2)}px 0`,
        padding: theme.spacing(2),
        flex: 1,
        display: 'flex',
        flexDirection: 'row',
        alignItems: 'center'
    },
    bannerText: {
        marginLeft: theme.spacing(1)
    },
    undoButton: {
        marginLeft: 'auto'
    },
    successLight: {
        backgroundColor: theme.palette.success.light,
        color: '#000'
    },
    successDark: {
        backgroundColor: theme.palette.success.dark,
        color: '#fff'
    },
    dropzone: {
        flex: 1,
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        padding: theme.spacing(3),
        borderWidth: 2,
        borderRadius: 2,
        borderColor: theme.palette.text.primary,
        borderStyle: 'dashed',
        backgroundColor: theme.palette.background.default,
        color: theme.palette.text.primary,
        outline: 'none',
        transition: 'border .24s ease-in-out'
    },
    dropzoneFocused: {
        borderColor: theme.palette.secondary.main
    },
    dropzoneAccept: {
        borderColor: theme.palette.success.main
    },
    dropzoneReject: {
        borderColor: theme.palette.error.main
    }
});

class PublishExtensionDialogComponent extends React.Component<PublishExtensionDialogComponent.Props, PublishExtensionDialogComponent.State> {

    static contextType = MainContext;
    declare context: MainContext;

    protected abortController = new AbortController();

    constructor(props: PublishExtensionDialogComponent.Props) {
        super(props);
        this.state = {
            open: false,
            publishing: false,
            fileToPublish: undefined,
            oldFileToPublish: undefined
        };
    }

    protected toMegaBytes = (bytes: number): string => {
        const megaBytes = bytes / (1024.0 * 1024.0);
        return megaBytes.toLocaleString('en-US', {
            minimumFractionDigits: 2,
            maximumFractionDigits: 2,
        });
    };

    protected handleOpenDialog = () => {
        this.setState({ open: true });
    };

    protected handleCancel = () => {
        if (this.state.publishing) {
            this.abortController.abort();
        }
        this.setState({
            open: false,
            publishing: false,
            fileToPublish: undefined,
            oldFileToPublish: undefined
        });
    };

    protected handleUndo = () => {
        const oldFileToPublish = this.state.oldFileToPublish;
        this.setState({ fileToPublish: oldFileToPublish, oldFileToPublish: undefined });
    };

    protected handleDrop = <T extends File>(acceptedFiles: T[]) => {
        if (this.state.fileToPublish) {
            this.setState({ oldFileToPublish: this.state.fileToPublish });
        }

        this.setState({ fileToPublish: acceptedFiles[0] });
    };

    protected handleFileDialogOpen = () => {
        this.setState({ oldFileToPublish: undefined });
    };

    protected handlePublish = async () => {
        if (!this.context.user || !this.state.fileToPublish) {
            return;
        }

        this.setState({ publishing: true });
        let published = false;
        let retryPublish = false;
        try {
            published = await this.tryPublishExtension(this.state.fileToPublish);
        } catch (err) {
            try {
                await this.tryResolveNamespaceError(err);
                retryPublish = true;
            } catch (namespaceError) {
                this.context.handleError(namespaceError);
            }
        }
        if (retryPublish) {
            try {
                published = await this.tryPublishExtension(this.state.fileToPublish);
            } catch (err) {
                this.context.handleError(err);
            }
        }
        if (published) {
            this.props.extensionPublished();
            this.setState({
                open: false,
                fileToPublish: undefined,
                oldFileToPublish: undefined
            });
        }

        this.setState({ publishing: false });
    };

    handleEnter = (e: KeyboardEvent) => {
        if (e.code ===  'Enter') {
            this.handlePublish();
        }
    };

    protected tryPublishExtension = async (fileToPublish: File): Promise<boolean> => {
        let published = false;
        const publishResponse = await this.context.service.publishExtension(this.abortController, fileToPublish);
        if (isError(publishResponse)) {
            throw publishResponse;
        }

        published = true;
        return published;
    };

    protected tryResolveNamespaceError = async (publishResponse: Readonly<ErrorResult>) => {
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
        const namespaceResponse = await this.context.service.createNamespace(this.abortController, namespace);
        if (isError(namespaceResponse)) {
            throw namespaceResponse;
        }
    };

    componentDidMount() {
        document.addEventListener('keydown', this.handleEnter);
    }

    componentWillUnmount() {
        this.abortController.abort();
        document.removeEventListener('keydown', this.handleEnter);
    }

    render() {
        const classes = this.props.classes;
        const successClass = this.context.pageSettings.themeType === 'dark' ? classes.successDark : classes.successLight;

        return <React.Fragment>
            <Button variant='outlined' onClick={this.handleOpenDialog}>Publish extension</Button>
            <Dialog open={this.state.open} onClose={this.handleCancel}>
                <DialogTitle>Publish extension</DialogTitle>
                <DialogContent>
                    {
                        this.state.oldFileToPublish
                            ? <Paper className={`${classes.banner} ${successClass}`}>
                                <CheckCircleOutline fontSize='large' />
                                <Typography variant='body1' className={classes.bannerText}>Changed extension package.</Typography>
                                <Button onClick={this.handleUndo} className={classes.undoButton}>Undo</Button>
                            </Paper>
                            : null
                    }
                    <Dropzone onDrop={this.handleDrop} onFileDialogOpen={this.handleFileDialogOpen} maxFiles={1} maxSize={512 * 1024 * 1024}>
                    {({ getRootProps, getInputProps, isFocused, isDragAccept, isDragReject }) => (
                        <section>
                            <div {...getRootProps({ className: clsx(
                                this.props.classes.dropzone,
                                isFocused && this.props.classes.dropzoneFocused,
                                isDragAccept && this.props.classes.dropzoneAccept,
                                isDragReject && this.props.classes.dropzoneReject
                                ) })}>
                                <input {...getInputProps({ accept: 'application/vsix,.vsix', multiple: false })} />
                                <p>Drag &amp; drop an extension here, or click to select an extension</p>
                                <p>(Only 1 *.vsix package at a time is accepted)</p>
                            </div>
                            {
                                this.state.fileToPublish
                                    ? <Box mt={1}>
                                        <Typography key={this.state.fileToPublish.name} variant='body2' classes={{ root: this.props.classes.boldText }}>
                                            {this.state.fileToPublish.name} ({this.toMegaBytes(this.state.fileToPublish.size)} MB)
                                        </Typography>
                                    </Box>
                                    : null
                            }
                        </section>
                    )}
                    </Dropzone>
                </DialogContent>
                <DialogActions>
                    <Button onClick={this.handleCancel} color='secondary'>
                        Cancel
                    </Button>
                    <ButtonWithProgress
                            autoFocus
                            title="After you click 'Publish', this extension will be available on the Marketplace"
                            error={!this.state.fileToPublish}
                            working={this.state.publishing}
                            onClick={this.handlePublish} >
                        Publish
                    </ButtonWithProgress>
                </DialogActions>
            </Dialog>
        </React.Fragment>;
    }
}

export namespace PublishExtensionDialogComponent {
    export interface Props extends WithStyles<typeof publishDialogStyle> {
        extensionPublished: () => void;
    }

    export interface State {
        open: boolean;
        publishing: boolean;
        fileToPublish?: File;
        oldFileToPublish?: File;
    }
}

export const PublishExtensionDialog = withStyles(publishDialogStyle)(PublishExtensionDialogComponent);