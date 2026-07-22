/********************************************************************************
 * Copyright (c) 2019 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import { FunctionComponent, ReactNode, useContext, useEffect, useState, useRef } from 'react';
import {
    Typography,
    Box,
    Button,
    Link,
    Dialog,
    DialogTitle,
    DialogContent,
    DialogContentText,
    DialogActions
} from '@mui/material';
import { styled } from '@mui/material/styles';
import KeyOutlinedIcon from '@mui/icons-material/KeyOutlined';
import { Link as RouteLink } from 'react-router';
import { DelayedLoadIndicator } from '../../../components/delayed-load-indicator';
import { Timestamp } from '../../../components/timestamp';
import { PersonalAccessToken } from '../../../extension-registry-types';
import { MainContext } from '../../../context';
import { DetailsCard } from '../../../components/details-card';
import { GenerateAccessTokenDialog } from './generate-access-token-dialog';
import { TrustedPublishingPromo } from '../trusted-publishing/trusted-publishing-promo';
import { EmptyPlaceholder, IconTile } from '../settings/settings-primitives';
import { SettingsHeader } from '../settings/settings-header';
import { UserSettingsRoutes } from '../user-settings-routes';

const TokenRow = styled(Box)({
    display: 'flex',
    alignItems: 'center',
    gap: '1rem',
    padding: '1.125rem 1.25rem'
});

export const UserSettingsTokens: FunctionComponent = () => {
    const { service, user, handleError, pageSettings } = useContext(MainContext);

    const [tokens, setTokens] = useState(new Array<PersonalAccessToken>());
    const [loading, setLoading] = useState(true);
    const [showDeleteAll, setShowDeleteAll] = useState(false);

    const abortController = useRef<AbortController>(new AbortController());
    useEffect(() => {
        updateTokens();
        return () => {
            abortController.current.abort();
        };
    }, []);

    const updateTokens = async () => {
        if (!user) {
            return;
        }
        try {
            const tokens = await service.getAccessTokens(abortController.current, user);
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
            await service.deleteAccessToken(abortController.current, token);
            updateTokens();
        } catch (err) {
            handleError(err);
        }
    };

    const onShowDeleteAll = () => setShowDeleteAll(true);
    const onHideDeleteAll = () => setShowDeleteAll(false);

    const handleDeleteAll = async () => {
        onHideDeleteAll();
        setLoading(true);
        try {
            await service.deleteAllAccessTokens(abortController.current, tokens);
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
        return (
            <TokenRow key={'token:' + token.id}>
                <IconTile sx={{ width: '2.25rem', height: '2.25rem' }}>
                    <KeyOutlinedIcon sx={{ fontSize: '1.0625rem' }} />
                </IconTile>
                <Box sx={{ flex: 1, minWidth: 0 }}>
                    <Typography
                        noWrap
                        sx={{
                            fontSize: '0.90625rem',
                            fontWeight: 600
                        }}>
                        {token.description}
                    </Typography>
                    <Typography sx={{ fontSize: '0.78125rem', color: 'text.disabled', mt: '0.125rem' }}>
                        Expires{' '}
                        {token.expiresTimestamp ? (
                            <Timestamp value={token.expiresTimestamp} isFutureTime={true} />
                        ) : (
                            'never'
                        )}
                    </Typography>
                    <Typography sx={{ fontSize: '0.75rem', color: 'text.disabled', mt: '0.125rem' }}>
                        Created <Timestamp value={token.createdTimestamp} />
                        {/* Only stamped by the server once the token has been used to publish. */}
                        {token.accessedTimestamp ? (
                            <>
                                {' '}
                                &middot; Last used <Timestamp value={token.accessedTimestamp} />
                            </>
                        ) : null}
                    </Typography>
                </Box>
                <Button
                    variant='outlined'
                    size='small'
                    sx={{ flexShrink: 0 }}
                    onClick={() => handleDelete(token)}
                    disabled={loading}>
                    Revoke
                </Button>
            </TokenRow>
        );
    };

    const agreement = user?.publisherAgreement;
    if (agreement && (agreement.status === 'none' || agreement.status === 'outdated')) {
        const publisherAgreementName = pageSettings?.publisherAgreement?.name ?? '';
        const publisherAgreementContact = pageSettings?.publisherAgreement?.email;

        return (
            <Box>
                <SettingsHeader title='Access Tokens' />
                <Typography variant='body1'>
                    Access tokens cannot be created as you currently do not have an {publisherAgreementName} Publisher
                    Agreement signed. Please return to your{' '}
                    <Link color='secondary' underline='hover' component={RouteLink} to={UserSettingsRoutes.PROFILE}>
                        Profile
                    </Link>{' '}
                    page to sign the Publisher Agreement.
                    {publisherAgreementContact !== undefined && (
                        <>
                            {' '}
                            Should you believe this is in error, please contact{' '}
                            <Link color='secondary' underline='hover' href={`mailto:${publisherAgreementContact}`}>
                                {publisherAgreementContact}
                            </Link>
                            .
                        </>
                    )}
                </Typography>
            </Box>
        );
    }

    return (
        <Box>
            <SettingsHeader
                title='Access Tokens'
                description='Personal access tokens let you publish extensions from the command line. Treat them like passwords.'
                actions={
                    <>
                        <Button
                            variant='outlined'
                            color='error'
                            onClick={onShowDeleteAll}
                            disabled={loading || tokens.length === 0}>
                            Delete all
                        </Button>
                        <GenerateAccessTokenDialog handleTokenGenerated={handleTokenGenerated} />
                    </>
                }
            />
            <Box mb='1.375rem'>
                <TrustedPublishingPromo />
            </Box>
            <DelayedLoadIndicator loading={loading} />
            {tokens.length > 0 ? (
                <DetailsCard>{tokens.map(token => renderToken(token))}</DetailsCard>
            ) : !loading ? (
                <EmptyPlaceholder>You currently have no tokens.</EmptyPlaceholder>
            ) : null}
            <Dialog open={showDeleteAll} onClose={onHideDeleteAll}>
                <DialogTitle>Delete all access tokens</DialogTitle>
                <DialogContent>
                    <DialogContentText component='div'>
                        Are you sure you want to delete all access tokens?
                    </DialogContentText>
                </DialogContent>
                <DialogActions>
                    <Button onClick={onHideDeleteAll}>Cancel</Button>
                    <Button variant='contained' color='error' autoFocus onClick={handleDeleteAll}>
                        Delete
                    </Button>
                </DialogActions>
            </Dialog>
        </Box>
    );
};
