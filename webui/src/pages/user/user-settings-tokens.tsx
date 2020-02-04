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
import { Theme, createStyles, WithStyles, withStyles, Typography, Box, Paper, Button } from "@material-ui/core";
import { UserData, PersonalAccessToken } from "../../extension-registry-types";
import { ExtensionRegistryService } from "../../extension-registry-service";
import { GenerateTokenDialog } from "./generate-token-dialog";

const tokensStyle = (theme: Theme) => createStyles({
    boldText: {
        fontWeight: 'bold'
    },
    deleteBtn: {
        color: theme.palette.error.main
    }
});

class UserSettingsTokensComponent extends React.Component<UserSettingsTokensComponent.Props, UserSettingsTokensComponent.State> {

    constructor(props: UserSettingsTokensComponent.Props) {
        super(props);

        this.state = {
            tokens: []
        };
    }

    componentDidMount() {
        this.updateTokens();
    }

    protected async updateTokens() {
        const tokens = await this.props.service.getTokens(this.props.user.tokensUrl);
        this.setState({ tokens });
    }

    protected handleDelete = async (token: PersonalAccessToken) => {
        await this.props.service.deleteToken(this.props.user.deleteTokenUrl, token);
        this.updateTokens();
    }

    protected handleDeleteAll = async () => {
        await Promise.all(this.state.tokens.map(token =>
            this.props.service.deleteToken(this.props.user.deleteTokenUrl, token)
        ));
        this.updateTokens();
    }

    protected handleTokenGenerated = () => {
        this.updateTokens();
    }

    render() {
        return <React.Fragment>
            <Box display='flex' justifyContent='space-between'>
                <Box>
                    <Typography variant='h5' gutterBottom>Tokens</Typography>
                </Box>
                <Box display='flex'>
                    <Box mr={1}>
                        <GenerateTokenDialog handleTokenGenerated={this.handleTokenGenerated} service={this.props.service} user={this.props.user} />
                    </Box>
                    <Box>
                        <Button variant='outlined' onClick={this.handleDeleteAll} classes={{ root: this.props.classes.deleteBtn }}>Delete all</Button>
                    </Box>
                </Box>
            </Box>
            <Box my={2}>
                <Typography variant='body1'>
                    {
                        this.state.tokens.length ? 'Tokens you have generated.' : 'There are no tokens generated.'
                    }
                </Typography>
            </Box>
            <Box>
                <Paper>
                    {
                        this.state.tokens.map(token => {
                            return <Box key={'token:' + token.value} p={2} display='flex' justifyContent='space-between'>
                                <Box display='flex' alignItems='center'>
                                    <Typography classes={{ root: this.props.classes.boldText }}>{token.value}</Typography>
                                </Box>
                                <Button
                                    variant='outlined'
                                    onClick={() => this.handleDelete(token)}
                                    classes={{ root: this.props.classes.deleteBtn }}>
                                    Delete
                                </Button>
                            </Box>;
                        })
                    }
                </Paper>
            </Box>
        </React.Fragment>;
    }
}

export namespace UserSettingsTokensComponent {
    export interface Props extends WithStyles<typeof tokensStyle> {
        user: UserData
        service: ExtensionRegistryService
    }

    export interface State {
        tokens: PersonalAccessToken[]
    }
}

export const UserSettingsTokens = withStyles(tokensStyle)(UserSettingsTokensComponent);