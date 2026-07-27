/******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/

import { FunctionComponent, useContext, useState } from 'react';
import { Box, Button, Typography } from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import { Namespace, TrustedPublisher, TrustedPublisherProvider } from '../../../extension-registry-types';
import { DelayedLoadIndicator } from '../../../components/delayed-load-indicator';
import { MainContext } from '../../../context';
import { useReportedQuery } from '../../../hooks/use-reported-query';
import { useUserNamespaces } from '../use-user-namespaces';
import { PublisherList } from './publisher-list';
import { RegisterTrustedPublisherDialog, TrustedPublishingDocsLink } from './register-trusted-publisher-dialog';
import {
    useAllTrustedPublisherProviders,
    useAllTrustedPublishers,
    useDeleteTrustedPublisher
} from './use-trusted-publishers';

// Stable order across namespaces: namespace, then extension, then registration id.
const byNamespaceExtensionId = (a: TrustedPublisher, b: TrustedPublisher): number =>
    a.namespace.localeCompare(b.namespace) || a.extension.localeCompare(b.extension) || a.id - b.id;

// The backend sets trustedPublishingUrl only where the user may manage TP; the guard narrows the type too.
type TrustedPublishingNamespace = Namespace & { trustedPublishingUrl: string };
const canManageTrustedPublishers = (ns: Namespace): ns is TrustedPublishingNamespace =>
    Boolean(ns.trustedPublishingUrl);

// Providers are configured server-side, so the per-namespace lists overlap; keep each id once.
const dedupeById = (providers: readonly TrustedPublisherProvider[]): TrustedPublisherProvider[] =>
    providers.filter((provider, index, all) => all.findIndex(p => p.id === provider.id) === index);

/** Settings tab showing the trusted publishers of every namespace the user belongs to as one stack. */
export const UserSettingsTrustedPublishers: FunctionComponent = () => {
    const { handleError } = useContext(MainContext);
    const { data: namespaces = [], isLoading: namespacesLoading } = useReportedQuery(useUserNamespaces());

    // only probe namespaces the user may manage; the rest have no trustedPublishingUrl
    const trustedPublishingNamespaces = namespaces.filter(canManageTrustedPublishers);
    const { providersByUrl, isLoading: providersLoading } = useAllTrustedPublisherProviders(
        trustedPublishingNamespaces.map(ns => ns.trustedPublishingUrl)
    );
    const manageableNamespaces = trustedPublishingNamespaces.filter(
        ns => (providersByUrl[ns.trustedPublishingUrl] ?? []).length > 0
    );
    const { publishers: allPublishers, isLoading: publishersLoading } = useAllTrustedPublishers(
        manageableNamespaces.map(ns => ns.trustedPublishingUrl)
    );
    const { mutateAsync: deleteTrustedPublisher, isPending: deleting } = useDeleteTrustedPublisher();
    const [dialogOpen, setDialogOpen] = useState(false);

    const loading = namespacesLoading || providersLoading || publishersLoading || deleting;
    const publishers = [...allPublishers].sort(byNamespaceExtensionId);
    const providers = dedupeById(manageableNamespaces.flatMap(ns => providersByUrl[ns.trustedPublishingUrl] ?? []));

    const remove = async (publisher: TrustedPublisher) => {
        const trustedPublishingUrl = manageableNamespaces.find(
            ns => ns.name === publisher.namespace
        )?.trustedPublishingUrl;
        if (!trustedPublishingUrl) {
            return;
        }
        try {
            await deleteTrustedPublisher({ trustedPublishingUrl, id: publisher.id });
        } catch (err) {
            handleError(err);
        }
    };

    return (
        <>
            <Box
                sx={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    flexDirection: { xs: 'column', sm: 'column', md: 'row', lg: 'row', xl: 'row' },
                    alignItems: { xs: 'center', sm: 'center', md: 'normal', lg: 'normal', xl: 'normal' }
                }}>
                <Box>
                    <Typography variant='h5' gutterBottom>
                        Trusted Publishers
                    </Typography>
                </Box>
                {manageableNamespaces.length > 0 ? (
                    <Box sx={{ display: 'flex', flexWrap: 'wrap' }}>
                        <Box sx={{ mr: 1, mb: 1 }}>
                            <Button variant='outlined' startIcon={<AddIcon />} onClick={() => setDialogOpen(true)}>
                                Add a trusted publisher
                            </Button>
                        </Box>
                    </Box>
                ) : null}
            </Box>
            <Typography variant='body2' color='text.secondary' sx={{ mb: 2 }}>
                Let CI/CD workflows publish your extensions without long-lived access tokens. The workflow authenticates
                with a short-lived OIDC token at publish time, accepted only when its repository, workflow and (if set)
                environment match a registration. <TrustedPublishingDocsLink />
            </Typography>
            <DelayedLoadIndicator loading={loading} />
            {publishers.length > 0 ? (
                <PublisherList
                    publishers={publishers}
                    providers={providers}
                    loading={deleting}
                    rowDetail='namespace/extension'
                    onDelete={remove}
                />
            ) : !loading ? (
                <Typography variant='body1'>
                    {manageableNamespaces.length === 0
                        ? 'Trusted publishers are registered per namespace — create or join a namespace first.'
                        : 'No trusted publishers registered yet.'}
                </Typography>
            ) : null}
            <RegisterTrustedPublisherDialog
                open={dialogOpen}
                onClose={() => setDialogOpen(false)}
                namespaces={manageableNamespaces.map(ns => ({
                    name: ns.name,
                    extensionNames: Object.keys(ns.extensions),
                    trustedPublishingUrl: ns.trustedPublishingUrl
                }))}
                providers={providers}
            />
        </>
    );
};
