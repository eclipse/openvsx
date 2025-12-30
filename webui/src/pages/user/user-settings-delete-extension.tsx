/** ******************************************************************************
 * Copyright (c) 2025 Precies. Software OU and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */

import React, { FunctionComponent, useEffect } from 'react';
import { Box } from '@mui/material';
import { TargetPlatformVersion } from '../../extension-registry-types';
import { DelayedLoadIndicator } from '../../components/delayed-load-indicator';
import { ExtensionVersionContainer } from '../admin-dashboard/extension-version-container';
import { useNavigate } from 'react-router';
import { useDeleteExtensionsMutation, useGetExtensionQuery } from '../../store/api';
import { UserSettingsRoutes } from './user-settings';

export const UserSettingsDeleteExtension: FunctionComponent<UserSettingsDeleteExtensionProps> = props => {
    const navigate = useNavigate();
    const { data: extension, isLoading } = useGetExtensionQuery(props);
    const [deleteExtensions] = useDeleteExtensionsMutation();

    useEffect(() => {
        if (!isLoading && extension == null) {
            navigate(UserSettingsRoutes.EXTENSIONS);
        }
    }, [extension, isLoading]);

    const onRemove = async (targetPlatformVersions?: TargetPlatformVersion[]) => {
        if (extension == null) {
            return;
        }

        await deleteExtensions({ namespace: extension.namespace, extension: extension.name, targetPlatformVersions: targetPlatformVersions?.map(({ version, targetPlatform }) => ({ version, targetPlatform })) });
    };

    return (
        <Box>
            <DelayedLoadIndicator loading={isLoading} />
            {
                extension ?
                    <ExtensionVersionContainer onRemove={onRemove} extension={extension} />
                    : ''
            }
        </Box>
    );
};

export interface UserSettingsDeleteExtensionProps {
    namespace: string;
    extension: string;
}