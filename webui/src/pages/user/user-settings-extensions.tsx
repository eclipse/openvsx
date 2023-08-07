/** ******************************************************************************
 * Copyright (c) 2022 Precies. Software and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */

import React, { FunctionComponent, useContext, useEffect, useState } from 'react';
import { Extension } from '../../extension-registry-types';
import { Box, Typography } from '@mui/material';
import { PublishExtensionDialog } from './publish-extension-dialog';
import { UserExtensionList } from './user-extension-list';
import { isError } from '../../extension-registry-types';
import { DelayedLoadIndicator } from '../../components/delayed-load-indicator';
import { MainContext } from '../../context';

export const UserSettingsExtensions: FunctionComponent = () => {

    const [loading, setLoading] = useState(true);
    const [extensions, setExtensions] = useState(Array<Extension>());
    const { user, service, handleError } = useContext(MainContext);
    const abortController = new AbortController();

    useEffect(() => {
        updateExtensions();
        return () => {
            abortController.abort();
        };
    }, []);

    const handleExtensionPublished = () => {
        setLoading(true);
        updateExtensions();
    };

    const updateExtensions = async (): Promise<void> => {
        if (!user) {
            return;
        }
        try {
            const response = await service.getExtensions(abortController);
            if (isError(response)) {
                throw response;
            }

            const extensions = response as Extension[];
            setExtensions(extensions);
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
                    <PublishExtensionDialog extensionPublished={handleExtensionPublished}/>
                </Box>
            </Box>
        </Box>
        <Box mt={2}>
            <DelayedLoadIndicator loading={loading} />
            {
                extensions && extensions.length > 0
                ? <UserExtensionList extensions={extensions} loading={loading} />
                : <Typography  variant='body1'>No extensions published under this namespace yet.</Typography>
            }
        </Box>
    </>;
};