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
import { Theme, createStyles, WithStyles, withStyles, Typography, Box, Paper, Button } from '@material-ui/core';
import { Link as RouteLink } from 'react-router-dom';
import { DelayedLoadIndicator } from '../../components/delayed-load-indicator';
import { Timestamp } from '../../components/timestamp';
import { PersonalAccessToken } from '../../extension-registry-types';
import { MainContext } from '../../context';
import { GenerateTokenDialog } from './generate-token-dialog';
import { UserSettingsRoutes } from './user-settings';

const tokensStyle = (theme: Theme) => createStyles({
    header: {
        display: 'flex',
        justifyContent: 'space-between',
        [theme.breakpoints.down('sm')]: {
            flexDirection: 'column',
            alignItems: 'center'
        }
    },
    buttons: {
        display: 'flex',
        flexWrap: 'wrap',
        [theme.breakpoints.down('sm')]: {
            justifyContent: 'center'
        }
    },
    description: {
        fontWeight: 'bold',
        overflow: 'hidden',
        textOverflow: 'ellipsis'
    },
    deleteBtn: {
        color: theme.palette.error.main,
        height: 36
    },
    link: {
        color: theme.palette.info.main
    },
    empty: {
        [theme.breakpoints.down('sm')]: {
            textAlign: 'center'
        }
    }
});

class UserSettingsTokensComponent extends React.Component<UserSettingsTokensComponent.Props, UserSettingsTokensComponent.State> {

    static contextType = MainContext;
    declare context: MainContext;

    constructor(props: UserSettingsTokensComponent.Props) {
        super(props);

        this.state = { tokens: [], loading: true };
    }

    componentDidMount() {
        this.updateTokens();
    }

    protected async updateTokens() {
        if (!this.context.user) {
            return;
        }
        try {
            const tokens = await this.context.service.getAccessTokens(this.context.user);
            this.setState({ tokens, loading: false });
        } catch (err) {
            this.context.handleError(err);
            this.setState({ loading: false });
        }
    }

    protected handleDelete = async (token: PersonalAccessToken) => {
        this.setState({ loading: true });
        try {
            await this.context.service.deleteAccessToken(token);
            this.updateTokens();
        } catch (err) {
            this.context.handleError(err);
        }
    };

    protected handleDeleteAll = async () => {
        this.setState({ loading: true });
        try {
            await this.context.service.deleteAllAccessTokens(this.state.tokens);
            this.updateTokens();
        } catch (err) {
            this.context.handleError(err);
        }
    };

    protected handleTokenGenerated = () => {
        this.setState({ loading: true });
        this.updateTokens();
    };

    render() {
        const agreement = this.context.user?.publisherAgreement;
        if (agreement === 'none' || agreement === 'outdated') {
            return <Box>
                <Typography variant='body1' className={this.props.classes.empty}>
                    {
                        agreement === 'outdated'
                        ? 'Your publisher agreement is outdated.'
                        : 'You have not signed a publisher agreement yet.'
                    } Please sign the agreement on
                    the <RouteLink className={this.props.classes.link} to={UserSettingsRoutes.PROFILE}>Profile</RouteLink> page.
                </Typography>
            </Box>;
        }
        return <React.Fragment>
            <Box className={this.props.classes.header}>
                <Box>
                    <Typography variant='h5' gutterBottom>Access Tokens</Typography>
                </Box>
                <Box className={this.props.classes.buttons}>
                    <Box mr={1} mb={1}>
                        <GenerateTokenDialog
                            handleTokenGenerated={this.handleTokenGenerated}
                        />
                    </Box>
                    <Box>
                        <Button
                            variant='outlined'
                            onClick={this.handleDeleteAll}
                            classes={{ root: this.props.classes.deleteBtn }}>
                            Delete all
                        </Button>
                    </Box>
                </Box>
            </Box>
            <Box mt={2}>
                {
                    this.state.tokens.length === 0 && !this.state.loading ?
                    <Typography variant='body1' className={this.props.classes.empty}>
                        You currently have no tokens.
                    </Typography> : null
                }
            </Box>
            <Box mt={2}>
                <DelayedLoadIndicator loading={this.state.loading}/>
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
                <Typography variant='body2'>Created: <Timestamp value={token.createdTimestamp}/></Typography>
                <Typography variant='body2'>Accessed: {token.accessedTimestamp ? <Timestamp value={token.accessedTimestamp}/> : 'never'}</Typography>
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
    }

    export interface State {
        tokens: PersonalAccessToken[];
        loading: boolean;
    }
}

export const UserSettingsTokens = withStyles(tokensStyle)(UserSettingsTokensComponent);