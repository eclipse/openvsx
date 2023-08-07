/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import React, { FunctionComponent, useState, useContext, useEffect } from 'react';
import { SearchListContainer } from './search-list-container';
import { ExtensionListSearchfield } from '../extension-list/extension-list-searchfield';
import { Button, Typography } from '@mui/material';
import { MainContext } from '../../context';
import { isError, Extension } from '../../extension-registry-types';
import { ExtensionVersionContainer } from './extension-version-container';
import { StyledInput } from './namespace-input';

export const ExtensionAdmin: FunctionComponent = props => {
    const abortController = new AbortController();
    useEffect(() => {
        return () => {
            abortController.abort();
        };
    }, []);

    const [loading, setLoading] = useState(false);

    const [extensionValue, setExtensionValue] = useState('');
    const handleExtensionChange = (value: string) => {
        setExtensionValue(value);
    };

    const [namespaceValue, setNamespaceValue] = useState('');
    const handleNamespaceChange = (value: string) => {
        setNamespaceValue(value);
    };

    const [error, setError] = useState('');

    const [extensionFieldError, setExtensionFieldError] = useState(false);
    const [namespaceFieldError, setNamespaceFieldError] = useState(false);

    const { service, handleError } = useContext(MainContext);
    const [extension, setExtension] = useState<Extension | undefined>(undefined);
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
        try {
            setLoading(true);
            const extensionDetail = await service.admin.getExtension(abortController, namespaceValue, extensionValue);
            if (isError(extensionDetail)) {
                throw extensionDetail;
            }
            setExtension(extensionDetail);
            setError('');
            setLoading(false);
        } catch (err) {
            if (err && err.status === 404) {
                setError(`Extension not found: ${namespaceValue}.${extensionValue}`);
                setExtension(undefined);
            } else {
                handleError(err);
            }
            setLoading(false);
        }
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
                <ExtensionVersionContainer onUpdate={findExtension} extension={extension} />
                : ''
        }
        loading={loading}
    />;
};