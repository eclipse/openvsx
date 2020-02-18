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
import { Container, AppBar, Toolbar, Typography, IconButton, CssBaseline, Box, Theme, Link } from '@material-ui/core';
import { WithStyles, createStyles, withStyles } from '@material-ui/styles';
import AccountBoxIcon from '@material-ui/icons/AccountBox';
import GitHubIcon from '@material-ui/icons/GitHub';
import { Route, Link as RouteLink, Switch } from 'react-router-dom';
import { ExtensionListContainer, ExtensionListRoutes } from './pages/extension-list/extension-list-container';
import { UserSettings, UserSettingsRoutes } from './pages/user/user-settings';
import { ExtensionDetailRoutes, ExtensionDetail } from './pages/extension-detail/extension-detail';
import { ExtensionRegistryAvatar } from './pages/user/avatar';
import { ExtensionRegistryService } from './extension-registry-service';
import { UserData, isError } from './extension-registry-types';
import { handleError } from './utils';

export namespace ExtensionRegistryPages {
    export const EXTENSION_REGISTRY = 'extension-registry';
    export const EXTENSION_UPDATE = 'extension-update';
    export const LOGIN = 'login';
    export const USER_PROFILE = 'user-profile';
    export const USER_REGISTRY = 'user-registry';
}

const mainStyles = (theme: Theme) => createStyles({
    link: {
        textDecoration: 'none',
        color: theme.palette.text.primary
    },
    toolbar: {
        justifyContent: 'space-between'
    },
    footer: {
        backgroundColor: theme.palette.secondary.contrastText,
        padding: theme.spacing(2),
        marginTop: theme.spacing(5)
    },
    footerBox: {
        display: 'flex',
        alignItems: 'center',
        fontSize: '1.1rem'
    }
});

class MainComponent extends React.Component<MainComponent.Props, MainComponent.State> {

    constructor(props: MainComponent.Props) {
        super(props);

        this.state = {};
    }

    componentDidMount() {
        this.updateUser();
    }

    protected async updateUser() {
        try {
            const user = await this.props.service.getUser();
            if (isError(user)) {
                this.setState({ user: undefined });
            } else {
                this.setState({ user });
            }
        } catch (err) {
            handleError(err);
        }
    }

    render() {
        return <React.Fragment>
            <CssBaseline />
            <Box display='flex' flexDirection='column' minHeight='100vh'>
                <AppBar position='sticky'>
                    <Toolbar classes={{ root: this.props.classes.toolbar }}>
                        <Box>
                            <RouteLink to={ExtensionListRoutes.EXTENSION_LIST} className={this.props.classes.link}>
                                <Box display='flex'>
                                    <Box display='flex' alignItems='center' marginRight={1}>
                                        <img src={this.props.logoURL} style={{
                                            width: 'auto',
                                            height: 25,
                                            paddingRight: 10
                                        }}/>
                                    </Box>
                                    <Typography variant='h6' noWrap>{this.props.pageTitle}</Typography>
                                </Box>
                            </RouteLink>
                        </Box>
                        <Box display='flex' alignItems='center'>
                            {
                                this.state.user ?
                                    <ExtensionRegistryAvatar
                                        user={this.state.user}
                                        service={this.props.service} />
                                    :
                                    <IconButton href={this.props.service.getLoginUrl()} title="Log In">
                                        <AccountBoxIcon />
                                    </IconButton>
                            }
                        </Box>
                    </Toolbar>
                </AppBar>
                <Box flex='1'>
                    <Switch>
                        <Route exact path={[ExtensionListRoutes.EXTENSION_LIST]}
                            render={routeProps =>
                                <ExtensionListContainer
                                    {...routeProps}
                                    service={this.props.service}
                                    listHeaderTitle={this.props.listHeaderTitle} />
                            } />
                        <Route path={UserSettingsRoutes.MAIN_W_TAB_PARAM}
                            render={routeProps =>
                                <UserSettings
                                    {...routeProps}
                                    user={this.state.user}
                                    service={this.props.service} />
                            } />
                        <Route path={ExtensionDetailRoutes.EXTENSION_DETAIL_MAIN_ROUTE}
                            render={routeProps =>
                                <ExtensionDetail
                                    {...routeProps}
                                    user={this.state.user}
                                    service={this.props.service} />
                            } />
                        <Route path='*'>
                            <Container>
                                <Box height='90vh' display='flex' justifyContent='center' alignItems='center'>
                                    <Typography variant='h2'>Oooups...this is a 404 page.</Typography>
                                </Box>
                            </Container>
                        </Route>
                    </Switch>
                </Box>
                <footer className={this.props.classes.footer}>
                    <Link target='_blank' href='https://github.com/eclipse/openvsx'>
                        <Box className={this.props.classes.footerBox}>
                            <GitHubIcon/>&nbsp;eclipse/openvsx
                        </Box>
                    </Link>
                </footer>
            </Box>
        </React.Fragment>;
    }
}

export namespace MainComponent {
    export interface Props extends WithStyles<typeof mainStyles> {
        service: ExtensionRegistryService;
        pageTitle: string;
        listHeaderTitle: string;
        logoURL: string;
    }

    export interface State {
        user?: UserData;
    }
}

export const Main = withStyles(mainStyles)(MainComponent);
