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

import React, { FunctionComponent, ReactNode } from 'react';
import { Box } from '@mui/material';

interface TabPanelProps {
    children?: ReactNode;
    value: number;
    index: number;
}

/**
 * TabPanel component for conditionally displaying tab content.
 * Shows children when the selected tab value matches this panel's index.
 */
export const TabPanel: FunctionComponent<TabPanelProps> = ({
    children,
    value,
    index,
}) => {
    return (
        <Box
            role='tabpanel'
            hidden={value !== index}
            id={`scan-tabpanel-${index}`}
            aria-labelledby={`scan-tab-${index}`}
        >
            {value === index && children}
        </Box>
    );
};
