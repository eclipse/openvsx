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

import { FunctionComponent, ReactNode, useContext } from 'react';
import { Link as RouteLink } from 'react-router';
import { Box } from '@mui/material';
import LayersOutlinedIcon from '@mui/icons-material/LayersOutlined';
import { MainContext } from '../../../context';
import { PillTab, PillTabs } from '../../../components/pill-tabs';
import { createRoute } from '../../../utils';
import { UserSettingsRoutes } from '../user-settings-routes';
import { useActiveSettingsTab, useSettingsTabs } from './settings-tabs';

// Icon + label inside a pill.
const PillLabel: FunctionComponent<{ icon: ReactNode; children: ReactNode }> = ({ icon, children }) => (
    <Box
        sx={{
            display: 'flex',
            alignItems: 'center',
            gap: '0.4375rem',
            '& svg': { fontSize: '1rem' }
        }}>
        {icon}
        {children}
    </Box>
);

/** Below-md settings navigation: sticky scrollable pill tabs shared with the extension detail page. */
export const UserSettingsMobileNav: FunctionComponent = () => {
    const { user } = useContext(MainContext);
    const activeTab = useActiveSettingsTab();
    const settingsTabs = useSettingsTabs();

    if (!user) {
        return null;
    }

    return (
        <PillTabs
            value={activeTab ?? false}
            gutters={{ xs: '1rem', sm: '1.75rem' }}
            sx={{ display: { md: 'none' }, mb: '0.5rem' }}>
            {settingsTabs.map(({ value, label, icon }) => (
                <PillTab
                    key={value}
                    value={value}
                    label={<PillLabel icon={icon}>{label}</PillLabel>}
                    component={RouteLink}
                    to={createRoute([UserSettingsRoutes.ROOT, value])}
                />
            ))}
            <PillTab
                value='namespaces'
                label={<PillLabel icon={<LayersOutlinedIcon />}>Namespaces</PillLabel>}
                component={RouteLink}
                to={UserSettingsRoutes.NAMESPACES}
            />
        </PillTabs>
    );
};
