/********************************************************************************
 * Copyright (c) 2019 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import * as React from 'react';
import { withStyles, createStyles } from '@material-ui/styles';
import { Theme, WithStyles, Avatar, Popper, Paper, ClickAwayListener, Typography, Box, Grow } from '@material-ui/core';
import ExitToAppIcon from '@material-ui/icons/ExitToAppOutlined';
import { Link } from 'react-router-dom';
import { UserData } from '../extension-registry-types';
import { ExtensionRegistryService } from '../extension-registry-service';
import { createAbsoluteURL } from '../utils';
import { UserSettingsRoutes } from './user/user-settings';
import PopperJS from 'popper.js';

const avatarStyle = (theme: Theme) => createStyles({
    avatar: {
        cursor: 'pointer',
        width: '30px',
        height: '30px'
    },
    link: {
        textDecoration: 'none',
        display: 'flex',
        alignItems: 'center',
        color: theme.palette.text.primary
    }
});

class ExtensionRegistryAvatarComponent extends React.Component<ExtensionRegistryAvatarComponent.Props, ExtensionRegistryAvatarComponent.State> {

    protected avatarButton: HTMLElement | null;
    protected popperRef: PopperJS | null;

    constructor(props: ExtensionRegistryAvatarComponent.Props) {
        super(props);

        this.state = {
            open: false
        };
    }

    protected handleAvatarClick = () => {
        this.setState({ open: !this.state.open });
    }
    protected handleClose = () => this.setState({ open: false });

    componentDidUpdate(prevProps: ExtensionRegistryAvatarComponent.Props, prevState: ExtensionRegistryAvatarComponent.State) {
        if (this.popperRef) {
            this.popperRef.update();
            this.popperRef.update();
        }
    }

    render() {
        return <React.Fragment>
            <Avatar
                onClick={this.handleAvatarClick}
                src={this.props.user.avatarUrl}
                variant='rounded'
                classes={{ root: this.props.classes.avatar }}
                ref={ref => this.avatarButton = ref} />
            <Popper open={this.state.open} anchorEl={this.avatarButton} popperRef={ref => this.popperRef = ref} placement='bottom-end' disablePortal transition>
                {({ TransitionProps, placement }) => (
                    <Grow {...TransitionProps}>
                        <Paper>
                            <ClickAwayListener onClickAway={this.handleClose}>
                                <Box p={1}>
                                    <Typography variant='overline' color='textPrimary'>Logged in as {this.props.user.name}</Typography>
                                    <Box mt={1}>
                                        <Box>
                                            <Link onClick={this.handleClose} to={UserSettingsRoutes.PROFILE_ROUTE} className={this.props.classes.link}>
                                                <Typography variant='button' color='textPrimary'>
                                                    Settings
                                                </Typography>
                                            </Link>
                                        </Box>
                                        <Box>
                                            <a href={createAbsoluteURL([this.props.service.apiUrl, '-', 'user', 'logout'])}
                                                onClick={this.handleClose} className={this.props.classes.link}>
                                                <Typography variant='button'>
                                                    Log Out
                                                </Typography>
                                                <ExitToAppIcon fontSize='small' />
                                            </a>
                                        </Box>
                                    </Box>
                                </Box>
                            </ClickAwayListener>
                        </Paper>
                    </Grow>
                )}
            </Popper>
        </React.Fragment>;
    }
}

export namespace ExtensionRegistryAvatarComponent {
    export interface Props extends WithStyles<typeof avatarStyle> {
        user: UserData
        service: ExtensionRegistryService
    }

    export interface State {
        open: boolean
    }
}

export const ExtensionRegistryAvatar = withStyles(avatarStyle)(ExtensionRegistryAvatarComponent);
