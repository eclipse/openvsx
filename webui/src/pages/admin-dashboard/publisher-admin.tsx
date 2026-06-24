/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import { FunctionComponent, useContext, createContext, useEffect, useCallback, ReactNode } from 'react';
import { Typography, Box } from '@mui/material';
import { useParams, useNavigate } from 'react-router-dom';
import { MainContext } from '../../context';
import { StyledInput } from './namespace-input';
import { SearchListContainer } from './search-list-container';
import { PublisherDetails } from './publisher-details';
import { AdminDashboardRoutes } from './admin-dashboard-routes';
import { usePublisherInfo } from './use-publisher-admin';

// eslint-disable-next-line react-refresh/only-export-components
export const UpdateContext = createContext({ handleUpdate: () => {} });

export const PublisherAdmin: FunctionComponent = () => {
    const { publisher: publisherParam } = useParams<{ publisher: string }>();
    const navigate = useNavigate();
    const { pageSettings, user, handleError } = useContext(MainContext);

    const { data: publisher, isFetching: loading, error, refetch } = usePublisherInfo(publisherParam ?? '');

    const is404 = !!error && (error as { status?: number }).status === 404;
    const notFound = is404 ? (publisherParam ?? '') : '';

    // Non-404 lookup failures keep flowing through the global error dialog.
    useEffect(() => {
        if (error && !is404) {
            handleError(error);
        }
    }, [error, is404, handleError]);

    const handleSubmit = (inputValue: string) => {
        if (inputValue) {
            navigate(`${AdminDashboardRoutes.PUBLISHER_ADMIN}/${inputValue}`);
        } else {
            navigate(AdminDashboardRoutes.PUBLISHER_ADMIN);
        }
    };

    const handleUpdate = useCallback(() => {
        refetch();
    }, [refetch]);

    let listContainer: ReactNode = '';
    if (publisher && pageSettings && user) {
        listContainer = (
            <UpdateContext.Provider value={{ handleUpdate }}>
                <PublisherDetails publisherInfo={publisher} />
            </UpdateContext.Provider>
        );
    } else if (notFound) {
        listContainer = (
            <Box display='flex' flexDirection='column'>
                <Typography variant='body1' color='error'>
                    Publisher {notFound} not found.
                </Typography>
            </Box>
        );
    }

    return (
        <SearchListContainer
            searchContainer={[
                <StyledInput
                    placeholder='Publisher Name'
                    key='pi'
                    value={publisherParam || ''}
                    onSubmit={handleSubmit}
                    onChange={() => {}}
                />
            ]}
            listContainer={listContainer}
            loading={loading}
        />
    );
};
