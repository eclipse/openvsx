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

import { FunctionComponent, ReactNode, useEffect, useState } from 'react';
import { Box, Button, Divider, Stack, Typography } from '@mui/material';
import { Link as RouteLink } from 'react-router-dom';
import { Extension, VERSION_ALIASES, VersionTargetPlatforms } from '../../extension-registry-types';
import { ExtensionHeader } from './extension-header';
import { ExtensionStatusChips } from './extension-status-chips';
import { ExtensionVersionTable } from './extension-version-table';
import { DeleteVersionDialog, VersionDeleteTarget } from './extension-version-delete-dialog';
import { DeleteAllVersionsDialog } from './extension-delete-all-versions-dialog';
import { ExtensionDetailRoutes } from '../../pages/extension-detail/extension-detail-routes';
import { createRoute } from '../../utils';

export const ExtensionDetailView: FunctionComponent<ExtensionDetailViewProps> = props => {
    const { extension, actions, onRemoveVersion, onVersionDeleted } = props;

    const [page, setPage] = useState(0);
    const [deleteDialogVersion, setDeleteDialogVersion] = useState<VersionTargetPlatforms | null>(null);
    const [deleteAllOpen, setDeleteAllOpen] = useState(false);

    useEffect(() => {
        setPage(0);
    }, [extension]);

    const publicRoute = createRoute([ExtensionDetailRoutes.ROOT, extension.namespace, extension.name]);
    const allVersions = (extension.allTargetPlatformVersions ?? []).filter(v => !VERSION_ALIASES.includes(v.version));

    return (
        <Box>
            <ExtensionHeader extension={extension} />
            {extension.description && (
                <Typography variant='body1' mb={2}>
                    {extension.description}
                </Typography>
            )}
            <ExtensionStatusChips extension={extension} />
            <Divider sx={{ my: 2 }} />
            <Stack direction='row' spacing={2} mb={3}>
                <Button variant='outlined' component={RouteLink} to={publicRoute}>
                    View in Marketplace
                </Button>
                <Button
                    variant='outlined'
                    color='error'
                    onClick={() => setDeleteAllOpen(true)}
                    disabled={allVersions.length === 0}>
                    Delete All Versions
                </Button>
                {actions}
            </Stack>
            <Typography variant='h6' gutterBottom>
                Versions
            </Typography>
            <ExtensionVersionTable
                versions={allVersions}
                page={page}
                onPageChange={setPage}
                onDeleteVersion={setDeleteDialogVersion}
            />
            {deleteDialogVersion && (
                <DeleteVersionDialog
                    open={true}
                    onClose={() => setDeleteDialogVersion(null)}
                    extension={extension}
                    version={deleteDialogVersion}
                    onRemove={onRemoveVersion}
                    onDeleted={onVersionDeleted}
                />
            )}
            {deleteAllOpen && (
                <DeleteAllVersionsDialog
                    open={true}
                    onClose={() => setDeleteAllOpen(false)}
                    extension={extension}
                    versions={allVersions}
                    onRemove={onRemoveVersion}
                    onDeleted={onVersionDeleted}
                />
            )}
        </Box>
    );
};

export interface ExtensionDetailViewProps {
    extension: Extension;
    actions?: ReactNode;
    onRemoveVersion: (targets: VersionDeleteTarget[]) => Promise<unknown>;
    onVersionDeleted: () => void;
}
