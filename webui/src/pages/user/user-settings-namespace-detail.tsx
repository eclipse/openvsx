/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import React, { useState, FunctionComponent } from 'react';
import { withRouter, RouteComponentProps } from 'react-router-dom';
import { makeStyles, Box, Button, Link, Paper, Grid, Typography } from '@material-ui/core';
import WarningIcon from '@material-ui/icons/Warning';
import { UserNamespaceExtensionListContainer } from './user-namespace-extension-list';
import { AdminDashboardRoutes } from '../admin-dashboard/admin-dashboard';
import { Namespace, UserData } from '../../extension-registry-types';
import { NamespaceChangeDialog } from '../admin-dashboard/namespace-change-dialog';
import { UserNamespaceMemberList } from './user-namespace-member-list';
import { UserNamespaceDetails } from './user-namespace-details';

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
    },
    changeButton: {
        [theme.breakpoints.down('md')]: {
            marginLeft: theme.spacing(2)
        }
    },
    namespaceHeader: {
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        marginBottom: theme.spacing(1),
        [theme.breakpoints.down('sm')]: {
            flexDirection: 'column',
            alignItems: 'center'
        }
    }
}));

export const NamespaceDetailComponent: FunctionComponent<NamespaceDetailComponent.Props> = props => {
    const classes = useStyles();
    const [changeDialogIsOpen, setChangeDialogIsOpen] = useState(false);
    const handleCloseChangeDialog = async () => {
        setChangeDialogIsOpen(false);
    };
    const handleOpenChangeDialog = () => {
        setChangeDialogIsOpen(true);
    };

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
            <Grid item>
                <Box className={classes.namespaceHeader}>
                    <Typography variant='h4'>{props.namespace.name}</Typography>
                    { props.location.pathname.startsWith(AdminDashboardRoutes.NAMESPACE_ADMIN)
                        ? <Button className={classes.changeButton} variant='outlined' onClick={handleOpenChangeDialog}>
                            Change Namespace
                        </Button>
                        : null
                    }
                </Box>
            </Grid>
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
                <UserNamespaceDetails namespace={props.namespace}/>
            </Grid>
            <Grid item>
                <UserNamespaceExtensionListContainer
                    namespace={props.namespace}
                />
            </Grid>
        </Grid>
        <NamespaceChangeDialog
            open={changeDialogIsOpen}
            onClose={handleCloseChangeDialog}
            namespace={props.namespace}
            setLoadingState={props.setLoadingState} />
    </>;
};

export namespace NamespaceDetailComponent {
    export interface Props extends RouteComponentProps {
        namespace: Namespace;
        filterUsers: (user: UserData) => boolean;
        fixSelf: boolean;
        setLoadingState: (loading: boolean) => void;
        namespaceAccessUrl?: string;
        theme?: string;
    }
}

export const NamespaceDetail = withRouter(NamespaceDetailComponent);
