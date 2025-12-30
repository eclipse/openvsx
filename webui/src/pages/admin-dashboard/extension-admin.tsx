/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import React, { FunctionComponent, useState } from 'react';
import { SearchListContainer } from './search-list-container';
import { ExtensionListSearchfield } from '../extension-list/extension-list-searchfield';
import { Button, Typography } from '@mui/material';
import { TargetPlatformVersion } from '../../extension-registry-types';
import { ExtensionVersionContainer } from './extension-version-container';
import { StyledInput } from './namespace-input';
import { useAdminDeleteExtensionsMutation, useAdminGetExtensionQuery } from '../../store/api';

export const ExtensionAdmin: FunctionComponent = props => {
    const [extensionValue, setExtensionValue] = useState('');
    const [error, setError] = useState('');
    const [extensionFieldError, setExtensionFieldError] = useState(false);
    const [namespaceFieldError, setNamespaceFieldError] = useState(false);
    const [namespaceValue, setNamespaceValue] = useState('');

    const { data: extension, isLoading } = useAdminGetExtensionQuery({ namespace: namespaceValue, extension: extensionValue }, { skip: !namespaceValue || !extensionValue });
    const [deleteExtensions] = useAdminDeleteExtensionsMutation();

    const handleExtensionChange = (value: string) => {
        setExtensionValue(value);
    };

    const handleNamespaceChange = (value: string) => {
        setNamespaceValue(value);
    };

    const findExtension = async () => {
        if (!namespaceValue) {
            setNamespaceFieldError(true);
            setError('Name of Namespace is mandatory');
            return;
        }
        setNamespaceFieldError(false);
        if (!extensionValue) {
            setExtensionFieldError(true);
            setError('Name of Extension is mandatory');
            return;
        }
        setExtensionFieldError(false);
    };

    const onRemove = async (targetPlatformVersions?: TargetPlatformVersion[]) => {
        if (extension == null) {
            return;
        }

        await deleteExtensions({ namespace: extension.namespace, extension: extension.name, targetPlatformVersions: targetPlatformVersions?.map(({ version, targetPlatform }) => ({ version, targetPlatform })) });
    };

    return <SearchListContainer
        searchContainer={[
            <StyledInput
                placeholder='Namespace'
                error={namespaceFieldError}
                key='nsi'
                onChange={handleNamespaceChange}
                hideIconButton={true}
                autoFocus={true} />,
            <ExtensionListSearchfield
                error={extensionFieldError}
                key='ei'
                onSearchChanged={handleExtensionChange}
                searchQuery={extensionValue}
                onSearchSubmit={findExtension}
                placeholder='Extension'
                hideIconButton={true}
                autoFocus={false} />,
            <Button key='btn' variant='contained' onClick={findExtension}>Search Extension</Button>,
            error ? <Typography color='error'>{error}</Typography> : ''
        ]}
        listContainer={
            extension ?
                <ExtensionVersionContainer onRemove={onRemove} extension={extension} />
                : ''
        }
        loading={isLoading}
    />;
};