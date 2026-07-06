/******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/

import { FunctionComponent, useContext, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { MainContext } from '../../context';
import { DelayedLoadIndicator } from '../../components/delayed-load-indicator';
import { ExtensionDetailView } from '../../components/extension/extension-detail-view';
import { UserSettingsRoutes } from './user-settings-routes';
import { useDeleteUserExtensionVersions, useUserExtension } from './use-user-extension';

export const UserSettingsExtensionSettings: FunctionComponent<UserSettingsExtensionSettingsProps> = props => {
    const { handleError } = useContext(MainContext);
    const navigate = useNavigate();

    const {
        data: extension,
        isFetching: loading,
        error,
        refetch
    } = useUserExtension({ namespace: props.namespace, extension: props.extension });
    const { mutateAsync: deleteVersions } = useDeleteUserExtensionVersions();

    useEffect(() => {
        if (!error) {
            return;
        }
        if ((error as { status?: number }).status === 404) {
            navigate(UserSettingsRoutes.EXTENSIONS);
        } else {
            handleError(error);
        }
    }, [error, navigate, handleError]);

    if (loading) {
        return <DelayedLoadIndicator loading={true} />;
    }

    if (!extension) {
        return null;
    }

    return (
        <ExtensionDetailView
            extension={extension}
            onRemoveVersion={targets =>
                deleteVersions({
                    namespace: extension.namespace,
                    extension: extension.name,
                    targetPlatformVersions: targets
                })
            }
            onVersionDeleted={refetch}
        />
    );
};

export interface UserSettingsExtensionSettingsProps {
    namespace: string;
    extension: string;
}
