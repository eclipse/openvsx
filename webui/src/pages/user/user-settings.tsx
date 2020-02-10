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
import { ExtensionRegistryService } from "../../extension-registry-service";
import { UserData } from "../../extension-registry-types";
import { UserSettingTabs } from "./user-setting-tabs";
import { UserSettingsTokens } from "./user-settings-tokens";
import { UserSettingsProfile } from "./user-settings-profile";

export namespace UserSettingsRoutes {
    export const MAIN = createRoute(['user-settings']);
    export const MAIN_W_TAB_PARAM = createRoute([MAIN, ':tab']);
    export const PROFILE_ROUTE = createRoute([MAIN, 'profile']);
    export const TOKENS_ROUTE = createRoute([MAIN, 'tokens']);
}

const profileStyles = (theme: Theme) => createStyles({

});

class UserSettingsComponent extends React.Component<UserSettingsComponent.Props> {

    render() {
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
        user?: UserData;
        service: ExtensionRegistryService;
    }
}

export const UserSettings = withStyles(profileStyles)(UserSettingsComponent);
