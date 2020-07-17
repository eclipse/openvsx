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
import { Theme, createStyles, WithStyles, withStyles, Grid, Typography, Avatar } from '@material-ui/core';
import { UserData } from '../../extension-registry-types';
import { ExtensionRegistryService } from '../../extension-registry-service';

const profileStyle = (theme: Theme) => createStyles({
    profile: {
        [theme.breakpoints.up('lg')]: {
            justifyContent: 'space-between'
        },
        ['@media(max-width: 1040px)']: {
            flexDirection: 'row-reverse',
            justifyContent: 'flex-end',
            '& > div:first-of-type': {
                marginLeft: '2rem'
            }
        },
        [theme.breakpoints.down('sm')]: {
            textAlign: 'center',
            flexDirection: 'column-reverse',
            '& > div:first-of-type': {
                marginLeft: '0',
                marginTop: '2rem'
            }
        }
    },
    avatar: {
        width: '150px',
        height: '150px',
        [theme.breakpoints.down('sm')]: {
            margin: '0 auto',
        }
    }
});

class UserSettingsProfileComponent extends React.Component<UserSettingsProfileComponent.Props, UserSettingsProfileComponent.State> {

    constructor(props: UserSettingsProfileComponent.Props) {
        super(props);

        this.state = {};
    }

    render() {
        return <React.Fragment>
            <Grid container className={this.props.classes.profile}>
                <Grid item>
                    <Typography variant='h5' gutterBottom>Profile</Typography>
                    <Typography variant='body1'>Login name: {this.props.user.loginName}</Typography>
                    <Typography variant='body1'>Full name: {this.props.user.fullName}</Typography>
                </Grid>
                <Grid item>
                    <Avatar classes={{ root: this.props.classes.avatar }} variant='rounded' src={this.props.user.avatarUrl} />
                </Grid>
            </Grid>
        </React.Fragment>;
    }
}

export namespace UserSettingsProfileComponent {
    export interface Props extends WithStyles<typeof profileStyle> {
        user: UserData
        service: ExtensionRegistryService
    }

    export interface State {

    }
}

export const UserSettingsProfile = withStyles(profileStyle)(UserSettingsProfileComponent);