/********************************************************************************
 * Copyright (c) 2019 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import React, { FunctionComponent, ReactNode, useContext, useEffect, useState } from 'react';
import { Theme, Typography, Box, Paper, Button, Link } from '@mui/material';
import { Link as RouteLink } from 'react-router-dom';
import { DelayedLoadIndicator } from '../../components/delayed-load-indicator';
import { Timestamp } from '../../components/timestamp';
import { PersonalAccessToken } from '../../extension-registry-types';
import { MainContext } from '../../context';
import { GenerateTokenDialog } from './generate-token-dialog';
import { UserSettingsRoutes } from './user-settings';
import styled from '@mui/material/styles/styled';

const link = ({ theme }: { theme: Theme }) => ({
    color: theme.palette.secondary.main,
    textDecoration: 'none',
    '&:hover': {
        textDecoration: 'underline'
    }
});

const StyledLink = styled(Link)(link);
const StyledRouteLink = styled(RouteLink)(link);

const EmptyTypography = styled(Typography)(({ theme }: { theme: Theme }) => ({
    [theme.breakpoints.down('sm')]: {
        textAlign: 'center'
    }
}));

const DeleteButton = styled(Button)(({ theme }: { theme: Theme }) => ({
    color: theme.palette.error.main,
    height: 36
}));

export const UserSettingsTokens: FunctionComponent = () => {

    const { service, user, handleError } = useContext(MainContext);

    const [tokens, setTokens] = useState(new Array<PersonalAccessToken>());
    const [loading, setLoading] = useState(true);

    const abortController = new AbortController();
    useEffect(() => {
        updateTokens();
        return () => {
            abortController.abort();
        };
    }, []);

    const updateTokens = async() => {
        if (!user) {
            return;
        }
        try {
            const tokens = await service.getAccessTokens(abortController, user);
            setTokens(tokens);
            setLoading(false);
        } catch (err) {
            handleError(err);
            setLoading(false);
        }
    };

    const handleDelete = async (token: PersonalAccessToken) => {
        setLoading(true);
        try {
            await service.deleteAccessToken(abortController, token);
            updateTokens();
        } catch (err) {
            handleError(err);
        }
    };

    const handleDeleteAll = async () => {
        setLoading(true);
        try {
            await service.deleteAllAccessTokens(abortController, tokens);
            updateTokens();
        } catch (err) {
            handleError(err);
        }
    };

    const handleTokenGenerated = () => {
        setLoading(true);
        updateTokens();
    };

    const renderToken = (token: PersonalAccessToken): ReactNode => {
        return <Box key={'token:' + token.id} p={2} display='flex' justifyContent='space-between'>
            <Box alignItems='center' overflow='auto'>
                <Typography sx={{ fontWeight: 'bold', overflow: 'hidden', textOverflow: 'ellipsis' }}>{token.description}</Typography>
                <Typography variant='body2'>Created: <Timestamp value={token.createdTimestamp}/></Typography>
                <Typography variant='body2'>Accessed: {token.accessedTimestamp ? <Timestamp value={token.accessedTimestamp}/> : 'never'}</Typography>
            </Box>
            <Box display='flex' alignItems='center'>
                <DeleteButton
                    variant='outlined'
                    onClick={() => handleDelete(token)}
                    disabled={loading}>
                    Delete
                </DeleteButton>
            </Box>
        </Box>;
    };

    const agreement = user?.publisherAgreement;
    if (agreement && (agreement.status === 'none' || agreement.status === 'outdated')) {
        return <Box>
            <EmptyTypography variant='body1'>
                Access tokens cannot be created as you currently do not have an Eclipse Foundation Open VSX
                Publisher Agreement signed. Please return to
                your <StyledRouteLink to={UserSettingsRoutes.PROFILE}>Profile</StyledRouteLink> page
                to sign the Publisher Agreement. Should you believe this is in error, please
                contact <StyledLink href='mailto:license@eclipse.org'>license@eclipse.org</StyledLink>.
            </EmptyTypography>
        </Box>;
    }

    return <>
        <Box
            sx={{
                display: 'flex',
                justifyContent: 'space-between',
                flexDirection: { xs: 'column', sm: 'column', md: 'row', lg: 'row', xl: 'row' },
                alignItems: { xs: 'center', sm: 'center', md: 'normal', lg: 'normal', xl: 'normal' }
            }}
        >
            <Box>
                <Typography variant='h5' gutterBottom>Access Tokens</Typography>
            </Box>
            <Box
                sx={{
                    display: 'flex',
                    flexWrap: 'wrap',
                    justifyContent: { xs: 'center', sm: 'center', md: 'normal', lg: 'normal', xl: 'normal' }
                }}
            >
                <Box mr={1} mb={1}>
                    <GenerateTokenDialog
                        handleTokenGenerated={handleTokenGenerated}
                    />
                </Box>
                <Box>
                    <DeleteButton
                        variant='outlined'
                        onClick={handleDeleteAll}
                        disabled={loading}>
                        Delete all
                    </DeleteButton>
                </Box>
            </Box>
        </Box>
        <Box mt={2}>
            {
                tokens.length === 0 && !loading ?
                <EmptyTypography variant='body1'>
                    You currently have no tokens.
                </EmptyTypography> : null
            }
        </Box>
        <Box mt={2}>
            <DelayedLoadIndicator loading={loading}/>
            <Paper elevation={3}>
                {tokens.map(token => renderToken(token))}
            </Paper>
        </Box>
    </>;
};

export namespace UserSettingsTokens {
    export interface Props {}
    export interface State {
        tokens: PersonalAccessToken[];
        loading: boolean;
    }
}