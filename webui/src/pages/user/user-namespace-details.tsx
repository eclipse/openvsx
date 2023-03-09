/********************************************************************************
 * Copyright (c) 2022 Precies. Software and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import * as React from 'react';
import { withStyles, Theme, createStyles, WithStyles, TextField, Typography, Grid, Button, IconButton, Slider,
    Dialog, DialogActions, DialogTitle, DialogContent, InputAdornment, Select, MenuItem, Paper } from '@material-ui/core';
import { CheckCircleOutline } from '@material-ui/icons';
import BusinessIcon from '@material-ui/icons/Business';
import DeleteIcon from '@material-ui/icons/Delete';
import EditIcon from '@material-ui/icons/Edit';
import GitHubIcon from '@material-ui/icons/GitHub';
import LinkedInIcon from '@material-ui/icons/LinkedIn';
import PersonIcon from '@material-ui/icons/Person';
import RotateLeftIcon from '@material-ui/icons/RotateLeft';
import RotateRightIcon from '@material-ui/icons/RotateRight';
import TwitterIcon from '@material-ui/icons/Twitter';
import ZoomInIcon from '@material-ui/icons/ZoomIn';
import ZoomOutIcon from '@material-ui/icons/ZoomOut';
import CloseIcon from '@material-ui/icons/Close';
import { MainContext } from '../../context';
import { DelayedLoadIndicator } from '../../components/delayed-load-indicator';
import { Namespace, NamespaceDetails, isError } from '../../extension-registry-types';
import Dropzone from 'react-dropzone';
import AvatarEditor, { Position } from 'react-avatar-editor';
import clsx from 'clsx';
import _ from 'lodash';

const detailStyles = (theme: Theme) => createStyles({
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
    successLight: {
        backgroundColor: theme.palette.success.light,
        color: '#000'
    },
    successDark: {
        backgroundColor: theme.palette.success.dark,
        color: '#fff'
    },
    closeButton: {
        marginLeft: 'auto'
    },
    addButton: {
        [theme.breakpoints.down('md')]: {
            marginLeft: theme.spacing(2)
        }
    },
    dialogOverflow: {
        overflowY: 'unset'
    },
    avatarSection: {
        display: 'grid'
    },
    avatarButtons: {
        gridRow: 1,
        gridColumn: 1,
        marginBottom: theme.spacing(-6),
        display: 'none',
        height: 'fit-content',
        justifyContent: 'flex-end',
        '&:hover': {
            display: 'flex'
        }
    },
    textSecondary: {
        color: theme.palette.text.secondary
    },
    dropzone: {
        gridRow: 1,
        gridColumn: 1,
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
        transition: 'border .24s ease-in-out',
        '&:hover + $avatarButtons': {
            display: 'flex'
        }
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

class UserNamespaceDetailsComponent extends React.Component<UserNamespaceDetails.Props, UserNamespaceDetails.State> {
    static readonly INPUT_DISPLAY_NAME = 'display-name';
    static readonly INPUT_DESCRIPTION = 'description';
    static readonly INPUT_WEBSITE = 'website';
    static readonly INPUT_SUPPORT_LINK = 'support-link';
    static readonly INPUT_LINKEDIN = 'linkedin';
    static readonly INPUT_GITHUB = 'github';
    static readonly INPUT_TWITTER = 'twitter';
    static readonly LINKED_IN_PERSONAL = 'in';
    static readonly LINKED_IN_COMPANY = 'company';

    static contextType = MainContext;
    declare context: MainContext;

    protected abortController = new AbortController();
    private editor: React.RefObject<AvatarEditor>;

    constructor(props: UserNamespaceDetails.Props) {
        super(props);
        this.editor = React.createRef();
        this.state = {
            detailsUpdated: false,
            bannerNamespaceName: '',
            loading: true,
            editing: false,
            editorScale: 1,
            editorScaleAdjusted: 1,
            editorRotation: 0,
            prevEditorScale: 1,
            prevEditorRotation: 0,
            linkedInAccountType: UserNamespaceDetailsComponent.LINKED_IN_PERSONAL
        };
    }

    componentDidMount(): void {
        this.getNamespaceDetails();
    }

    componentDidUpdate(prevProps: UserNamespaceDetails.Props) {
        const prevNamespace = prevProps.namespace;
        const newNamespace = this.props.namespace;
        if (prevNamespace !== newNamespace) {
            this.setState({ loading: true });
            this.getNamespaceDetails();
        }
    }

    protected copy<Type>(arg: Type): Type {
        return JSON.parse(JSON.stringify(arg));
    }

    protected getNamespaceDetails = async (): Promise<void> => {
        if (!this.props.namespace.name) {
            this.setState({ currentDetails: undefined, newDetails: undefined, loading: false });
            return;
        }

        try {
            const details = await this.context.service.getNamespaceDetails(this.abortController, this.props.namespace.name);
            if (isError(details)) {
                throw details;
            }

            let linkedInAccountType = UserNamespaceDetailsComponent.LINKED_IN_PERSONAL;
            const linkedin = details.socialLinks.linkedin;
            if (linkedin) {
                const linkedinPath = linkedin.split('/');
                details.socialLinks.linkedin = linkedinPath[linkedinPath.length - 1];
                linkedInAccountType = linkedinPath[linkedinPath.length - 2];
            }

            const github = details.socialLinks.github;
            if (github) {
                details.socialLinks.github = github.substring(github.lastIndexOf('/') + 1);
            }

            const twitter = details.socialLinks.twitter;
            if (twitter) {
                details.socialLinks.twitter = twitter.substring(twitter.lastIndexOf('/') + 1);
            }

            const currentDetails = this.copy<NamespaceDetails>(details);
            const newDetails = this.copy<NamespaceDetails>(details);
            this.setState({ currentDetails, newDetails, linkedInAccountType, loading: false });
        } catch (err) {
            this.context.handleError(err);
            this.setState({ loading: false });
        } finally {
            this.setState({ detailsUpdated: false });
        }
    };

    protected setNamespaceDetails = async () => {
        if (!this.state.newDetails) {
            return;
        }

        this.setState({ loading: true, detailsUpdated: false });
        try {
            const details = this.copy<NamespaceDetails>(this.state.newDetails);
            details.socialLinks.linkedin = details.socialLinks.linkedin
                ? `https://www.linkedin.com/${this.state.linkedInAccountType}/${details.socialLinks.linkedin}`
                : undefined;

            details.socialLinks.github = details.socialLinks.github
                ? 'https://github.com/' + details.socialLinks.github
                : undefined;

            details.socialLinks.twitter = details.socialLinks.twitter
                ? 'https://twitter.com/' + details.socialLinks.twitter
                : undefined;

            const result = await this.context.service.setNamespaceDetails(this.abortController, details);
            if (isError(result)) {
                throw result;
            }

            this.setState({
                detailsUpdated: true,
                currentDetails: this.copy<NamespaceDetails>(this.state.newDetails),
                bannerNamespaceName: (this.state.newDetails.displayName || this.state.newDetails.name)
            });
        } catch (err) {
            this.context.handleError(err);
        } finally {
            this.setState({ loading: false });
        }
    };

    protected handleInputChange = (event: React.ChangeEvent<HTMLInputElement>) => {
        if (!this.state.newDetails) {
            return;
        }

        const input = event.target;
        const details = this.state.newDetails;
        switch (input.name) {
            case UserNamespaceDetailsComponent.INPUT_DISPLAY_NAME:
                details.displayName = input.value;
                break;
            case UserNamespaceDetailsComponent.INPUT_DESCRIPTION:
                details.description = input.value;
                break;
            case UserNamespaceDetailsComponent.INPUT_WEBSITE:
                details.website = input.value;
                break;
            case UserNamespaceDetailsComponent.INPUT_SUPPORT_LINK:
                details.supportLink = input.value;
                break;
            case UserNamespaceDetailsComponent.INPUT_LINKEDIN:
                if (input.value.startsWith('https://www.linkedin.com/')) {
                    if (input.value.lastIndexOf('/') === input.value.length - 1) {
                        input.value = input.value.substring(0, input.value.length - 1);
                    }

                    const linkedinPath = input.value.split('/');
                    details.socialLinks.linkedin = linkedinPath[linkedinPath.length - 1];
                    const linkedInAccountType = linkedinPath[linkedinPath.length - 2];
                    this.setState({ linkedInAccountType });
                } else {
                    details.socialLinks.linkedin = input.value;
                }
                break;
            case UserNamespaceDetailsComponent.INPUT_GITHUB:
                if (input.value.startsWith('https://github.com/')) {
                    if (input.value.lastIndexOf('/') === input.value.length - 1) {
                        input.value = input.value.substring(0, input.value.length - 1);
                    }

                    details.socialLinks.github = input.value.substring(input.value.lastIndexOf('/') + 1);
                } else {
                    details.socialLinks.github = input.value;
                }
                break;
            case UserNamespaceDetailsComponent.INPUT_TWITTER:
                if (input.value.startsWith('https://twitter.com/')) {
                    if (input.value.lastIndexOf('/') === input.value.length - 1) {
                        input.value = input.value.substring(0, input.value.length - 1);
                    }

                    details.socialLinks.twitter = input.value.substring(input.value.lastIndexOf('/') + 1);
                } else {
                    details.socialLinks.twitter = input.value;
                }
                break;
        }

        this.setState({ newDetails: details });
    };

    protected handleSelectChange = (event: React.ChangeEvent<{ name: string | undefined, value: unknown}>) => {
        const linkedInAccountType = event.target.value as string;
        this.setState({ linkedInAccountType });
    };

    protected handleDrop = <T extends File>(acceptedFiles: T[]) => {
        const file = acceptedFiles[0];
        if (file.type !== 'image/png' && file.type != 'image/jpeg') {
            this.context.handleError(new Error(`Unsupported file type '${file.type}'`));
            return;
        }

        this.setState({
            dropzoneFile: file,
            editing: true,
            editorScale: 1,
            editorScaleAdjusted: 1,
            editorRotation: 0,
            editorPosition: undefined
        });
    };

    protected handleFileDialogOpen = () => {
        this.setState({ dropzoneFile: undefined, logoPreview: undefined });
    };

    protected removeLogoFile = () => {
        this.setState({ dropzoneFile: undefined, logoPreview: undefined, editing: false });
    };

    protected rotateLeft = () => {
        const editorRotation = this.state.editorRotation - 90;
        this.setState({ editorRotation });
    };

    protected rotateRight = () => {
        const editorRotation = this.state.editorRotation + 90;
        this.setState({ editorRotation });
    };

    protected handleEditorScaleChange = (event: React.ChangeEvent<{}>, value: number | number[]) => {
        const editorScale = (typeof value === 'number') ? value : value[0];
        const editorScaleAdjusted = this.adjustScale(editorScale);
        this.setState({ editorScale, editorScaleAdjusted });
    };

    protected handleCancelEditLogo = () => {
        const editorScale = this.state.prevEditorScale;
        const editorScaleAdjusted = this.adjustScale(editorScale);
        const editorRotation = this.state.prevEditorRotation;
        const editorPosition = this.state.prevEditorPosition;
        this.setState({ editorScale, editorScaleAdjusted, editorRotation, editorPosition, editing: false });
    };

    protected handleSaveLogo = () => {
        const canvasScaled = this.editor.current?.getImageScaledToCanvas();
        if (canvasScaled) {
            const dataUrl = canvasScaled.toDataURL();
            this.setState({ logoPreview: dataUrl, editing: false });
            if (this.state.newDetails) {
                const newDetails = this.state.newDetails;
                newDetails.logo = this.state.dropzoneFile!.name;
                const prefix = 'data:image/png;base64,';
                newDetails.logoBytes = dataUrl.substring(prefix.length);
                this.setState({ newDetails });
            }
        }
    };

    protected adjustScale = (x: number) => {
        return x < 1 ? (0.5 + (x / 2)) : x;
    };

    protected percentageLabelFormat = (value: number) => {
        return `${Math.round(value * 100)}%`;
    };

    protected deleteLogo = () => {
        this.setState({ logoPreview: undefined });
        if (this.state.newDetails) {
            const newDetails = this.state.newDetails;
            newDetails.logo = undefined;
            newDetails.logoBytes = undefined;
            this.setState({ newDetails });
        }
    };

    protected editLogo = () => {
        const prevEditorScale = this.state.editorScale;
        const prevEditorRotation = this.state.editorRotation;
        const prevEditorPosition = this.state.editorPosition;

        this.setState({
            prevEditorScale,
            prevEditorRotation,
            prevEditorPosition,
            editing: true
        });
    };

    protected handleEditorPositionChange = (editorPosition: Position) => {
        this.setState({ editorPosition });
    };

    protected isDropzoneDisabled = (): boolean => {
        return this.state.logoPreview !== undefined || (this.state.newDetails !== undefined && this.state.newDetails.logo !== undefined);
    };

    protected handleClose = () => {
        this.setState({ detailsUpdated: false });
    };

    render() {
        if (!this.state.newDetails) {
            return <DelayedLoadIndicator loading={this.state.loading} />;
        }

        const { classes } = this.props;
        const successClass = this.context.pageSettings.themeType === 'dark' ? classes.successDark : classes.successLight;

        return <React.Fragment>
            <Dialog
                open={this.state.editing}
                onClose={() => this.setState({ editing: false })} >
                <DialogTitle >
                    Edit namespace logo
                </DialogTitle>
                <DialogContent className={classes.dialogOverflow}>
                    <Grid container spacing={1}>
                        <Grid item container justify='center'>
                            <AvatarEditor
                                ref={this.editor}
                                image={this.state.dropzoneFile || ''}
                                width={120}
                                height={120}
                                border={8}
                                color={[255, 255, 255, 0.6]}
                                scale={this.state.editorScaleAdjusted}
                                rotate={this.state.editorRotation}
                                position={this.state.editorPosition}
                                onPositionChange={this.handleEditorPositionChange}>
                            </AvatarEditor>
                        </Grid>
                        <Grid item container spacing={2}>
                            <Grid item><ZoomOutIcon/></Grid>
                            <Grid item xs>
                                <Slider
                                    min={0}
                                    max={2}
                                    step={0.01}
                                    scale={this.adjustScale}
                                    color='secondary'
                                    valueLabelDisplay='auto'
                                    valueLabelFormat={this.percentageLabelFormat}
                                    value={this.state.editorScale}
                                    onChange={this.handleEditorScaleChange}/>
                            </Grid>
                            <Grid item><ZoomInIcon/></Grid>
                        </Grid>
                        <Grid item container spacing={2} justify='center'>
                            <Grid item>
                                <IconButton onClick={this.rotateLeft} title='Rotate image counter-clockwise'>
                                    <RotateLeftIcon/>
                                </IconButton>
                            </Grid>
                            <Grid item>
                                <IconButton onClick={this.rotateRight} title='Rotate image clockwise'>
                                    <RotateRightIcon/>
                                </IconButton>
                            </Grid>
                        </Grid>
                    </Grid>
                </DialogContent>
                <DialogActions>
                    <Button
                        variant='contained'
                        color='primary'
                        onClick={this.handleCancelEditLogo} >
                        Cancel
                    </Button>
                    <Button
                        autoFocus
                        onClick={this.handleSaveLogo} >
                        Save logo
                    </Button>
                </DialogActions>
            </Dialog>
            <Grid container spacing={2}>
                <Grid item container>
                    <Typography variant='h5'>Details</Typography>
                </Grid>
                {   this.state.detailsUpdated
                    ? <Grid item container spacing={2} alignItems='center'>
                        <Paper className={`${classes.banner} ${successClass}`}>
                            <CheckCircleOutline fontSize='large' />
                            <Typography variant='body1' className={classes.bannerText}>The details of the &ldquo;{this.state.bannerNamespaceName}&rdquo; namespace were updated.</Typography>
                            <IconButton className={classes.closeButton} onClick={this.handleClose}><CloseIcon /></IconButton>
                        </Paper>
                    </Grid>
                    : null
                }
                <Grid item container spacing={2} alignItems='center'>
                    <Grid item>
                        <Dropzone
                            onDrop={this.handleDrop}
                            onFileDialogOpen={this.handleFileDialogOpen}
                            noClick={this.isDropzoneDisabled()}
                            noKeyboard={this.isDropzoneDisabled()}
                            noDrag={this.isDropzoneDisabled()}
                            maxFiles={1}
                            maxSize={4 * 1024 * 1024}>
                            {({ getRootProps, getInputProps, isFocused, isDragAccept, isDragReject }) => (
                                <section className={classes.avatarSection}>
                                    <div {...getRootProps({ className: clsx(
                                        classes.dropzone,
                                        isFocused && classes.dropzoneFocused,
                                        isDragAccept && classes.dropzoneAccept,
                                        isDragReject && classes.dropzoneReject
                                        ) })}>
                                        <input {...getInputProps({ accept: 'image/jpeg,image/png', multiple: false })} />
                                        <img src={this.state.logoPreview || this.state.newDetails?.logo || this.context.pageSettings.urls.extensionDefaultIcon}/>
                                    </div>
                                    { this.state.logoPreview || this.state.newDetails?.logo ?
                                        <div className={classes.avatarButtons}>
                                            { this.state.logoPreview ?
                                                <IconButton onClick={this.editLogo} title='Edit logo'>
                                                    <EditIcon/>
                                                </IconButton>
                                                : null
                                            }
                                            <IconButton onClick={this.deleteLogo} title='Delete logo'>
                                                <DeleteIcon/>
                                            </IconButton>
                                        </div>
                                        : null
                                    }
                                </section>
                            )}
                        </Dropzone>
                    </Grid>
                    <Grid item xs>
                        <Grid container spacing={2}>
                            <Grid item container>
                                <TextField fullWidth
                                    label='Display name'
                                    name={UserNamespaceDetailsComponent.INPUT_DISPLAY_NAME}
                                    value={ this.state.newDetails.displayName || '' }
                                    onChange={ this.handleInputChange } />
                            </Grid>
                            <Grid item container>
                                <TextField fullWidth
                                    multiline
                                    rows={3}
                                    margin='dense'
                                    variant='outlined'
                                    label='Description'
                                    name={UserNamespaceDetailsComponent.INPUT_DESCRIPTION}
                                    value={ this.state.newDetails.description || '' }
                                    onChange={ this.handleInputChange } />
                            </Grid>
                        </Grid>
                    </Grid>
                </Grid>
                <Grid item container>
                    <TextField fullWidth
                            label='Website'
                            type='url'
                            name={UserNamespaceDetailsComponent.INPUT_WEBSITE}
                            value={ this.state.newDetails.website || '' }
                            onChange={ this.handleInputChange } />
                </Grid>
                <Grid item container>
                    <TextField fullWidth
                        label='Support link'
                        type='url'
                        name={UserNamespaceDetailsComponent.INPUT_SUPPORT_LINK}
                        value={ this.state.newDetails.supportLink || '' }
                        onChange={ this.handleInputChange } />
                </Grid>
                <Grid item container>
                    <Grid container spacing={2} alignItems='flex-end'>
                        <Grid item>
                            <LinkedInIcon titleAccess='LinkedIn profile'/>
                        </Grid>
                        <Grid item xs>
                            <TextField fullWidth
                                name={UserNamespaceDetailsComponent.INPUT_LINKEDIN}
                                value={ this.state.newDetails.socialLinks.linkedin || '' }
                                onChange={ this.handleInputChange }
                                InputProps={{ startAdornment: <InputAdornment position='start'>
                                    <Select value={ this.state.linkedInAccountType } onChange={ this.handleSelectChange } disableUnderline>
                                        <MenuItem value={ UserNamespaceDetailsComponent.LINKED_IN_PERSONAL } selected={true}><PersonIcon className={classes.textSecondary} titleAccess='Personal profile'/></MenuItem>
                                        <MenuItem value={ UserNamespaceDetailsComponent.LINKED_IN_COMPANY }><BusinessIcon className={classes.textSecondary} titleAccess='Company profile'/></MenuItem>
                                    </Select>
                                    <Typography color='textSecondary'>https://www.linkedin.com/{this.state.linkedInAccountType}/</Typography>
                                </InputAdornment> }}/>
                        </Grid>
                    </Grid>
                    <Grid container spacing={2} alignItems='flex-end'>
                        <Grid item>
                            <GitHubIcon titleAccess='GitHub profile'/>
                        </Grid>
                        <Grid item xs>
                            <TextField fullWidth
                                name={UserNamespaceDetailsComponent.INPUT_GITHUB}
                                value={ this.state.newDetails.socialLinks.github || '' }
                                onChange={ this.handleInputChange }
                                InputProps={{ startAdornment: <InputAdornment position='start'>https://github.com/</InputAdornment> }}/>
                        </Grid>
                    </Grid>
                    <Grid container spacing={2} alignItems='flex-end'>
                        <Grid item>
                            <TwitterIcon titleAccess='Twitter profile'/>
                        </Grid>
                        <Grid item xs>
                            <TextField fullWidth
                                name={UserNamespaceDetailsComponent.INPUT_TWITTER}
                                value={ this.state.newDetails.socialLinks.twitter || '' }
                                onChange={ this.handleInputChange }
                                InputProps={{ startAdornment: <InputAdornment position='start'>https://twitter.com/</InputAdornment> }}/>
                        </Grid>
                    </Grid>
                </Grid>
                <Grid item container justify='flex-end'>
                    <Button className={classes.addButton} variant='outlined' disabled={_.isEqual(this.state.currentDetails, this.state.newDetails)} onClick={this.setNamespaceDetails}>
                        Save Namespace Details
                    </Button>
                </Grid>
            </Grid>
        </React.Fragment>;
     }
 }

export namespace UserNamespaceDetails {
    export interface Props extends WithStyles<typeof detailStyles> {
        namespace: Namespace;
    }
    export interface State {
        currentDetails?: NamespaceDetails;
        newDetails?: NamespaceDetails;
        detailsUpdated: boolean;
        bannerNamespaceName: string;
        loading: boolean;
        dropzoneFile?: File;
        logoPreview?: string;
        editing: boolean;
        editorScale: number;
        editorScaleAdjusted: number;
        editorRotation: number;
        editorPosition?: Position;
        prevEditorScale: number;
        prevEditorRotation: number;
        prevEditorPosition?: Position;
        linkedInAccountType: string;
    }
}

 export const UserNamespaceDetails = withStyles(detailStyles)(UserNamespaceDetailsComponent);