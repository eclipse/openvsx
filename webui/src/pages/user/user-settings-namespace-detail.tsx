/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import React, { FunctionComponent } from 'react';
import { makeStyles, Grid, Link, Paper, Box } from '@material-ui/core';
import WarningIcon from '@material-ui/icons/Warning';
import { UserNamespaceExtensionListContainer } from './user-namespace-extension-list';
import { Namespace, UserData } from '../../extension-registry-types';
import { UserNamespaceMemberList } from './user-namespace-member-list';

export interface NamespaceDetailConfig {
    defaultMemberRole?: 'contributor' | 'owner';
}
export const NamespaceDetailConfigContext = React.createContext<NamespaceDetailConfig>({});

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
    lightTheme: {
        color: '#333',
    },
    darkTheme: {
        color: '#fff',
    },
    banner: {
        maxWidth: '800px',
        margin: `0 ${theme.spacing(6)}px ${theme.spacing(4)}px ${theme.spacing(6)}px`,
        padding: theme.spacing(2),
        display: 'flex',
        [theme.breakpoints.down('sm')]: {
            margin: `0 0 ${theme.spacing(2)}px 0`,
        }
    },
    warningLight: {
        backgroundColor: theme.palette.warning.light,
        color: '#000',
        '& a': {
            color: '#000',
            textDecoration: 'underline'
        }
    },
    warningDark: {
        backgroundColor: theme.palette.warning.dark,
        color: '#fff',
        '& a': {
            color: '#fff',
            textDecoration: 'underline'
        }
    }
}));

export const NamespaceDetail: FunctionComponent<NamespaceDetail.Props> = props => {
    const classes = useStyles();
    return <>
        <Grid container direction='column' spacing={4} className={classes.namespaceDetailContainer}>
            {
                !props.namespace.verified && props.namespaceAccessUrl
                ? <Grid item>
                    <Paper className={`${classes.banner} ${props.theme === 'dark' ? classes.warningDark : classes.warningLight} ${props.theme === 'dark' ? classes.darkTheme : classes.lightTheme}`}>
                        <WarningIcon fontSize='large' />
                        <Box ml={1}>
                            This namespace is not verified. <Link
                                href={props.namespaceAccessUrl}
                                target='_blank' >
                                See the documentation
                            </Link> to learn about claiming namespaces.
                        </Box>
                    </Paper>
                </Grid>
                : null
            }
            {
                props.namespace.membersUrl
                ? <Grid item>
                    <UserNamespaceMemberList
                        setLoadingState={props.setLoadingState}
                        namespace={props.namespace}
                        filterUsers={props.filterUsers}
                        fixSelf={props.fixSelf} />
                </Grid>
                : null
            }
            <Grid item>
                <UserNamespaceExtensionListContainer
                    namespace={props.namespace}
                />
            </Grid>
        </Grid>
    </>;
};

export namespace NamespaceDetail {
    export interface Props {
        namespace: Namespace;
        filterUsers: (user: UserData) => boolean;
        fixSelf: boolean;
        setLoadingState: (loading: boolean) => void;
        namespaceAccessUrl?: string;
        theme?: string;
    }
}
