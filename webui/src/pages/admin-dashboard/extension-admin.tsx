/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import { FunctionComponent, useState, useContext, useEffect } from 'react';
import { SearchListContainer } from './search-list-container';
import { ExtensionListSearchfield } from '../extension-list/extension-list-searchfield';
import { Button, Typography } from '@mui/material';
import { MainContext } from '../../context';
import { TargetPlatformVersion } from '../../extension-registry-types';
import { ExtensionVersionContainer } from './extension-version-container';
import { StyledInput } from './namespace-input';
import useQueryParam from '../../hooks/scan-admin/use-query-params-state';
import { useAdminExtension, useDeleteExtension } from './use-extension-admin';

export const ExtensionAdmin: FunctionComponent = () => {
    const { handleError } = useContext(MainContext);

    const [extensionValue, setExtensionValue] = useQueryParam('extension', '');
    const handleExtensionChange = (value: string) => {
        setExtensionValue(value);
    };

    const [namespaceValue, setNamespaceValue] = useQueryParam('namespace', '');
    const handleNamespaceChange = (value: string) => {
        setNamespaceValue(value);
    };

    const [validationError, setValidationError] = useState('');
    const [extensionFieldError, setExtensionFieldError] = useState(false);
    const [namespaceFieldError, setNamespaceFieldError] = useState(false);

    // The lookup runs only after a validated search; null keeps the query idle.
    const [target, setTarget] = useState<{ namespace: string; extension: string } | null>(null);

    const { data, isFetching: loading, error: queryError, refetch } = useAdminExtension(target);
    const { mutateAsync: deleteExtension } = useDeleteExtension();

    const is404 = !!queryError && (queryError as { status?: number }).status === 404;
    // Hide a previously loaded extension once a lookup fails.
    const extension = queryError ? undefined : data;
    const fetchError = is404 ? `Extension not found: ${target?.namespace}.${target?.extension}` : '';
    const error = validationError || fetchError;

    // Non-404 lookup failures keep flowing through the global error dialog.
    useEffect(() => {
        if (queryError && !is404) {
            handleError(queryError);
        }
    }, [queryError, is404, handleError]);

    const findExtension = () => {
        if (!namespaceValue) {
            setNamespaceFieldError(true);
            setValidationError('Name of Namespace is mandatory');
            return;
        }
        setNamespaceFieldError(false);
        if (!extensionValue) {
            setExtensionFieldError(true);
            setValidationError('Name of Extension is mandatory');
            return;
        }
        setExtensionFieldError(false);
        setValidationError('');
        if (target && target.namespace === namespaceValue && target.extension === extensionValue) {
            // Re-submitting the same coordinates should force a fresh lookup.
            refetch();
        } else {
            setTarget({ namespace: namespaceValue, extension: extensionValue });
        }
    };

    const onRemove = async (targetPlatformVersions?: TargetPlatformVersion[]) => {
        if (extension == null) {
            return;
        }

        await deleteExtension({
            namespace: extension.namespace,
            extension: extension.name,
            targetPlatformVersions: targetPlatformVersions?.map(({ version, targetPlatform }) => ({
                version,
                targetPlatform
            }))
        });
        await refetch();
    };

    return (
        <SearchListContainer
            searchContainer={[
                <StyledInput
                    placeholder='Namespace'
                    error={namespaceFieldError}
                    key='nsi'
                    onChange={handleNamespaceChange}
                    value={namespaceValue}
                    hideIconButton={true}
                    autoFocus={true}
                />,
                <ExtensionListSearchfield
                    error={extensionFieldError}
                    key='ei'
                    onSearchChanged={handleExtensionChange}
                    searchQuery={extensionValue}
                    onSearchSubmit={findExtension}
                    placeholder='Extension'
                    hideIconButton={true}
                    autoFocus={false}
                />,
                <Button key='btn' variant='contained' onClick={findExtension}>
                    Search Extension
                </Button>,
                error ? <Typography color='error'>{error}</Typography> : ''
            ]}
            listContainer={extension ? <ExtensionVersionContainer onRemove={onRemove} extension={extension} /> : ''}
            loading={loading}
        />
    );
};
