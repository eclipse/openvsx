import React, { FunctionComponent, useState, useContext } from 'react';
import { Typography, Box, Button } from '@material-ui/core';

import { NamespaceDetail } from '../user/user-settings-namespace-detail';
import { DelayedLoadIndicator } from '../../components/delayed-load-indicator';
import { Namespace, isError } from '../../extension-registry-types';
import { PageSettingsContext, ServiceContext } from '../../default/default-app';
import { UserContext } from '../../main';
import { NamespaceInput } from './namespace-input';
import { SearchListContainer } from './search-list-container';

interface NamespaceAdminProps {
    handleError: (err: {}) => void
}

export const NamespaceAdmin: FunctionComponent<NamespaceAdminProps> = props => {
    const [loading, setLoading] = useState(false);
    const setLoadingState = (loadingState: boolean) => {
        setLoading(loadingState);
    };

    const pageSettings = useContext(PageSettingsContext);
    const service = useContext(ServiceContext);
    const user = useContext(UserContext);

    const [currentNamespace, setCurrentNamespace] = useState<Namespace | undefined>();
    const [notFound, setNotFound] = useState('');
    const fetchNamespace = async (namespaceName: string) => {
        if (namespaceName !== '') {
            setLoading(true);
            const namespace = await service.findNamespace(namespaceName);
            if (isError(namespace)) {
                setNotFound(namespaceName);
                setCurrentNamespace(undefined);
            } else {
                setNotFound('');
                setCurrentNamespace(namespace);
            }
            setLoading(false);
        } else {
            setNotFound('');
            setCurrentNamespace(undefined);
        }
    };

    return (<>
        <DelayedLoadIndicator loading={loading} />
        <SearchListContainer
            searchContainer={
                <NamespaceInput onSubmit={fetchNamespace} />
            }
            listContainer={
                currentNamespace && pageSettings && user ?
                    <NamespaceDetail
                        setLoadingState={setLoadingState}
                        handleError={props.handleError}
                        namespace={currentNamespace}
                        pageSettings={pageSettings}
                        service={service}
                        user={user}
                    />
                    : notFound ?
                        <Box>
                            <Typography variant='body1'>
                                Namespace {notFound} not found. Do you want to create it?
                                </Typography>
                            <Button>Create Namespace {notFound}</Button>

                        </Box>
                        : ''
            }
        />
    </>);
};