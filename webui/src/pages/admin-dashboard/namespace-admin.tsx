/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import React, { FunctionComponent, useState, useContext, ReactNode } from 'react';
import { Typography, Box } from '@mui/material';
import { NamespaceDetail, NamespaceDetailConfigContext } from '../user/user-settings-namespace-detail';
import { ButtonWithProgress } from '../../components/button-with-progress';
import { MainContext } from '../../context';
import { StyledInput } from './namespace-input';
import { SearchListContainer } from './search-list-container';
import { useAdminCreateNamespaceMutation, useAdminGetNamespaceQuery, useGetUserQuery } from '../../store/api';

export const NamespaceAdmin: FunctionComponent = props => {
    const { pageSettings } = useContext(MainContext);
    const [namespaceName, setNamespaceName] = useState<string>();
    const [inputValue, setInputValue] = useState('');
    const [creating, setCreating] = useState(false);

    const [createNamespace] = useAdminCreateNamespaceMutation();
    const { data: user } = useGetUserQuery();
    const { data: currentNamespace, isLoading } = useAdminGetNamespaceQuery(namespaceName as string, { skip: namespaceName == null });

    const fetchNamespace = (namespaceName: string) => {
        setNamespaceName(namespaceName ? namespaceName : undefined);
    };

    const onChangeInput = (name: string) => {
        setInputValue(name);
    };

    const onCreate = async () => {
        setCreating(true);
        await createNamespace({
            name: inputValue
        });
        setCreating(false);
    };

    let listContainer: ReactNode = '';
    if (currentNamespace && pageSettings && user) {
        listContainer = <NamespaceDetailConfigContext.Provider value={{ defaultMemberRole: 'owner' }}>
            <NamespaceDetail
                namespace={currentNamespace}
                filterUsers={() => true}
                fixSelf={false}
            />
        </NamespaceDetailConfigContext.Provider>;
    } else if (namespaceName && currentNamespace == null && !isLoading) {
        listContainer = <Box display='flex' flexDirection='column' justifyContent='center' alignItems='center'>
            <Typography variant='body1'>
                Namespace {namespaceName} not found. Do you want to create it?
            </Typography>
            <Box mt={3}>
                <ButtonWithProgress
                    working={creating}
                    onClick={onCreate}>
                    Create Namespace {namespaceName}
                </ButtonWithProgress>
            </Box>
        </Box>;
    }

    return <SearchListContainer
        searchContainer={
            [<StyledInput key='nsi' placeholder='Namespace' onSubmit={fetchNamespace} onChange={onChangeInput} />]
        }
        listContainer={listContainer}
        loading={isLoading}
    />;
};