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
import { handleError, toLocalTime } from "../../utils";
import { UserData, PersonalAccessToken } from "../../extension-registry-types";
import { ExtensionRegistryService } from "../../extension-registry-service";
import { GenerateTokenDialog } from "./generate-token-dialog";

const tokensStyle = (theme: Theme) => createStyles({
    description: {
        fontWeight: 'bold',
        overflow: 'hidden',
        textOverflow: 'ellipsis'
    },
    deleteBtn: {
        color: theme.palette.error.main,
        height: 36
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
        try {
            const tokens = await this.props.service.getAccessTokens(this.props.user);
            this.setState({ tokens });
        } catch (err) {
            handleError(err);
        }
    }

    protected handleDelete = async (token: PersonalAccessToken) => {
        try {
            await this.props.service.deleteAccessToken(token);
            this.updateTokens();
        } catch (err) {
            handleError(err);
        }
    }

    protected handleDeleteAll = async () => {
        try {
            await this.props.service.deleteAllAccessTokens(this.state.tokens);
            this.updateTokens();
        } catch (err) {
            handleError(err);
        }
    }

    protected handleTokenGenerated = () => {
        this.updateTokens();
    }

    render() {
        return <React.Fragment>
            <Box display='flex' justifyContent='space-between'>
                <Box>
                    <Typography variant='h5' gutterBottom>Access Tokens</Typography>
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
                    {this.state.tokens.length ? 'Tokens you have generated:' : 'You currently have no tokens.'}
                </Typography>
            </Box>
            <Box>
                <Paper>
                    {this.state.tokens.map(token => this.renderToken(token))}
                </Paper>
            </Box>
        </React.Fragment >;
    }

    protected renderToken(token: PersonalAccessToken): React.ReactNode {
        return <Box key={'token:' + token.id} p={2} display='flex' justifyContent='space-between'>
            <Box alignItems='center' overflow='auto'>
                <Typography classes={{ root: this.props.classes.description }}>{token.description}</Typography>
                <Typography variant='body2'>Created: {toLocalTime(token.createdTimestamp)!.toLocaleString()}</Typography>
                <Typography variant='body2'>Accessed: {token.accessedTimestamp ? toLocalTime(token.accessedTimestamp)!.toLocaleString() : 'never'}</Typography>
            </Box>
            <Box display='flex' alignItems='center'>
                <Button
                    variant='outlined'
                    onClick={() => this.handleDelete(token)}
                    classes={{ root: this.props.classes.deleteBtn }}>
                    Delete
                </Button>
            </Box>
        </Box>;
    }

}

export namespace UserSettingsTokensComponent {
    export interface Props extends WithStyles<typeof tokensStyle> {
        user: UserData;
        service: ExtensionRegistryService;
    }

    export interface State {
        tokens: PersonalAccessToken[];
    }
}

export const UserSettingsTokens = withStyles(tokensStyle)(UserSettingsTokensComponent);