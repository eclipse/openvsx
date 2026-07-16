/********************************************************************************
 * Copyright (c) 2019 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import { ReactElement } from 'react';
import { Tabs, Tab, useTheme, useMediaQuery } from '@mui/material';
import { useNavigate, useParams } from 'react-router-dom';
import { createRoute } from '../../utils';
import { UserSettingsRoutes } from './user-settings-routes';

const TABS = [
    { value: 'profile', label: 'Profile' },
    { value: 'tokens', label: 'Access Tokens' },
    { value: 'namespaces', label: 'Namespaces' },
    { value: 'extensions', label: 'Extensions' },
    { value: 'customers', label: 'Rate Limiting' }
];

export const UserSettingTabs = (): ReactElement => {
    const theme = useTheme();
    const isATablet = useMediaQuery(theme.breakpoints.down('md'));
    const isAMobile = useMediaQuery(theme.breakpoints.down('sm'));
    const { tab } = useParams();

    const navigate = useNavigate();

    const generateRoute = (tab: string) => {
        return createRoute([UserSettingsRoutes.ROOT, tab]);
    };

    return (
        <Tabs
            value={tab ?? 'extensions'}
            orientation={isATablet ? 'horizontal' : 'vertical'}
            centered={isAMobile}
            indicatorColor='secondary'>
            {TABS.map(({ value, label }) => (
                // MUI's Tabs only fires `onChange` when the clicked tab differs from the
                // currently selected one, so a per-Tab `onClick` is used instead to also
                // navigate when the already-active tab is clicked.
                <Tab key={value} value={value} label={label} onClick={() => navigate(generateRoute(value))} />
            ))}
        </Tabs>
    );
};
