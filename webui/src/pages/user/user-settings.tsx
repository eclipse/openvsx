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
import { createURL } from "../../utils";
import { UserSettingTabs } from "./user-setting-tabs";
import { ExtensionRegistryService } from "../../extension-registry-service";
import { UserSettingsTokens } from "./user-settings-tokens";
import { UserSettingsProfile } from "./user-settings-profile";
import { UserData } from "../../extension-registry-types";

export namespace UserSettingsRoutes {
    export const MAIN = createURL(['user-settings']);
    export const MAIN_W_TAB_PARAM = createURL([MAIN, ':tab']);
    export const PROFILE_ROUTE = createURL([MAIN, 'profile']);
    export const TOKENS_ROUTE = createURL([MAIN, 'tokens']);
}

const profileStyles = (theme: Theme) => createStyles({

});

class UserSettingsComponent extends React.Component<UserSettingsComponent.Props, UserSettingsComponent.State> {

    constructor(props: UserSettingsComponent.Props) {
        super(props);

        this.state = {};
    }

    componentDidMount() {
        this.props.service.getUser().then(user => {
            if (UserData.is(user)) {
                this.setState({ user });
            }
        });
    }

    render() {
        const user = this.state.user;
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
                    <Grid container>
                        <Grid item md={2}>
                            <UserSettingTabs {...this.props} />
                        </Grid>
                        <Grid item md={10}>
                            <Box pt={1} pl={6}>
                                <Route path={UserSettingsRoutes.PROFILE_ROUTE}>
                                    <UserSettingsProfile service={this.props.service} user={user} />
                                </Route>
                                <Route path={UserSettingsRoutes.TOKENS_ROUTE}>
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
        service: ExtensionRegistryService
    }
    export interface State {
        user?: UserData
    }
}

export const UserSettings = withStyles(profileStyles)(UserSettingsComponent);