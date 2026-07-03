/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import { FunctionComponent, useContext, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Button, Typography } from '@mui/material';

import { MainContext } from '../../context';
import { SearchListContainer } from './search-list-container';
import { StyledInput } from './namespace-input';
import { ExtensionListSearchfield } from '../extension-list/extension-list-searchfield';
import { useAdminExtension, useDeleteExtension } from './use-extension-admin';
import { ExtensionDetailView } from '../../components/extension/extension-detail-view';
import { AdminDashboardRoutes } from './admin-dashboard-routes';
import { createRoute } from '../../utils';

export const ExtensionAdmin: FunctionComponent = () => {
    const { handleError } = useContext(MainContext);
    const navigate = useNavigate();

    const { namespace: nsParam, extension: extParam } = useParams<{ namespace?: string; extension?: string }>();

    const [namespaceValue, setNamespaceValue] = useState(nsParam ?? '');
    const [extensionValue, setExtensionValue] = useState(extParam ?? '');

    useEffect(() => {
        setNamespaceValue(nsParam ?? '');
        setExtensionValue(extParam ?? '');
    }, [nsParam, extParam]);

    const [validationError, setValidationError] = useState('');
    const [extensionFieldError, setExtensionFieldError] = useState(false);
    const [namespaceFieldError, setNamespaceFieldError] = useState(false);

    const target = nsParam && extParam ? { namespace: nsParam, extension: extParam } : null;

    const { data, isFetching: loading, error: queryError, refetch } = useAdminExtension(target);
    const { mutateAsync: deleteExtension } = useDeleteExtension();

    const is404 = !!queryError && (queryError as { status?: number }).status === 404;
    const extension = queryError ? undefined : data;
    const fetchError = is404 ? `Extension not found: ${target?.namespace}.${target?.extension}` : '';
    const error = validationError || fetchError;

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
        if (nsParam === namespaceValue && extParam === extensionValue) {
            refetch();
        } else {
            navigate(createRoute([AdminDashboardRoutes.EXTENSION_ADMIN, namespaceValue, extensionValue]));
        }
    };

    return (
        <SearchListContainer
            searchContainer={[
                <StyledInput
                    placeholder='Namespace'
                    error={namespaceFieldError}
                    key='nsi'
                    onChange={setNamespaceValue}
                    value={namespaceValue}
                    hideIconButton={true}
                    autoFocus={true}
                />,
                <ExtensionListSearchfield
                    error={extensionFieldError}
                    key='ei'
                    onSearchChanged={setExtensionValue}
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
            listContainer={
                extension ? (
                    <ExtensionDetailView
                        extension={extension}
                        onRemoveVersion={targets =>
                            deleteExtension({
                                namespace: extension.namespace,
                                extension: extension.name,
                                targetPlatformVersions: targets
                            })
                        }
                        onVersionDeleted={refetch}
                    />
                ) : (
                    ''
                )
            }
            loading={loading}
        />
    );
};
