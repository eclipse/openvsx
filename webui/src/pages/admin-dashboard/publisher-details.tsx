/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import { FunctionComponent, ReactNode, useContext, useState } from 'react';
import {
    Alert,
    Avatar,
    Box,
    Chip,
    CircularProgress,
    Divider,
    LinearProgress,
    Paper,
    Stack,
    ToggleButton,
    ToggleButtonGroup,
    Tooltip,
    Typography
} from '@mui/material';
import { useIsMutating } from '@tanstack/react-query';
import { Link as RouterLink } from 'react-router-dom';
import GitHubIcon from '@mui/icons-material/GitHub';
import PersonIcon from '@mui/icons-material/Person';
import FolderSharedIcon from '@mui/icons-material/FolderShared';
import VpnKeyIcon from '@mui/icons-material/VpnKey';
import ExtensionIcon from '@mui/icons-material/Extension';
import GavelIcon from '@mui/icons-material/Gavel';
import WarningAmberIcon from '@mui/icons-material/WarningAmber';
import { AdminUser } from '../../extension-registry-types';
import { ErrorResponse } from '../../server-request';
import { MainContext } from '../../context';
import { UserExtensionList } from '../user/user-extension-list';
import { handleError as formatError, toLocalTime } from '../../utils';
import { AdminDashboardRoutes } from './admin-dashboard-routes';
import { PublisherRevokeDialog } from './publisher-revoke-dialog';
import { PublisherRevokeTokensButton } from './publisher-revoke-tokens-button';
import {
    type PublisherRole,
    publisherMutationKey,
    usePublisherInfo,
    useUpdatePublisherRole
} from './use-publisher-admin';

// Ordered as an escalating permission scale, low → high.
const ROLE_OPTIONS: { value: PublisherRole; label: string }[] = [
    { value: 'none', label: 'No role' },
    { value: 'privileged', label: 'Privileged' },
    { value: 'admin', label: 'Admin' }
];

const AGREEMENT_META = {
    signed: { label: 'Signed', color: 'success' as const },
    outdated: { label: 'Outdated', color: 'warning' as const },
    none: { label: 'Not signed', color: 'default' as const }
};

const DetailSection: FunctionComponent<{ icon: ReactNode; title: string; count?: number; children: ReactNode }> = ({
    icon,
    title,
    count,
    children
}) => (
    <Box>
        <Stack direction='row' spacing={0.75} alignItems='center' sx={{ mb: 1 }}>
            {icon}
            <Typography variant='subtitle2'>{title}</Typography>
            {count !== undefined && (
                <Typography variant='caption' color='text.secondary'>
                    ({count})
                </Typography>
            )}
        </Stack>
        {children}
    </Box>
);

/**
 * The details card for the publisher selected in the search. Identity and role are
 * available immediately from the search result; the account info (agreement, tokens,
 * extensions) loads on demand. A top progress bar reflects any write in flight.
 */
