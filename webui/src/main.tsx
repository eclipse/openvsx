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

    private scrollListener?: () => void;
    private scrollTimeout?: number;

    constructor(props: MainComponent.Props) {
        super(props);

        this.state = {
            userLoading: true,
            showNormalFooter: false,
            showFixedFooter: true,
            fadeFooter: false,
            error: '',
            isErrorDialogOpen: false
        };
    }

    componentDidMount() {
        this.scrollListener = () => {
            const element = document.scrollingElement;
            if (!element) {
                return;
            }
            clearTimeout(this.scrollTimeout);
            const fixFooter = element.scrollTop === 0
                && element.scrollHeight > element.clientHeight + this.getContentPadding();
            if (fixFooter && this.state.showNormalFooter) {
                this.setState({ showNormalFooter: false, showFixedFooter: true, fadeFooter: true });
            } else if (!fixFooter && this.state.showFixedFooter) {
                this.setState({ showNormalFooter: true, fadeFooter: true });
                this.scrollTimeout = setTimeout(
                    () => this.setState({ showFixedFooter: false, fadeFooter: false }),
                    2000
                );
            }
        };
        document.addEventListener('scroll', this.scrollListener);
        this.updateUser();
    }

    componentWillUnmount() {
        clearTimeout(this.scrollTimeout);
        if (this.scrollListener) {
            document.removeEventListener('scroll', this.scrollListener);
        }
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
        const classes = this.props.classes;
        return <React.Fragment>
            <CssBaseline />
            <Box className={classes.main}>
                <AppBar position='sticky'>
                    <Toolbar classes={{ root: classes.spreadHorizontally }}>
                        <Box className={classes.alignVertically}>
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
                <Box pb={`${this.getContentPadding()}px`}>
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
                    FooterContent && this.state.showNormalFooter ?
                        <footer className={classes.footer}>
                            <FooterContent />
                        </footer>
                        : null
                }
                {
                    FooterContent && this.state.showFixedFooter ?
                        <footer className={
                            this.state.fadeFooter ?
                            `${classes.footer} ${classes.fixed} ${
                                this.state.showNormalFooter ? classes.fadeOut : classes.fadeIn
                            }`
                            :
                            `${classes.footer} ${classes.fixed}`
                        }>
                            <FooterContent />
                        </footer>
                        : null
                }
            </Box>
        </React.Fragment>;
    }

    protected getContentPadding(): number {
        const metrics = this.props.pageSettings.metrics;
        if (metrics && metrics.maxFooterHeight) {
            return metrics.maxFooterHeight + 8;
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
        showNormalFooter: boolean;
        showFixedFooter: boolean;
        fadeFooter: boolean;
        error: string;
        isErrorDialogOpen: boolean
    }
}

export const Main = withStyles(mainStyles)(MainComponent);
