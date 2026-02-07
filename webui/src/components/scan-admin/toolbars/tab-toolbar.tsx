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

import React, { FunctionComponent } from 'react';
import { Paper, Tabs, Tab } from '@mui/material';

interface TabToolbarProps {
    selectedTab: number;
    onTabChange: (event: React.SyntheticEvent, newValue: number) => void;
    tabs: string[];
}

/**
 * TabToolbar component for the scan admin tab navigation.
 * Renders the Paper/Tabs bar for switching between scan tabs.
 */
export const TabToolbar: FunctionComponent<TabToolbarProps> = ({
    selectedTab,
    onTabChange,
    tabs,
}) => {
    return (
        <Paper sx={{ mb: 1.5 }}>
            <Tabs
                value={selectedTab}
                onChange={onTabChange}
                aria-label='scans tabs'
                sx={{
                    borderBottom: 1,
                    borderColor: 'divider',
                    minHeight: '48px',
                    '& .MuiTabs-indicator': {
                        backgroundColor: 'secondary.main',
                    },
                    '& .MuiTab-root': {
                        minHeight: '48px',
                    },
                }}
            >
                {tabs.map((label, index) => (
                    <Tab key={index} label={label} />
                ))}
            </Tabs>
        </Paper>
    );
};
