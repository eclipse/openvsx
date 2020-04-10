/********************************************************************************
 * Copyright (c) 2019 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import * as React from "react";
import { createStyles, Theme, WithStyles, withStyles, Grid, Container, Box, Typography } from "@material-ui/core";
import { RouteComponentProps, Route } from "react-router-dom";
import { createRoute } from "../../utils";
import { DelayedLoadIndicator } from "../../custom-mui-components/delayed-load-indicator";
import { ExtensionRegistryService } from "../../extension-registry-service";
import { UserData } from "../../extension-registry-types";
import { PageSettings } from "../../page-settings";
import { UserSettingTabs } from "./user-setting-tabs";
import { UserSettingsTokens } from "./user-settings-tokens";
import { UserSettingsProfile } from "./user-settings-profile";

export namespace UserSettingsRoutes {
    export const ROOT = createRoute(['user-settings']);
    export const MAIN = createRoute([ROOT, ':tab']);
    export const PROFILE = createRoute([ROOT, 'profile']);
    export const TOKENS = createRoute([ROOT, 'tokens']);
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
        }
    }
});

class UserSettingsComponent extends React.Component<UserSettingsComponent.Props> {

    componentDidMount() {
        document.title = `Settings – ${this.props.pageSettings.pageTitle}`;
    }

    render() {
        if (this.props.userLoading) {
            return <DelayedLoadIndicator loading={true}/>;
        }
        const user = this.props.user;
        if (!user) {
            return <Container>
                <Box mt={6}>
                    <Typography variant='h6'>You are not logged in!</Typography>
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
                                    <UserSettingsProfile service={this.props.service} user={user} />
                                </Route>
                                <Route path={UserSettingsRoutes.TOKENS}>
                                    <UserSettingsTokens service={this.props.service} user={user} />
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
        user?: UserData;
        userLoading: boolean;
        service: ExtensionRegistryService;
        pageSettings: PageSettings;
    }
}

export const UserSettings = withStyles(profileStyles)(UserSettingsComponent);
