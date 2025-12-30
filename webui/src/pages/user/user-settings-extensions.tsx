/** ******************************************************************************
 * Copyright (c) 2022 Precies. Software and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */

import React, { FunctionComponent } from 'react';
import { Box, Typography } from '@mui/material';
import { PublishExtensionDialog } from './publish-extension-dialog';
import { UserExtensionList } from './user-extension-list';
import { DelayedLoadIndicator } from '../../components/delayed-load-indicator';
import { useGetExtensionsQuery } from '../../store/api';

export const UserSettingsExtensions: FunctionComponent = () => {

    const { data: extensions, isLoading } = useGetExtensionsQuery();

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
                <Typography variant='h5' gutterBottom>Extensions</Typography>
            </Box>
            <Box
                sx={{
                    display: 'flex',
                    flexWrap: 'wrap',
                    justifyContent: { xs: 'center', sm: 'center', md: 'normal', lg: 'normal', xl: 'normal' }
                }}
            >
                <Box mr={1} mb={1}>
                    <PublishExtensionDialog />
                </Box>
            </Box>
        </Box>
        <Box mt={2}>
            <DelayedLoadIndicator loading={isLoading} />
            {
                extensions != null && extensions.length > 0
                ? <UserExtensionList extensions={extensions} loading={isLoading} canDelete />
                : <Typography  variant='body1'>No extensions published under this namespace yet.</Typography>
            }
        </Box>
    </>;
};