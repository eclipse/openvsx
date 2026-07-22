/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import { FunctionComponent, ReactNode, createContext } from 'react';
import { Box, Link, Paper, Grid, Typography } from '@mui/material';
import { styled, Theme } from '@mui/material/styles';
import WarningIcon from '@mui/icons-material/Warning';
import { NamespaceExtensionList, FetchNamespaceExtension } from './namespace-extension-list';
import { NamespaceMemberList } from './namespace-member-list';
import { NamespaceDetails } from './namespace-details';
import { Namespace, UserData } from '../../extension-registry-types';

export interface NamespaceDetailConfig {
    defaultMemberRole?: 'contributor' | 'owner';
}

// eslint-disable-next-line react-refresh/only-export-components
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
        margin: `0 0 ${theme.spacing(2)} 0`
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

/**
 * Reusable view of a namespace: its optional not-verified warning, header, member list, details and
 * extension list. It is page-agnostic — page-specific concerns are injected:
 *  - `headerActions` lets a host render extra buttons in the header (e.g. the admin
 *    "Change Namespace"/"Delete" actions together with their dialogs).
 *  - `fetchExtension` selects the endpoint used to load each extension (public vs. admin).
 */
export const NamespaceDetailView: FunctionComponent<NamespaceDetailViewProps> = props => {
    const warningColor = props.theme === 'dark' ? '#fff' : '#151515';
    return (
        <NamespaceDetailContainer container direction='column' spacing={4}>
            {!props.namespace.verified && props.namespaceAccessUrl ? (
                <Grid item>
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
                            This namespace is not verified.{' '}
                            <Link href={props.namespaceAccessUrl} target='_blank'>
                                See the documentation
                            </Link>{' '}
                            to learn about claiming namespaces.
                        </Box>
                    </WarningPaper>
                </Grid>
            ) : null}
            <Grid item>
                <NamespaceHeader>
                    <Typography variant='h4'>{props.namespace.name}</Typography>
                    {props.headerActions}
                </NamespaceHeader>
            </Grid>
            {props.namespace.membersUrl ? (
                <Grid item>
                    <NamespaceMemberList
                        setLoadingState={props.setLoadingState}
                        namespace={props.namespace}
                        filterUsers={props.filterUsers}
                        fixSelf={props.fixSelf}
                    />
                </Grid>
            ) : null}
            {props.namespace.detailsUrl ? (
                <Grid item>
                    <NamespaceDetails namespace={props.namespace} />
                </Grid>
            ) : null}
            <Grid item>
                <NamespaceExtensionList
                    namespace={props.namespace}
                    fetchExtension={props.fetchExtension}
                    routePrefix={props.extensionRoutePrefix}
                />
            </Grid>
        </NamespaceDetailContainer>
    );
};

export interface NamespaceDetailViewProps {
    namespace: Namespace;
    filterUsers: (user: UserData) => boolean;
    fixSelf: boolean;
    setLoadingState: (loading: boolean) => void;
    namespaceAccessUrl?: string;
    theme?: string;
    // Extra actions rendered in the header (e.g. the admin "Change Namespace"/"Delete" buttons and
    // their dialogs). Supplied by the host page so this view stays free of page-specific concerns.
    headerActions?: ReactNode;
    // Endpoint used to retrieve each extension's detail; forwarded to the extension list. Defaults to
    // the public registry API when omitted (e.g. the user surface). The admin surface passes the admin
    // endpoint so inactive/soft-deleted extensions are shown too.
    fetchExtension?: FetchNamespaceExtension;
    // Base route each extension card links to. Supplied by the host: the user surface passes the user
    // settings extension route, the admin surface passes the admin extension route.
    extensionRoutePrefix: string;
}
