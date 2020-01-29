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
import { Theme, createStyles, WithStyles, withStyles, Grid, Typography, Avatar } from "@material-ui/core";
import { UserData } from "../../extension-registry-types";
import { ExtensionRegistryService } from "../../extension-registry-service";

const profileStyle = (theme: Theme) => createStyles({
    avatar: {
        width: '150px',
        height: '150px'
    }
});

class UserSettingsProfileComponent extends React.Component<UserSettingsProfileComponent.Props, UserSettingsProfileComponent.State> {

    constructor(props: UserSettingsProfileComponent.Props) {
        super(props);

        this.state = {};
    }

    render() {
        return <React.Fragment>
            <Grid container>
                <Grid item md={9}>
                    <Typography variant='h5' gutterBottom>Profile</Typography>
                    <Typography variant='body1'>Username: {this.props.user.name}</Typography>
                </Grid>
                <Grid item md={3}>
                    <Avatar classes={{ root: this.props.classes.avatar }} variant='rounded' src={this.props.user.avatarUrl} />
                </Grid>
            </Grid>
        </React.Fragment>;
    }
}

export namespace UserSettingsProfileComponent {
    export interface Props extends WithStyles<typeof profileStyle> {
        user: UserData
        service: ExtensionRegistryService
    }

    export interface State {

    }
}

export const UserSettingsProfile = withStyles(profileStyle)(UserSettingsProfileComponent);