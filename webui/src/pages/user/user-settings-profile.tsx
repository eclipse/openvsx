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
import { Theme, createStyles, WithStyles, withStyles, Grid, Typography, Avatar, Box, Link, Button } from '@material-ui/core';
import { Timestamp } from '../../components/timestamp';
import { UserData } from '../../extension-registry-types';
import { ExtensionRegistryService } from '../../extension-registry-service';
import { createAbsoluteURL } from '../../utils';
import { ErrorResponse } from '../../server-request';

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

    render(): React.ReactNode {
        const user = this.props.user;
        return <React.Fragment>
            <Grid container className={this.props.classes.profile}>
                <Grid item>
                    <Typography variant='h5' gutterBottom>Profile</Typography>
                    <Typography variant='body1'>Login name: {user.loginName}</Typography>
                    <Typography variant='body1'>Full name: {user.fullName}</Typography>
                </Grid>
                <Grid item>
                    <Avatar classes={{ root: this.props.classes.avatar }} variant='rounded' src={user.avatarUrl} />
                </Grid>
            </Grid>
            {this.renderPublisherAgreement()}
        </React.Fragment>;
    }

    protected renderPublisherAgreement(): React.ReactNode {
        const user = this.props.user;
        if (!user.publisherAgreement) {
            return null;
        }
        if (user.publisherAgreement === 'signed') {
            return <Box mt={3}>
                <Typography variant='body1'>
                    {
                        user.publisherAgreementTimestamp
                        ? <React.Fragment>You signed the Eclipse publisher agreement <Timestamp value={user.publisherAgreementTimestamp}/>.</React.Fragment>
                        : 'You signed the Eclipse publisher agreement.'
                    }
                </Typography>
            </Box>;
        }
        if (!user.additionalLogins || !user.additionalLogins.find(login => login.provider === 'eclipse')) {
            return <Box mt={3}>
                <Typography variant='body1'>
                    You need to sign a publisher agreement before you can publish any extension to this registry.
                    To start the signing process, please log in with an Eclipse Foundation account.
                </Typography>
                <Link href={createAbsoluteURL([this.props.service.serverUrl, 'oauth2', 'authorization', 'eclipse'])}>
                    <Button variant='outlined' color='secondary'>
                        Log in with Eclipse
                    </Button>
                </Link>
            </Box>;
        }
        return <Box mt={3}>
            <Typography variant='h3'>Publisher Agreement</Typography>
            <Typography variant='body1'>Bla bla bla.</Typography>
            <Button
                variant='outlined'
                color='secondary'
                onClick={() => this.signPublisherAgreement()} >
                I agree
            </Button>
        </Box>;
    }

    protected async signPublisherAgreement(): Promise<void> {
        try {
            await this.props.service.signPublisherAgreement();
            // TODO update user data
        } catch (err) {
            this.props.handleError(err);
        }
    }
}

export namespace UserSettingsProfileComponent {
    export interface Props extends WithStyles<typeof profileStyle> {
        user: UserData;
        service: ExtensionRegistryService;
        handleError: (err: Error | Partial<ErrorResponse>) => void;
    }

    export interface State {

    }
}

export const UserSettingsProfile = withStyles(profileStyle)(UserSettingsProfileComponent);