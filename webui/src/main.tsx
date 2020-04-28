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
import { ErrorDialog } from './custom-mui-components/error-dialog';
import "../src/main.css";

const mainStyles = (theme: Theme) => createStyles({
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
    main: {
        flex: 1,
        overflow: 'auto'
    },
    footer: {
        backgroundColor: theme.palette.primary.dark,
        padding: `${theme.spacing(1.5)}px ${theme.spacing(3)}px`,
        [theme.breakpoints.down('sm')]: {
            padding: `${theme.spacing(1)}px ${theme.spacing(2)}px`
        }
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

    componentDidMount() {
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

    setError = (err: {}) => {
        const error = handleError(err);
        this.setState({ error, isErrorDialogOpen: true });
    }

    handleDialogClose = () => {
        this.setState({ isErrorDialogOpen: false });
    }

    render() {
        const {
            toolbarContent: ToolbarContent,
            footerContent: FooterContent,
            additionalRoutes: AdditionalRoutes
         } = this.props.pageSettings;
        return <React.Fragment>
            <CssBaseline />
            <Box display='flex' flexDirection='column' height='100%'>
                <AppBar position='sticky'>
                    <Toolbar classes={{ root: this.props.classes.spreadHorizontally }}>
                        <Box className={this.props.classes.alignVertically}>
                            {ToolbarContent ? <ToolbarContent /> : null}
                        </Box>
                        <Box display='flex' alignItems='center'>
                            {
                                this.state.user ?
                                    <UserAvatar
                                        user={this.state.user}
                                        service={this.props.service}
                                        setError={this.setError}
                                    />
                                    :
                                    <IconButton href={this.props.service.getLoginUrl()} title="Log In">
                                        <AccountBoxIcon />
                                    </IconButton>
                            }
                        </Box>
                    </Toolbar>
                </AppBar>
                <Box className={this.props.classes.main}>
                    <Switch>
                        <Route exact path={[ExtensionListRoutes.MAIN]}
                            render={routeProps =>
                                <ExtensionListContainer
                                    {...routeProps}
                                    service={this.props.service}
                                    pageSettings={this.props.pageSettings}
                                    setError={this.setError}
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
                                    setError={this.setError}
                                />
                            } />
                        <Route path={ExtensionDetailRoutes.MAIN}
                            render={routeProps =>
                                <ExtensionDetail
                                    {...routeProps}
                                    user={this.state.user}
                                    service={this.props.service}
                                    pageSettings={this.props.pageSettings}
                                    setError={this.setError}
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
                        <footer className={this.props.classes.footer}>
                            <Box className={`${this.props.classes.spreadHorizontally} ${this.props.classes.alignVertically}`}>
                                <FooterContent />
                            </Box>
                        </footer>
                        : null
                }
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
        userLoading: boolean;
        error: string;
        isErrorDialogOpen: boolean
    }
}

export const Main = withStyles(mainStyles)(MainComponent);
