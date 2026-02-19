/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import React, { FunctionComponent, createContext, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { Box, Button, Link, Paper, Grid, Typography } from '@mui/material';
import { styled, Theme } from '@mui/material/styles';
import WarningIcon from '@mui/icons-material/Warning';
import { UserNamespaceExtensionListContainer } from './user-namespace-extension-list';
import { AdminDashboardRoutes } from '../admin-dashboard/admin-dashboard';
import { Namespace, UserData, isError } from '../../extension-registry-types';
import { NamespaceChangeDialog } from '../admin-dashboard/namespace-change-dialog';
import { UserNamespaceMemberList } from './user-namespace-member-list';
import { UserNamespaceDetails } from './user-namespace-details';
import { MainContext } from '../../context';
import { ButtonWithProgress } from '../../components/button-with-progress';

export interface NamespaceDetailConfig {
    defaultMemberRole?: 'contributor' | 'owner';
}
export const NamespaceDetailConfigContext = createContext<NamespaceDetailConfig>({});

const NamespaceDetailContainer = styled(Grid)(({ theme }: { theme: Theme }) => ({
    flex: 5,
    padding: theme.spacing(0, 1),
    [theme.breakpoints.only('md')]: {
        width: '80%'
    },
    [theme.breakpoints.down('sm')]: {
        width: '100%'
    }
}));

const WarningPaper = styled(Paper)(({ theme }: { theme: Theme }) => ({
    maxWidth: '800px',
    margin: `0 ${theme.spacing(6)} ${theme.spacing(4)} ${theme.spacing(6)}`,
    padding: theme.spacing(2),
    display: 'flex',
    [theme.breakpoints.down('sm')]: {
        margin: `0 0 ${theme.spacing(2)} 0`,
    }
}));

const NamespaceHeader = styled(Box)(({ theme }: { theme: Theme }) => ({
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: theme.spacing(1),
    [theme.breakpoints.down('sm')]: {
        flexDirection: 'column',
        alignItems: 'center'
    }
}));

export const NamespaceDetail: FunctionComponent<NamespaceDetailProps> = props => {
    const [changeDialogIsOpen, setChangeDialogIsOpen] = useState(false);
    const [deleting, setDeleting] = useState(false);
    const { pathname } = useLocation();
    const navigate = useNavigate();
    const { service, handleError } = React.useContext(MainContext);

    const handleCloseChangeDialog = async () => {
        setChangeDialogIsOpen(false);
    };
    const handleOpenChangeDialog = () => {
        setChangeDialogIsOpen(true);
    };

    const handleDelete = async () => {
        if (!window.confirm(`Are you sure you want to delete the namespace "${props.namespace.name}" and all its extensions? This action cannot be undone.`)) {
            return;
        }
        
        try {
            setDeleting(true);
            props.setLoadingState(true);
            const abortController = new AbortController();
            const result = await service.admin.deleteNamespace(abortController, props.namespace.name);
            
            if (isError(result)) {
                throw result;
            }
            
            // Navigate back to namespace admin page after successful deletion
            navigate(AdminDashboardRoutes.NAMESPACE_ADMIN);
        } catch (err) {
            handleError(err);
        } finally {
            setDeleting(false);
            props.setLoadingState(false);
        }
    };

    const warningColor = props.theme === 'dark' ? '#fff' : '#151515';
    return <>
        <NamespaceDetailContainer container direction='column' spacing={4}>
            {
                !props.namespace.verified && props.namespaceAccessUrl
                ? <Grid item>
                    <WarningPaper
                        sx={{
                            backgroundColor: `warning.${props.theme}`,
                            color: warningColor,
                            '& a': {
                                color: warningColor,
                                textDecoration: 'underline'
                            }
                        }}>
                        <WarningIcon fontSize='large' />
                        <Box ml={1}>
                            This namespace is not verified. <Link
                                href={props.namespaceAccessUrl}
                                target='_blank' >
                                See the documentation
                            </Link> to learn about claiming namespaces.
                        </Box>
                    </WarningPaper>
                </Grid>
                : null
            }
            <Grid item>
                <NamespaceHeader>
                    <Typography variant='h4'>{props.namespace.name}</Typography>
                    { pathname.startsWith(AdminDashboardRoutes.NAMESPACE_ADMIN)
                        ? <Box sx={{ display: 'flex', gap: 1, ml: { xs: 2, sm: 2, md: 2, lg: 0, xl: 0 } }}>
                            <Button variant='outlined' onClick={handleOpenChangeDialog}>
                                Change Namespace
                            </Button>
                            <ButtonWithProgress
                                variant='outlined'
                                color='error'
                                working={deleting}
                                onClick={handleDelete}>
                                Delete Namespace
                            </ButtonWithProgress>
                        </Box>
                        : null
                    }
                </NamespaceHeader>
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
            {
                props.namespace.detailsUrl
                ? <Grid item>
                    <UserNamespaceDetails namespace={props.namespace}/>
                </Grid>
                : null
            }
            <Grid item>
                <UserNamespaceExtensionListContainer
                    namespace={props.namespace}
                />
            </Grid>
        </NamespaceDetailContainer>
        <NamespaceChangeDialog
            open={changeDialogIsOpen}
            onClose={handleCloseChangeDialog}
            namespace={props.namespace}
            setLoadingState={props.setLoadingState} />
    </>;
};

export interface NamespaceDetailProps {
    namespace: Namespace;
    filterUsers: (user: UserData) => boolean;
    fixSelf: boolean;
    setLoadingState: (loading: boolean) => void;
    namespaceAccessUrl?: string;
    theme?: string;
}