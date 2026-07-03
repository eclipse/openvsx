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

import { FunctionComponent, useState, useContext, useEffect, useRef, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { MainContext } from '../../context';
import { isError, Extension } from '../../extension-registry-types';
import { DelayedLoadIndicator } from '../../components/delayed-load-indicator';
import { ExtensionDetailView } from '../../components/extension/extension-detail-view';
import { UserSettingsRoutes } from './user-settings-routes';

export const UserSettingsExtensionSettings: FunctionComponent<UserSettingsExtensionSettingsProps> = props => {
    const { service, handleError } = useContext(MainContext);
    const navigate = useNavigate();
    const abortController = useRef<AbortController>(new AbortController());

    const [loading, setLoading] = useState(false);
    const [extension, setExtension] = useState<Extension | undefined>(undefined);

    useEffect(() => {
        return () => {
            abortController.current.abort();
        };
    }, []);

    const loadExtension = useCallback(async () => {
        try {
            setLoading(true);
            const result = await service.getExtension(abortController.current, props.namespace, props.extension);
            if (isError(result)) {
                throw result;
            }
            setExtension(result);
        } catch (err) {
            if (err && (err as { status?: number }).status === 404) {
                navigate(UserSettingsRoutes.EXTENSIONS);
            } else {
                handleError(err);
            }
        } finally {
            setLoading(false);
        }
    }, [props.namespace, props.extension]);

    useEffect(() => {
        loadExtension();
    }, [loadExtension]);

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
                service.deleteExtensions(abortController.current, {
                    namespace: extension.namespace,
                    extension: extension.name,
                    targetPlatformVersions: targets
                })
            }
            onVersionDeleted={loadExtension}
        />
    );
};

export interface UserSettingsExtensionSettingsProps {
    namespace: string;
    extension: string;
}
