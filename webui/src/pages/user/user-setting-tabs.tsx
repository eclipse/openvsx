/********************************************************************************
 * Copyright (c) 2019 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import { ReactElement, useMemo } from 'react';
import { Tabs, Tab, useTheme, useMediaQuery } from '@mui/material';
import { useNavigate, useParams } from 'react-router';
import { createRoute } from '../../utils';
import { UserSettingsRoutes } from './user-settings-routes';
import { useRegistryValue } from '../../hooks/use-registry-value';
import { isTrustedPublishingEnabled } from './trusted-publishing/use-trusted-publishers';

export const UserSettingTabs = (): ReactElement => {
    const theme = useTheme();
    const isATablet = useMediaQuery(theme.breakpoints.down('md'));
    const isAMobile = useMediaQuery(theme.breakpoints.down('sm'));
    const { tab } = useParams();
    const trustedPublishingEnabled = useRegistryValue(isTrustedPublishingEnabled);

    const navigate = useNavigate();

    const generateRoute = (tab: string) => {
        return createRoute([UserSettingsRoutes.ROOT, tab]);
    };

    const tabs = useMemo(
        () => [
            { value: 'profile', label: 'Profile' },
            { value: 'tokens', label: 'Access Tokens' },
            ...(trustedPublishingEnabled ? [{ value: 'trusted-publishers', label: 'Trusted Publishers' }] : []),
            { value: 'namespaces', label: 'Namespaces' },
            { value: 'extensions', label: 'Extensions' },
            { value: 'customers', label: 'Rate Limiting' }
        ],
        [trustedPublishingEnabled]
    );

    return (
        <Tabs
            value={tab ?? 'extensions'}
            orientation={isATablet ? 'horizontal' : 'vertical'}
            centered={isAMobile}
            indicatorColor='secondary'>
            {tabs.map(({ value, label }) => (
                // MUI's Tabs only fires `onChange` when the clicked tab differs from the
                // currently selected one, so a per-Tab `onClick` is used instead to also
                // navigate when the already-active tab is clicked.
                <Tab key={value} value={value} label={label} onClick={() => navigate(generateRoute(value))} />
            ))}
        </Tabs>
    );
};
