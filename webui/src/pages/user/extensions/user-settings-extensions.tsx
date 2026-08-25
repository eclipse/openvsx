/** ******************************************************************************
 * Copyright (c) 2022 Precies. Software and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */

import { FunctionComponent, useContext, useEffect, useState, useRef } from 'react';
import { Link as RouteLink } from 'react-router';
import { Extension } from '../../../extension-registry-types';
import { Box, Button } from '@mui/material';
import FileUploadOutlinedIcon from '@mui/icons-material/FileUploadOutlined';
import { isError } from '../../../extension-registry-types';
import { DelayedLoadIndicator } from '../../../components/delayed-load-indicator';
import { MainContext } from '../../../context';
import { ManageExtensionCard } from '../../../components/extension/manage-extension-card';
import { EmptyPlaceholder } from '../settings/settings-primitives';
import { ExtensionGrid } from '../../../components/page-primitives';
import { SettingsHeader } from '../settings/settings-header';
import { UserSettingsRoutes } from '../user-settings-routes';
import { PublishRoutes } from '../../publish/publish-routes';

export const UserSettingsExtensions: FunctionComponent = () => {
    const [loading, setLoading] = useState(true);
    const [extensions, setExtensions] = useState(Array<Extension>());
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
            const response = await service.getExtensions(abortController.current);
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

    return (
        <Box>
            <SettingsHeader
                title='Extensions'
                actions={
                    <Button
                        variant='outlined'
                        component={RouteLink}
                        to={PublishRoutes.ROOT}
                        startIcon={<FileUploadOutlinedIcon sx={{ fontSize: '0.9375rem' }} />}>
                        Publish extension
                    </Button>
                }
            />
            <DelayedLoadIndicator loading={loading} />
            {extensions && extensions.length > 0 ? (
                <ExtensionGrid>
                    {extensions.map(extension => (
                        <ManageExtensionCard
                            key={`${extension.namespace}.${extension.name}-${extension.version}`}
                            extension={extension}
                            routePrefix={UserSettingsRoutes.EXTENSIONS}
                        />
                    ))}
                </ExtensionGrid>
            ) : !loading ? (
                <EmptyPlaceholder>You haven&apos;t published any extensions yet.</EmptyPlaceholder>
            ) : null}
        </Box>
    );
};
