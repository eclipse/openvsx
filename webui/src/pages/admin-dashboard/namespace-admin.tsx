/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import { FunctionComponent, useState, useContext, useEffect, ReactNode } from 'react';
import { Typography, Box, Button } from '@mui/material';
import { useParams, useNavigate } from 'react-router';
import { NamespaceDetailView, NamespaceDetailConfigContext } from '../../components/namespace/namespace-detail-view';
import { ButtonWithProgress } from '../../components/button-with-progress';
import { MainContext } from '../../context';
import { StyledInput } from './namespace-input';
import { SearchListContainer } from './search-list-container';
import { AdminDashboardRoutes } from './admin-dashboard-routes';
import { NamespaceChangeDialog } from './namespace-change-dialog';
import { NamespaceDeleteDialog } from './namespace-delete-dialog';
import { useAdminNamespace, useClearAdminNamespace, useCreateNamespace } from './use-namespace-admin';

export const NamespaceAdmin: FunctionComponent = () => {
    const { service, pageSettings, user, handleError } = useContext(MainContext);
    const { namespace: nsParam } = useParams<{ namespace?: string }>();
    const navigate = useNavigate();

    const [searchName, setSearchName] = useState(nsParam ?? '');
    const [inputValue, setInputValue] = useState(nsParam ?? '');
    // The namespace detail view can drive the loading indicator while it performs its own work.
    const [detailLoading, setDetailLoading] = useState(false);
    const [changeDialogIsOpen, setChangeDialogIsOpen] = useState(false);
    const [deleteDialogIsOpen, setDeleteDialogIsOpen] = useState(false);

    const { data: currentNamespace, isFetching, error, refetch } = useAdminNamespace(searchName);
    const { mutateAsync: createNamespace, isPending: isCreatingNamespace } = useCreateNamespace();
    const clearNamespace = useClearAdminNamespace();

    const is404 = !!error && (error as { status?: number }).status === 404;
    const notFound = is404 ? searchName : '';

    // Non-404 lookup failures keep flowing through the global error dialog.
    useEffect(() => {
        if (error && !is404) {
            handleError(error);
        }
    }, [error, is404, handleError]);

    // Keep the URL in sync with the searched namespace so the view is deep-linkable.
    useEffect(() => {
        const target = searchName
            ? `${AdminDashboardRoutes.NAMESPACE_ADMIN}/${encodeURIComponent(searchName)}`
            : AdminDashboardRoutes.NAMESPACE_ADMIN;
        navigate(target, { replace: true });
    }, [searchName, navigate]);

    const fetchNamespace = (namespaceName: string) => {
        if (namespaceName && namespaceName === searchName) {
            // Re-submitting the same name should force a fresh lookup.
            refetch();
        } else {
            setSearchName(namespaceName);
        }
    };

    const onChangeInput = (name: string) => {
        setInputValue(name);
    };

    const handleDeleteNamespace = () => {
        clearNamespace(searchName);
        setSearchName('');
    };

    const onCreate = async () => {
        try {
            await createNamespace(inputValue);
            setSearchName(inputValue);
        } catch (err) {
            handleError(err);
        }
    };

    const loading = isFetching || detailLoading;

    let listContainer: ReactNode = '';
    if (currentNamespace && pageSettings && user) {
        const headerActions = (
            <Box>
                <Button
                    sx={{ ml: { xs: 2, sm: 2, md: 2, lg: 0, xl: 0 } }}
                    variant='outlined'
                    onClick={() => setChangeDialogIsOpen(true)}>
                    Change Namespace
                </Button>
                {Object.keys(currentNamespace.extensions).length === 0 && (
                    <Button
                        variant='outlined'
                        sx={{ color: 'error.main', height: 36, ml: { xs: 2 } }}
                        onClick={() => setDeleteDialogIsOpen(true)}>
                        Delete
                    </Button>
                )}
            </Box>
        );
        listContainer = (
            <NamespaceDetailConfigContext.Provider value={{ defaultMemberRole: 'owner' }}>
                <NamespaceDetailView
                    setLoadingState={setDetailLoading}
                    namespace={currentNamespace}
                    filterUsers={() => true}
                    fixSelf={false}
                    headerActions={headerActions}
                    extensionRoutePrefix={AdminDashboardRoutes.EXTENSION_ADMIN}
                    fetchExtension={(abortController, extension) =>
                        service.admin.getExtension(abortController, currentNamespace.name, extension.name)
                    }
                />
                <NamespaceChangeDialog
                    open={changeDialogIsOpen}
                    onClose={() => setChangeDialogIsOpen(false)}
                    namespace={currentNamespace}
                    setLoadingState={setDetailLoading}
                />
                <NamespaceDeleteDialog
                    open={deleteDialogIsOpen}
                    onClose={() => setDeleteDialogIsOpen(false)}
                    onDelete={() => {
                        setDeleteDialogIsOpen(false);
                        handleDeleteNamespace();
                    }}
                    namespace={currentNamespace}
                    setLoadingState={setDetailLoading}
                />
            </NamespaceDetailConfigContext.Provider>
        );
    } else if (notFound) {
        listContainer = (
            <Box display='flex' flexDirection='column' justifyContent='center' alignItems='center'>
                <Typography variant='body1'>Namespace {notFound} not found. Do you want to create it?</Typography>
                <Box mt={3}>
                    <ButtonWithProgress working={isCreatingNamespace} onClick={onCreate}>
                        Create Namespace {notFound}
                    </ButtonWithProgress>
                </Box>
            </Box>
        );
    }

    return (
        <SearchListContainer
            searchContainer={[
                <StyledInput
                    key='nsi'
                    placeholder='Namespace'
                    value={inputValue}
                    onSubmit={fetchNamespace}
                    onChange={onChangeInput}
                />
            ]}
            listContainer={listContainer}
            loading={loading}
        />
    );
};
