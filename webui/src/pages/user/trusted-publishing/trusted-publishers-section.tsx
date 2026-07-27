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
import { Box, Button, Divider, Grid, IconButton, Stack, Typography } from '@mui/material';
import { styled } from '@mui/material/styles';
import AddIcon from '@mui/icons-material/Add';
import CloseIcon from '@mui/icons-material/Close';
import { useReportedQuery } from '../../../hooks/use-reported-query';
import { MainContext } from '../../../context';
import { Namespace, TrustedPublisher, TrustedPublisherProvider, UrlString } from '../../../extension-registry-types';
import { RegisterTrustedPublisherDialog, TrustedPublishingDocsLink } from './register-trusted-publisher-dialog';
import { PublisherList } from './publisher-list';
import { TrustedPublisherProviderIcon } from './trusted-publisher-provider-icon';
import {
    useDeleteTrustedPublisher,
    useTrustedPublisherProviders,
    useTrustedPublishers
} from './use-trusted-publishers';

const AddPublisherPanel = styled(Box)(({ theme }) => ({
    position: 'relative',
    marginTop: theme.spacing(2),
    padding: theme.spacing(3),
    border: `2px dashed ${theme.palette.divider}`,
    borderRadius: theme.shape.borderRadius,
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    gap: theme.spacing(1.5),
    textAlign: 'center'
}));

const CloseAddPanelButton = styled(IconButton)({
    position: 'absolute',
    top: '0.5rem',
    right: '0.5rem'
});

export interface TrustedPublishersSectionProps {
    namespace: string;
    // base endpoint for this namespace's trusted-publishing requests
    trustedPublishingUrl: UrlString;
    // extensions selectable when registering
    extensionNames: string[];
    // filters the list and locks registrations to this extension
    extensionFilter?: string;
    providers: TrustedPublisherProvider[];
    // scope-specific first sentence of the section description; omit to render no description
    intro?: string;
    emptyText: string;
}

export const TrustedPublishersSection: FunctionComponent<TrustedPublishersSectionProps> = props => {
    const { handleError } = useContext(MainContext);
    const { data: allPublishers = [], isFetching } = useReportedQuery(useTrustedPublishers(props.trustedPublishingUrl));
    const { mutateAsync: deleteTrustedPublisher, isPending: deleting } = useDeleteTrustedPublisher();
    const [selectedProvider, setSelectedProvider] = useState('');
    const [dialogOpen, setDialogOpen] = useState(false);
    const [showAdd, setShowAdd] = useState(false);

    // the list endpoint is namespace-scoped, so extension pages filter client-side
    const publishers = props.extensionFilter
        ? allPublishers.filter(publisher => publisher.extension === props.extensionFilter)
        : allPublishers;
    const loading = isFetching || deleting;

    const remove = async (publisher: TrustedPublisher) => {
        try {
            await deleteTrustedPublisher({ trustedPublishingUrl: props.trustedPublishingUrl, id: publisher.id });
        } catch (err) {
            handleError(err);
        }
    };

    const openDialogWith = (providerId: string) => {
        setSelectedProvider(providerId);
        setDialogOpen(true);
    };

    // the empty state shows the add panel; otherwise it collapses behind a button
    const hasPublishers = publishers.length > 0;
    const addPanelVisible = !hasPublishers || showAdd;

    return (
        <>
            {props.intro ? (
                <Typography variant='body2' color='text.secondary' sx={{ mb: 2 }}>
                    {props.intro} The workflow authenticates with a short-lived OIDC token at publish time, accepted
                    only when its repository, workflow and (if set) environment match a registration below.{' '}
                    <TrustedPublishingDocsLink />
                </Typography>
            ) : null}
            <PublisherList
                publishers={publishers}
                providers={props.providers}
                loading={loading}
                rowDetail={props.extensionFilter ? 'none' : 'extension'}
                emptyText={props.emptyText}
                onDelete={remove}
            />
            {addPanelVisible ? (
                <AddPublisherPanel>
                    {hasPublishers && (
                        <CloseAddPanelButton
                            size='small'
                            onClick={() => setShowAdd(false)}
                            aria-label='Cancel adding a trusted publisher'>
                            <CloseIcon fontSize='small' />
                        </CloseAddPanelButton>
                    )}
                    <Typography variant='subtitle2' color='text.secondary'>
                        Add a trusted publisher
                    </Typography>
                    <Stack direction='row' spacing={2} useFlexGap sx={{ flexWrap: 'wrap', justifyContent: 'center' }}>
                        {props.providers.map(provider => (
                            <Button
                                key={provider.id}
                                variant='contained'
                                color='inherit'
                                disableElevation
                                startIcon={<TrustedPublisherProviderIcon providerId={provider.id} />}
                                onClick={() => openDialogWith(provider.id)}>
                                {provider.name}
                            </Button>
                        ))}
                    </Stack>
                </AddPublisherPanel>
            ) : (
                <Button variant='outlined' startIcon={<AddIcon />} onClick={() => setShowAdd(true)} sx={{ mt: 2 }}>
                    Add a trusted publisher
                </Button>
            )}
            <RegisterTrustedPublisherDialog
                open={dialogOpen}
                initialProvider={selectedProvider}
                onClose={() => setDialogOpen(false)}
                onRegistered={() => setShowAdd(false)}
                namespaces={[
                    {
                        name: props.namespace,
                        extensionNames: props.extensionNames,
                        trustedPublishingUrl: props.trustedPublishingUrl
                    }
                ]}
                namespace={props.namespace}
                lockedExtension={props.extensionFilter}
                providers={props.providers}
            />
        </>
    );
};

export const UserNamespaceTrustedPublishers: FunctionComponent<{ namespace: Namespace }> = ({ namespace }) => {
    const { user } = useContext(MainContext);
    const { trustedPublishingUrl } = namespace;
    // the providers query doubles as the feature probe: any failure hides the section
    const { data: providers = [] } = useTrustedPublisherProviders(trustedPublishingUrl);
    if (!user || !trustedPublishingUrl || providers.length === 0) {
        return null;
    }
    return (
        <Grid item>
            <Typography variant='h5' gutterBottom>
                Trusted Publishers
            </Typography>
            <TrustedPublishersSection
                namespace={namespace.name}
                trustedPublishingUrl={trustedPublishingUrl}
                extensionNames={Object.keys(namespace.extensions)}
                providers={providers}
                intro="Let a CI/CD workflow publish this namespace's extensions without a long-lived access token."
                emptyText='No trusted publishers registered yet.'
            />
        </Grid>
    );
};

export const ExtensionTrustedPublishers: FunctionComponent<{ namespace: string; extension: string }> = ({
    namespace,
    extension
}) => {
    const { user, service } = useContext(MainContext);
    const trustedPublishingUrl = service.userTrustedPublishingUrl(namespace);
    // the providers query doubles as the feature probe: any failure hides the section
    const { data: providers = [] } = useTrustedPublisherProviders(trustedPublishingUrl);
    if (!user || providers.length === 0) {
        return null;
    }
    return (
        <Box sx={{ mt: 2 }}>
            <Divider sx={{ mb: 2 }} />
            <Typography variant='h6' gutterBottom>
                Trusted Publishers
            </Typography>
            <TrustedPublishersSection
                namespace={namespace}
                trustedPublishingUrl={trustedPublishingUrl}
                extensionNames={[extension]}
                extensionFilter={extension}
                providers={providers}
                intro='Let a CI/CD workflow publish this extension without a long-lived access token.'
                emptyText='No trusted publishers registered for this extension yet.'
            />
        </Box>
    );
};
