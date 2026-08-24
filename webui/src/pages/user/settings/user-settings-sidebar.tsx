/********************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import { FunctionComponent, useContext, useState } from 'react';
import { useNavigate, useParams } from 'react-router';
import { Box, ButtonBase, IconButton, Typography } from '@mui/material';
import { styled } from '@mui/material/styles';
import WarningAmberIcon from '@mui/icons-material/WarningAmber';
import AddIcon from '@mui/icons-material/Add';
import { MainContext } from '../../../context';
import { Eyebrow, focusOutline } from '../../../components/page-primitives';
import { UserAvatar } from '../../../components/user-avatar';
import { MONO_FONT, NAVBAR_HEIGHT } from '../../../default/theme';
import { createRoute } from '../../../utils';
import { UserSettingsRoutes } from '../user-settings-routes';
import { CreateNamespaceDialog } from '../namespaces/create-namespace-dialog';
import { useHandleNamespaceCreated, useUserNamespaces } from '../namespaces/use-user-namespaces';
import { EmptyPlaceholder } from './settings-primitives';
import { SettingsTab, useActiveSettingsTab, useSettingsTabs } from './settings-tabs';

// Quiet full-width sidebar row that tints on hover/selection.
const NavItem = styled(ButtonBase, { shouldForwardProp: prop => prop !== 'active' })<{ active?: boolean }>(
    ({ theme, active }) => ({
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'flex-start',
        gap: '0.6875rem',
        width: '100%',
        textAlign: 'left',
        fontFamily: 'inherit',
        fontSize: '0.875rem',
        fontWeight: active ? 600 : 500,
        color: active ? theme.palette.text.primary : theme.palette.text.secondary,
        backgroundColor: active ? theme.palette.surface3 : 'transparent',
        padding: '0.5625rem 0.75rem',
        borderRadius: theme.shape.borderRadius,
        ...focusOutline(theme),
        '@media (hover: hover)': {
            '&:hover': { backgroundColor: theme.palette.surface3 }
        },
        '& .nav-icon': {
            display: 'flex',
            width: '1.0625rem',
            justifyContent: 'center',
            color: active ? theme.palette.text.primary : theme.palette.text.disabled,
            '& svg': { fontSize: '1.0625rem' }
        }
    })
);

// Namespace entry below the "Namespaces" separator; mono like all namespace names.
const NamespaceItem = styled(NavItem)({
    gap: '0.5rem',
    fontFamily: MONO_FONT,
    fontSize: '0.84375rem',
    padding: '0.5rem 0.75rem'
});

// The tab-sized twin of the settings tabs' placeholder: same dashed frame, scaled to the column.
const NamespacesPlaceholder = styled(EmptyPlaceholder)({
    padding: '1.125rem 0.75rem',
    fontSize: '0.78125rem',
    lineHeight: 1.45,
    margin: '0 0.25rem'
});

const TabNavItem: FunctionComponent<{ tab: SettingsTab }> = ({ tab }) => {
    const navigate = useNavigate();
    const activeTab = useActiveSettingsTab();
    return (
        <NavItem
            active={activeTab === tab.value}
            onClick={() => navigate(createRoute([UserSettingsRoutes.ROOT, tab.value]))}>
            <span className='nav-icon'>{tab.icon}</span>
            {tab.label}
        </NavItem>
    );
};

/** Desktop settings navigation: user head, tab list and the namespaces group. */
export const UserSettingsSidebar: FunctionComponent = () => {
    const { user } = useContext(MainContext);
    const navigate = useNavigate();
    const activeTab = useActiveSettingsTab();
    const settingsTabs = useSettingsTabs();
    const { namespace, extension } = useParams();
    const namespacesQuery = useUserNamespaces();
    const namespaces = namespacesQuery.data ?? [];
    const handleNamespaceCreated = useHandleNamespaceCreated();
    const [createDialogOpen, setCreateDialogOpen] = useState(false);

    if (!user) {
        return null;
    }

    // The namespaces tab shows the first namespace when none is selected explicitly.
    const getSelectedNamespace = (): string | undefined => {
        if (activeTab !== 'namespaces' || extension != null) {
            return undefined;
        }
        return namespace ?? namespaces[0]?.name;
    };
    const selectedNamespace = getSelectedNamespace();

    return (
        <Box
            component='aside'
            sx={{
                display: { xs: 'none', md: 'block' },
                minWidth: 0,
                position: 'sticky',
                top: `calc(${NAVBAR_HEIGHT} + 1.625rem)`,
                // Same z as the AppBar, later in the DOM: keeps the navbar's
                // backdrop-blur fan from blurring the sidebar's top rows.
                zIndex: 50
            }}>
            <Box
                sx={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: '0.75rem',
                    p: '0.375rem 0.25rem 1.125rem'
                }}>
                <UserAvatar
                    user={user}
                    sx={{
                        width: '2.75rem',
                        height: '2.75rem',
                        fontSize: '0.9375rem'
                    }}
                />
                <Box sx={{ minWidth: 0 }}>
                    <Typography noWrap sx={{ fontSize: '0.875rem', fontWeight: 700 }}>
                        {user.fullName || user.loginName}
                    </Typography>
                    <Typography noWrap sx={{ fontSize: '0.75rem', color: 'text.disabled' }}>
                        @{user.loginName}
                    </Typography>
                </Box>
            </Box>
            <Box component='nav' sx={{ display: 'flex', flexDirection: 'column', gap: '0.125rem' }}>
                {settingsTabs.map(tab => (
                    <TabNavItem key={tab.value} tab={tab} />
                ))}
                <Box
                    sx={{
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'space-between',
                        m: '1.25rem 0.75rem 0.5rem'
                    }}>
                    <Eyebrow sx={{ fontSize: '0.6875rem', letterSpacing: '0.06em' }}>Namespaces</Eyebrow>
                    <IconButton
                        size='small'
                        title='Create namespace'
                        aria-label='Create namespace'
                        onClick={() => setCreateDialogOpen(true)}
                        sx={theme => ({ p: '0.1875rem', borderRadius: `${theme.shape.borderRadius}px` })}>
                        <AddIcon sx={{ fontSize: '0.875rem' }} />
                    </IconButton>
                </Box>
                {namespaces.map(ns => (
                    <NamespaceItem
                        key={ns.name}
                        active={ns.name === selectedNamespace}
                        onClick={() => navigate(createRoute([UserSettingsRoutes.NAMESPACES, ns.name]))}>
                        <Typography component='span' noWrap sx={{ flex: 1, minWidth: 0, font: 'inherit' }}>
                            {ns.name}
                        </Typography>
                        {!ns.verified ? (
                            <WarningAmberIcon
                                titleAccess='Not verified'
                                sx={{ fontSize: '0.8125rem', color: 'warningAccent', flexShrink: 0 }}
                            />
                        ) : null}
                    </NamespaceItem>
                ))}
                {namespaces.length === 0 && !namespacesQuery.isLoading ? (
                    <NamespacesPlaceholder>You don&apos;t belong to any namespace yet.</NamespacesPlaceholder>
                ) : null}
            </Box>
            <CreateNamespaceDialog
                open={createDialogOpen}
                onClose={() => setCreateDialogOpen(false)}
                namespaceCreated={handleNamespaceCreated}
            />
        </Box>
    );
};
