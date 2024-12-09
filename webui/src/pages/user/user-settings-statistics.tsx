/********************************************************************************
 * Copyright (c) 2024 Precies. Software OU and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import { Box, Typography } from '@mui/material';
import React, { FunctionComponent, useContext, useEffect, useRef, useState } from 'react';
import { DelayedLoadIndicator } from '../../components/delayed-load-indicator';
import { MainContext } from '../../context';
import { isError, PublisherStatistics } from '../../extension-registry-types';
import { UserPublisherStatistics } from './user-publisher-statistics';

export const UserSettingsStatistics: FunctionComponent = () => {

    const [loading, setLoading] = useState(true);
    const [statistics, setStatistics] = useState<PublisherStatistics[]>([]);
    const { user, service, handleError } = useContext(MainContext);
    const abortController = useRef<AbortController>(new AbortController());

    useEffect(() => {
        updateExtensions();
        return () => {
            abortController.current.abort();
        };
    }, []);

    const updateExtensions = async (): Promise<void> => {
        if (!user) {
            return;
        }
        try {
            const response = await service.getStatistics(abortController.current);
            if (isError(response)) {
                throw response;
            }

            setStatistics(response as PublisherStatistics[]);
            setLoading(false);
        } catch (err) {
            handleError(err);
            setLoading(false);
        }
    };

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
            <Typography variant='h5' gutterBottom>Statistics</Typography>
        </Box>
    </Box>
    <Box mt={2}>
        <DelayedLoadIndicator loading={loading} />
        {
            statistics.length > 0
            ? <UserPublisherStatistics statistics={statistics} />
            : <Typography  variant='body1'>No statistics available yet.</Typography>
        }
    </Box>
</>;
};