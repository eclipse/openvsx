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
import BrokenImageIcon from '@material-ui/icons/BrokenImage';
import { Route, Link as RouteLink, Switch } from 'react-router-dom';
import { ExtensionListContainer, ExtensionListRoutes } from './pages/extension-list/extension-list-container';
import { UserSettings, UserSettingsRoutes } from './pages/user/user-settings';
import { ExtensionDetailRoutes, ExtensionDetail } from './pages/extension-detail/extension-detail';
import { UserAvatar } from './pages/user/avatar';
import { Optional } from './custom-mui-components/optional';
import { ExtensionRegistryService } from './extension-registry-service';
import { UserData, isError } from './extension-registry-types';
import { PageSettings } from './page-settings';
import { handleError } from './utils';

const mainStyles = (theme: Theme) => createStyles({
    link: {
        textDecoration: 'none',
        color: theme.palette.text.primary
    },
    toolbar: {
        justifyContent: 'space-between'
    },
    toolbarLogo: {
        width: 'auto',
        height: '40px',
        marginTop: '6px',
        marginRight: theme.spacing(2)
    },
    alignVertically: {
        display: 'flex',
        alignItems: 'center'
    },
    footer: {
        backgroundColor: theme.palette.primary.contrastText,
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
                            <RouteLink to={ExtensionListRoutes.MAIN} className={this.props.classes.link}>
                                <Box className={this.props.classes.alignVertically}>
                                    <Optional enabled={Boolean(this.props.pageSettings.logoURL)}>
                                        <img src={this.props.pageSettings.logoURL}
                                            className={this.props.classes.toolbarLogo}
                                            alt={this.props.pageSettings.toolbarText}/>
                                    </Optional>
                                    <Optional enabled={Boolean(this.props.pageSettings.toolbarText)}>
                                        <Typography variant='h6' noWrap>{this.props.pageSettings.toolbarText}</Typography>
                                    </Optional>
                                </Box>
                            </RouteLink>
                        </Box>
                        <Box display='flex' alignItems='center'>
                            {
                                this.state.user ?
                                    <UserAvatar
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
                        <Route exact path={[ExtensionListRoutes.MAIN]}
                            render={routeProps =>
                                <ExtensionListContainer
                                    {...routeProps}
                                    service={this.props.service}
                                    pageSettings={this.props.pageSettings} />
                            } />
                        <Route path={UserSettingsRoutes.MAIN}
                            render={routeProps =>
                                <UserSettings
                                    {...routeProps}
                                    user={this.state.user}
                                    service={this.props.service}
                                    pageSettings={this.props.pageSettings} />
                            } />
                        <Route path={ExtensionDetailRoutes.MAIN}
                            render={routeProps =>
                                <ExtensionDetail
                                    {...routeProps}
                                    user={this.state.user}
                                    service={this.props.service}
                                    pageSettings={this.props.pageSettings} />
                            } />
                        <Route path='*'>
                            <Container>
                                <Box height='30vh' display='flex' flexWrap='wrap' justifyContent='center' alignItems='center'>
                                    <Typography variant='h3'>Oooups...this is a 404 page.</Typography>
                                    <BrokenImageIcon style={{ fontSize: '4rem', flexBasis: '100%' }}/>
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
        pageSettings: PageSettings;
    }

    export interface State {
        user?: UserData;
    }
}

export const Main = withStyles(mainStyles)(MainComponent);