export const PublisherDetails: FunctionComponent<{ entry: AdminUser }> = ({ entry }) => {
    const { user } = entry;
    const { user: currentUser } = useContext(MainContext);
    const isCurrentUser = currentUser?.loginName === user.loginName && currentUser?.provider === user.provider;

    const [selectedRole, setSelectedRole] = useState<PublisherRole>(() => (user.role as PublisherRole) ?? 'none');
    const updateRole = useUpdatePublisherRole();
    const busy = useIsMutating({ mutationKey: publisherMutationKey }) > 0;

    const { data: publisherInfo, isLoading, error } = usePublisherInfo(user.loginName, user.provider ?? 'github', true);

    const handleRoleChange = (role: PublisherRole) => {
        if (role === selectedRole || !user.provider) {
            return;
        }
        // Optimistic: reflect the choice immediately, revert if the save fails.
        setSelectedRole(role);
        updateRole.mutate(
            { provider: user.provider, login: user.loginName, role },
            { onError: () => setSelectedRole((user.role as PublisherRole) ?? 'none') }
        );
    };

    const agreementStatus = publisherInfo?.user.publisherAgreement?.status ?? 'none';
    const agreement = AGREEMENT_META[agreementStatus];

    return (
        <Paper
            variant='outlined'
            sx={{
                position: 'relative',
                overflow: 'hidden',
                p: { xs: 2, md: 3 },
                flex: 1,
                display: 'flex',
                flexDirection: 'column'
            }}>
            {busy && <LinearProgress sx={{ position: 'absolute', top: 0, left: 0, right: 0 }} />}

            <Stack
                direction={{ xs: 'column', md: 'row' }}
                spacing={2}
                sx={{ minWidth: 0, alignItems: { md: 'center' } }}>
                <Avatar variant='rounded' src={user.avatarUrl} sx={{ width: 56, height: 56 }} />
                <Box sx={{ minWidth: 0, flex: 1 }}>
                    <Stack direction='row' spacing={1} alignItems='center' useFlexGap sx={{ flexWrap: 'wrap' }}>
                        <Typography variant='h6' noWrap>
                            {user.loginName}
                        </Typography>
                        <Chip
                            icon={
                                user.provider === 'github' ? (
                                    <GitHubIcon fontSize='small' />
                                ) : (
                                    <PersonIcon fontSize='small' />
                                )
                            }
                            label={user.provider ?? '—'}
                            size='small'
                            variant='outlined'
                        />
                        {isCurrentUser && (
                            <Chip
                                label='you'
                                size='small'
                                color='info'
                                variant='outlined'
                                sx={{ height: 18, '& .MuiChip-label': { px: 0.5, fontSize: '0.65rem' } }}
                            />
                        )}
                    </Stack>
                    <Typography variant='body2' color='text.secondary' noWrap>
                        {user.fullName || '—'}
                    </Typography>
                </Box>
                <Stack spacing={0.5} sx={{ flexShrink: 0, alignItems: { xs: 'flex-start', md: 'flex-end' } }}>
                    <Typography variant='caption' color='text.secondary'>
                        Role
                    </Typography>
                    <ToggleButtonGroup
                        exclusive
                        size='small'
                        color='primary'
                        value={selectedRole}
                        disabled={!user.provider || busy}
                        onChange={(_event, value) => value && handleRoleChange(value)}>
                        {ROLE_OPTIONS.map(o => (
                            <ToggleButton key={o.value} value={o.value} sx={{ textTransform: 'none', px: 1.5 }}>
                                {o.label}
                            </ToggleButton>
                        ))}
                    </ToggleButtonGroup>
                </Stack>
            </Stack>

            <Divider sx={{ my: 3 }} />

            {updateRole.isError && (
                <Alert severity='error' sx={{ mb: 2 }} onClose={() => updateRole.reset()}>
                    {formatError(updateRole.error as Error | Partial<ErrorResponse>)}
                </Alert>
            )}

            <Box sx={{ flex: 1, display: 'flex', flexDirection: 'column', gap: 3 }}>
                <DetailSection
                    icon={<FolderSharedIcon fontSize='small' color='action' />}
                    title='Namespaces'
                    count={entry.namespaces.length}>
                    {entry.namespaces.length > 0 ? (
                        <Stack direction='row' spacing={0.5} useFlexGap sx={{ flexWrap: 'wrap' }}>
                            {entry.namespaces.map(ns => (
                                <Chip
                                    key={ns.name}
                                    label={ns.name}
                                    size='small'
                                    component={RouterLink}
                                    to={`${AdminDashboardRoutes.NAMESPACE_ADMIN}/${encodeURIComponent(ns.name)}`}
                                    clickable
                                />
                            ))}
                        </Stack>
                    ) : (
                        <Typography variant='body2' color='text.secondary'>
                            None
                        </Typography>
                    )}
                </DetailSection>

                {isLoading && (
                    <Box sx={{ display: 'flex', justifyContent: 'center', py: 3 }}>
                        <CircularProgress />
                    </Box>
                )}
                {error && <Alert severity='error'>{formatError(error as Error | Partial<ErrorResponse>)}</Alert>}

                {publisherInfo && (
                    <>
                        <Stack direction='row' spacing={1} useFlexGap sx={{ flexWrap: 'wrap' }}>
                            <Tooltip title={toLocalTime(publisherInfo.user.publisherAgreement?.timestamp) ?? ''}>
                                <Chip
                                    icon={<GavelIcon fontSize='small' />}
                                    label={`Publisher agreement: ${agreement.label}`}
                                    size='small'
                                    color={agreement.color}
                                    variant={agreementStatus === 'none' ? 'outlined' : 'filled'}
                                />
                            </Tooltip>
                            <Chip
                                icon={<VpnKeyIcon fontSize='small' />}
                                label={`${publisherInfo.activeAccessTokenNum} active access token${
                                    publisherInfo.activeAccessTokenNum === 1 ? '' : 's'
                                }`}
                                size='small'
                                variant='outlined'
                            />
                        </Stack>

                        <DetailSection
                            icon={<ExtensionIcon fontSize='small' color='action' />}
                            title='Published extensions'
                            count={publisherInfo.extensions.length}>
                            {publisherInfo.extensions.length > 0 ? (
                                <UserExtensionList extensions={publisherInfo.extensions} loading={false} />
                            ) : (
                                <Typography variant='body2' color='text.secondary'>
                                    This user has not published any extensions.
                                </Typography>
                            )}
                        </DetailSection>

                        <Box sx={{ mt: 'auto', border: 1, borderColor: 'error.main', borderRadius: 1, p: 2 }}>
                            <Stack direction='row' spacing={0.75} alignItems='center' sx={{ mb: 0.5 }}>
                                <WarningAmberIcon fontSize='small' color='error' />
                                <Typography variant='subtitle2' color='error'>
                                    Danger zone
                                </Typography>
                            </Stack>
                            <Typography variant='body2' color='text.secondary' sx={{ mb: 1.5 }}>
                                Revoking deactivates {user.loginName}&apos;s access tokens, published extensions, and
                                publisher agreement. These actions cannot be undone.
                            </Typography>
                            <Stack direction='row' spacing={1} useFlexGap sx={{ flexWrap: 'wrap' }}>
                                {publisherInfo.activeAccessTokenNum > 0 && (
                                    <PublisherRevokeTokensButton publisherInfo={publisherInfo} />
                                )}
                                <PublisherRevokeDialog publisherInfo={publisherInfo} />
                            </Stack>
                        </Box>
                    </>
                )}
            </Box>
        </Paper>
    );
};
