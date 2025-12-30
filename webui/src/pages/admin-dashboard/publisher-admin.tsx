/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import React, { FunctionComponent, useState, useContext, createContext, ReactNode } from 'react';
import { Typography, Box } from '@mui/material';
import { PublisherInfo } from '../../extension-registry-types';
import { MainContext } from '../../context';
import { StyledInput } from './namespace-input';
import { SearchListContainer } from './search-list-container';
import { PublisherDetails } from './publisher-details';
import { apiSlice, useGetUserQuery } from '../../store/api';

export const UpdateContext = createContext({ handleUpdate: () => { } });
export const PublisherAdmin: FunctionComponent = props => {
    const { data: user } = useGetUserQuery();
    const { pageSettings } = useContext(MainContext);

    const [getPublisherInfo] = apiSlice.useLazyAdminGetPublisherInfoQuery();

    const [loading, setLoading] = useState(false);
    const [inputValue, setInputValue] = useState('');
    const onChangeInput = (name: string) => {
        setInputValue(name);
    };

    const [publisher, setPublisher] = useState<PublisherInfo | undefined>();
    const [notFound, setNotFound] = useState('');
    const fetchPublisher = async () => {
        const publisherName = inputValue;
        setLoading(true);
        if (publisherName !== '') {
            const { data: publisher } = await getPublisherInfo({ provider: 'github', login: publisherName });
            if (publisher != null) {
                setNotFound('');
                setPublisher(publisher);
            } else {
                setNotFound(publisherName);
                setPublisher(undefined);
            }
        } else {
            setNotFound('');
            setPublisher(undefined);
        }
        setLoading(false);
    };

    const handleUpdate = () => {
        fetchPublisher();
    };

    let listContainer: ReactNode = '';
    if (publisher && pageSettings && user) {
        listContainer = <UpdateContext.Provider value={{ handleUpdate }}>
            <PublisherDetails publisherInfo={publisher} />
        </UpdateContext.Provider>;
    } else if (notFound) {
        listContainer = <Box display='flex' flexDirection='column'>
            <Typography variant='body1' color='error'>
                Publisher {notFound} not found.
            </Typography>
        </Box>;
    }

    return <SearchListContainer
        searchContainer={
            [<StyledInput placeholder='Publisher Name' key='pi' onSubmit={fetchPublisher} onChange={onChangeInput} />]
        }
        listContainer={listContainer}
        loading={loading}
    />;
};