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
import { Container, AppBar, Toolbar, Typography, IconButton, CssBaseline, Box, Theme } from '@material-ui/core';
import { WithStyles, createStyles, withStyles } from '@material-ui/styles';
import AccountBoxIcon from '@material-ui/icons/AccountBox';
import BrokenImageIcon from '@material-ui/icons/BrokenImage';
import { Route, Switch } from 'react-router-dom';
import { ExtensionListContainer, ExtensionListRoutes } from './pages/extension-list/extension-list-container';
import { UserSettings, UserSettingsRoutes } from './pages/user/user-settings';
import { ExtensionDetailRoutes, ExtensionDetail } from './pages/extension-detail/extension-detail';
import { UserAvatar } from './pages/user/avatar';
import { ExtensionRegistryService } from './extension-registry-service';
import { UserData, isError } from './extension-registry-types';
import { PageSettings } from './page-settings';
import { handleError } from './utils';
import { ErrorDialog } from './components/error-dialog';
import '../src/main.css';
import { HeaderMenu } from './header-menu';
import { AdminDashboard, AdminDashboardRoutes } from './pages/admin-dashboard/admin-dashboard';

export const UserContext = React.createContext<UserData | undefined>(undefined);

const mainStyles = (theme: Theme) => createStyles({
    main: {
        display: 'flex',
        flexDirection: 'column',
        position: 'relative',
        minHeight: '100vh'
    },
    link: {
        textDecoration: 'none',
        color: theme.palette.text.primary
    },
    spreadHorizontally: {
        justifyContent: 'space-between'
    },
    alignVertically: {
        display: 'flex',
        alignItems: 'center'
    },
    footer: {
        position: 'absolute',
        bottom: 0,
        width: '100%',
        padding: `${theme.spacing(1.5)}px ${theme.spacing(3)}px`,
        backgroundColor: theme.palette.primary.dark
    },
    fixed: {
        position: 'fixed',
    },
    fadeIn: {
        animation: 'fadein 2s',
        opacity: 1
    },
    fadeOut: {
        animation: 'fadeout 2s',
        opacity: 0
    }
});

class MainComponent extends React.Component<MainComponent.Props, MainComponent.State> {

    constructor(props: MainComponent.Props) {
        super(props);

        this.state = {
            userLoading: true,
            error: '',
            isErrorDialogOpen: false
        };
    }

    componentDidMount(): void {
        this.updateUser();
    }

    protected async updateUser() {
        try {
            const user = await this.props.service.getUser();
            if (isError(user)) {
                this.setState({ user: undefined, userLoading: false });
            } else {
                this.setState({ user, userLoading: false });
            }
        } catch (err) {
            this.setState({ error: handleError(err), isErrorDialogOpen: true, userLoading: false });
        }
    }

    handleError = (err: {}) => {
        const error = handleError(err);
        this.setState({ error, isErrorDialogOpen: true });
    };

    handleDialogClose = () => {
        this.setState({ isErrorDialogOpen: false });
    };

    render(): React.ReactNode {
        const {
            toolbarContent: ToolbarContent,
            footerContent: FooterContent,
            additionalRoutes: AdditionalRoutes
        } = this.props.pageSettings.elements;
        const classes = this.props.classes;
        return <React.Fragment>
            <CssBaseline />
            <UserContext.Provider value={this.state.user}>
                <Switch>
                    <Route path={AdminDashboardRoutes.MAIN}>
                        <AdminDashboard></AdminDashboard>
                    </Route>
                    <Route path='*'>
                        <Box className={classes.main}>
                            <AppBar position='relative'>
                                <Toolbar classes={{ root: classes.spreadHorizontally }}>
                                    <Box className={classes.alignVertically}>
                                        {ToolbarContent ? <ToolbarContent /> : null}
                                    </Box>
                                    <Box className={classes.alignVertically}>
                                        <HeaderMenu pageSettings={this.props.pageSettings} />
                                        {
                                            this.state.user ?
                                                <UserAvatar
                                                    user={this.state.user}
                                                    service={this.props.service}
                                                    setError={this.handleError}
                                                />
                                                :
                                                <IconButton
                                                    href={this.props.service.getLoginUrl()}
                                                    title='Log In'
                                                    aria-label='Log In' >
                                                    <AccountBoxIcon />
                                                </IconButton>
                                        }
                                    </Box>
                                </Toolbar>
                            </AppBar>
                            <Box pb={`${this.getContentPadding()}px`}>
                                <Switch>
                                    <Route exact path={[ExtensionListRoutes.MAIN]}
                                        render={routeProps =>
                                            <ExtensionListContainer
                                                {...routeProps}
                                                service={this.props.service}
                                                pageSettings={this.props.pageSettings}
                                                handleError={this.handleError}
                                            />
                                        } />
                                    <Route path={UserSettingsRoutes.MAIN}
                                        render={routeProps =>
                                            <UserSettings
                                                {...routeProps}
                                                user={this.state.user}
                                                userLoading={this.state.userLoading}
                                                service={this.props.service}
                                                pageSettings={this.props.pageSettings}
                                                handleError={this.handleError}
                                            />
                                        } />
                                    <Route path={ExtensionDetailRoutes.MAIN}
                                        render={routeProps =>
                                            <ExtensionDetail
                                                {...routeProps}
                                                user={this.state.user}
                                                service={this.props.service}
                                                pageSettings={this.props.pageSettings}
                                                handleError={this.handleError}
                                            />
                                        } />
                                    {AdditionalRoutes ? <AdditionalRoutes /> : null}
                                    <Route path='*'>
                                        <Container>
                                            <Box height='30vh' display='flex' flexWrap='wrap' justifyContent='center' alignItems='center'>
                                                <Typography variant='h3'>Oooups...this is a 404 page.</Typography>
                                                <BrokenImageIcon style={{ fontSize: '4rem', flexBasis: '100%' }} />
                                            </Box>
                                        </Container>
                                    </Route>
                                </Switch>
                            </Box>
                            {
                                this.state.error ?
                                    <ErrorDialog
                                        errorMessage={this.state.error}
                                        isErrorDialogOpen={this.state.isErrorDialogOpen}
                                        handleCloseDialog={this.handleDialogClose} />
                                    : null
                            }
                            {
                                FooterContent ?
                                    <footer className={classes.footer}>
                                        <FooterContent />
                                    </footer>
                                    : null
                            }
                        </Box>
                    </Route>
                </Switch>
            </UserContext.Provider>
        </React.Fragment>;
    }

    protected getContentPadding(): number {
        const metrics = this.props.pageSettings.metrics;
        if (metrics && metrics.maxFooterHeight) {
            return metrics.maxFooterHeight + 24;
        } else {
            return 0;
        }
    }
}

export namespace MainComponent {
    export interface Props extends WithStyles<typeof mainStyles> {
        service: ExtensionRegistryService;
        pageSettings: PageSettings;
    }

    export interface State {
        user?: UserData;
        userLoading: boolean;
        error: string;
        isErrorDialogOpen: boolean
    }
}
export const Main = withStyles(mainStyles)(MainComponent);
