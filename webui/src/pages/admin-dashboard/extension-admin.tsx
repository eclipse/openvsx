/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import React, { FunctionComponent, useState, useContext } from 'react';
import { SearchListContainer } from './search-list-container';
import { ExtensionListSearchfield } from '../extension-list/extension-list-searchfield';
import { Button } from '@material-ui/core';
import { NamespaceInput } from './namespace-input';
import { ServiceContext } from '../../default/default-app';
import { ErrorHandlerContext } from '../../main';
import { handleError } from '../../utils';
import { isError, Extension } from '../../extension-registry-types';
import { ExtensionVersionContainer } from './extension-version-container';

export const ExtensionAdmin: FunctionComponent = props => {
    const errorContext = useContext(ErrorHandlerContext);

    const [extensionValue, setExtensionValue] = useState('');
    const handleExtensionChange = (value: string) => {
        setExtensionValue(value);
    };
    const handleExtensionSubmit = (value: string) => {
        findExtension();
    };

    const [namespaceValue, setNamespaceValue] = useState('');
    const handleNamespaceChange = (value: string) => {
        setNamespaceValue(value);
    };

    const handleSubmit = (event: React.MouseEvent<HTMLButtonElement, MouseEvent>) => {
        findExtension();
    };

    const [extensionFieldError, setExtensionFieldError] = useState(false);
    const [namespaceFieldError, setNamespaceFieldError] = useState(false);

    const handleUpdate = () => findExtension();
    const service = useContext(ServiceContext);
    const [extension, setExtension] = useState<Extension | undefined>(undefined);
    const findExtension = async () => {
        const extensionUrl = service.getExtensionApiUrl({
            namespace: namespaceValue,
            name: extensionValue
        });
        try {
            if (!namespaceValue) {
                setNamespaceFieldError(true);
                throw ({ message: 'Name of Namespace is mandatory' });
            } else {
                setNamespaceFieldError(false);
            }
            if (!extensionValue) {
                setExtensionFieldError(true);
                throw ({ message: 'Name of Extension is mandatory' });
            } else {
                setExtensionFieldError(false);
            }
            const extensionDetail = await service.getExtensionDetail(extensionUrl);
            if (isError(extensionDetail)) {
                setExtension(undefined);
                throw (extensionDetail);
            }
            setExtension(extensionDetail);
        } catch (err) {
            errorContext ? errorContext.handleError(err) : handleError(err);
        }
    };

    return (<>
        <SearchListContainer
            searchContainer={[
                <NamespaceInput
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
                    onSearchSubmit={handleExtensionSubmit}
                    placeholder='Extension'
                    hideIconButton={true}
                    autoFocus={false} />,
                <Button key='btn' variant='contained' onClick={handleSubmit}>Search Extension</Button>
            ]}
            listContainer={
                extension ?
                    <ExtensionVersionContainer onUpdate={handleUpdate} extension={extension} />
                    : ''
            }
        />
    </>);
};