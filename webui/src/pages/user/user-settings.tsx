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
import { createStyles, Theme, WithStyles, withStyles, Grid, Container, Box, Typography, Link } from '@material-ui/core';
import { RouteComponentProps, Route } from 'react-router-dom';
import { createRoute } from '../../utils';
import { DelayedLoadIndicator } from '../../components/delayed-load-indicator';
import { UserSettingTabs } from './user-setting-tabs';
import { UserSettingsTokens } from './user-settings-tokens';
import { UserSettingsProfile } from './user-settings-profile';
import { UserSettingsNamespaces } from './user-settings-namespaces';
import { MainContext } from '../../context';

export namespace UserSettingsRoutes {
    export const ROOT = createRoute(['user-settings']);
    export const MAIN = createRoute([ROOT, ':tab']);
    export const PROFILE = createRoute([ROOT, 'profile']);
    export const TOKENS = createRoute([ROOT, 'tokens']);
    export const NAMESPACES = createRoute([ROOT, 'namespaces']);
}

const profileStyles = (theme: Theme) => createStyles({
    container: {
        [theme.breakpoints.down('md')]: {
            flexDirection: 'column'
        }
    },
    tabs: {
        [theme.breakpoints.down('lg')]: {
            marginBottom: '3rem'
        },
    },
    info: {
        [theme.breakpoints.up('lg')]: {
            paddingTop: '.5rem',
            paddingLeft: '3rem',
            flex: '1'
        },
        [theme.breakpoints.down('md')]: {
            width: '100%'
        }
    }
});

class UserSettingsComponent extends React.Component<UserSettingsComponent.Props> {

    static contextType = MainContext;
    declare context: MainContext;

    componentDidMount() {
        document.title = `Settings – ${this.context.pageSettings.pageTitle}`;
    }

    render() {
        if (this.props.userLoading) {
            return <DelayedLoadIndicator loading={true} />;
        }
        const user = this.context.user;
        if (!user) {
            return <Container>
                <Box mt={6}>
                    <Typography variant='h4'>Not Logged In</Typography>
                    <Box mt={2}>
                        <Typography variant='body1'>
                            Please <Link color='secondary' href={this.context.service.getLoginUrl()}>log in with GitHub</Link> to
                            access your account settings.
                        </Typography>
                    </Box>
                </Box>
            </Container>;
        }
        return <React.Fragment>
            <Container>
                <Box mt={6}>
                    <Grid container className={this.props.classes.container}>
                        <Grid item className={this.props.classes.tabs}>
                            <UserSettingTabs {...this.props} />
                        </Grid>
                        <Grid item className={this.props.classes.info}>
                            <Box>
                                <Route path={UserSettingsRoutes.PROFILE}>
                                    <UserSettingsProfile user={user} />
                                </Route>
                                <Route path={UserSettingsRoutes.TOKENS}>
                                    <UserSettingsTokens />
                                </Route>
                                <Route path={UserSettingsRoutes.NAMESPACES}>
                                    <UserSettingsNamespaces />
                                </Route>
                            </Box>
                        </Grid>
                    </Grid>
                </Box>
            </Container>
        </React.Fragment>;
    }
}

export namespace UserSettingsComponent {
    export interface Props extends WithStyles<typeof profileStyles>, RouteComponentProps {
        userLoading: boolean;
    }
}

export const UserSettings = withStyles(profileStyles)(UserSettingsComponent);
