import React, { FunctionComponent } from 'react';
import { makeStyles, Grid } from '@material-ui/core';
import { UserNamespaceExtensionList } from './user-namespace-extension-list';
import { Namespace, UserData } from '../../extension-registry-types';
import { ExtensionRegistryService, PageSettings } from '../..';
import { ErrorResponse } from '../../server-request';
import { UserNamespaceMemberList } from './user-namespace-member-list';

const useStyles = makeStyles((theme) => ({
    namespaceDetailContainer: {
        flex: 5,
        padding: theme.spacing(0, 1),
        [theme.breakpoints.only('md')]: {
            width: '80%'
        },
        [theme.breakpoints.down('sm')]: {
            width: '100%'
        }
    },
    extensionListContainer: {
        marginTop: theme.spacing(4)
    }
}));

export interface NamespaceProps {
    namespace: Namespace;
    user: UserData;
    service: ExtensionRegistryService;
    handleError: (err: Error | Partial<ErrorResponse>) => void;
    pageSettings: PageSettings;
    setLoadingState: (loading: boolean) => void;
}

export const NamespaceDetail: FunctionComponent<NamespaceProps> = props => {
    const classes = useStyles();

    return <>
        <Grid container direction='column' spacing={4} className={classes.namespaceDetailContainer}>
            <Grid item>
                <UserNamespaceMemberList
                    setLoadingState={props.setLoadingState}
                    namespace={props.namespace}
                    handleError={props.handleError} />
            </Grid>
            <Grid item>
                <UserNamespaceExtensionList
                    namespace={props.namespace}
                    service={props.service}
                    setError={props.handleError}
                    pageSettings={props.pageSettings}
                />
            </Grid>
        </Grid>
    </>;
};
