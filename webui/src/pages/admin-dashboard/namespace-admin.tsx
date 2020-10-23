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
import { Typography, Box, Button } from '@material-ui/core';

import { NamespaceDetail, NamespaceDetailConfigContext } from '../user/user-settings-namespace-detail';
import { DelayedLoadIndicator } from '../../components/delayed-load-indicator';
import { Namespace } from '../../extension-registry-types';
import { MainContext } from '../../context';
import { StyledInput } from './namespace-input';
import { SearchListContainer } from './search-list-container';

export const NamespaceAdmin: FunctionComponent = props => {
    const [loading, setLoading] = useState(false);
    const setLoadingState = (loadingState: boolean) => {
        setLoading(loadingState);
    };

    const { pageSettings, service, user, handleError } = useContext(MainContext);

    const [currentNamespace, setCurrentNamespace] = useState<Namespace | undefined>();
    const [notFound, setNotFound] = useState('');
    const fetchNamespace = async (namespaceName: string) => {
        try {
            if (namespaceName !== '') {
                setLoading(true);
                const namespace = await service.admin.findNamespace(namespaceName);
                setNotFound('');
                setCurrentNamespace(namespace);
                setLoading(false);
            } else {
                setNotFound('');
                setCurrentNamespace(undefined);
            }
        } catch (err) {
            if (err && err.status && err.status === 404) {
                setNotFound(namespaceName);
                setCurrentNamespace(undefined);
            } else {
                handleError(err);
            }
            setLoading(false);
        }
    };

    const [inputValue, setInputValue] = useState('');
    const onChangeInput = (name: string) => {
        setInputValue(name);
    };

    const onCreate = async () => {
        await service.admin.createNamespace({
            name: inputValue
        });
        await fetchNamespace(inputValue);
    };

    return (<>
        <DelayedLoadIndicator loading={loading} />
        <SearchListContainer
            searchContainer={
                [<StyledInput key='nsi' placeholder='Namespace' onSubmit={fetchNamespace} onChange={onChangeInput} />]
            }
            listContainer={
                currentNamespace && pageSettings && user ?
                    <NamespaceDetailConfigContext.Provider value={{ defaultMemberRole: 'owner' }}>
                        <NamespaceDetail
                            setLoadingState={setLoadingState}
                            namespace={currentNamespace}
                            filterUsers={() => true}
                        />
                    </NamespaceDetailConfigContext.Provider>
                    : notFound ?
                        <Box display='flex' flexDirection='column' justifyContent='center' alignItems='center'>
                            <Typography variant='body1'>
                                Namespace {notFound} not found. Do you want to create it?
                            </Typography>
                            <Button variant='contained' color='primary' onClick={onCreate}>Create Namespace {notFound}</Button>
                        </Box>
                        : ''
            }
        />
    </>);
};