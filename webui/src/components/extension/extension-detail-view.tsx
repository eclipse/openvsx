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
import { Link as RouteLink } from 'react-router';
import { Extension, VERSION_ALIASES, VersionTargetPlatforms } from '../../extension-registry-types';
import { ExtensionHeader } from './extension-header';
import { ExtensionStatusChips } from './extension-status-chips';
import { ExtensionVersionTable } from './extension-version-table';
import { DeleteVersionDialog, VersionDeleteTarget } from './extension-version-delete-dialog';
import { DeleteAllVersionsDialog } from './extension-delete-all-versions-dialog';
import { ExtensionTrustedPublishers } from '../../pages/user/trusted-publishing/trusted-publishers-section';
import { ExtensionDetailRoutes } from '../../pages/extension-detail/extension-detail-routes';
import { createRoute } from '../../utils';

export const ExtensionDetailView: FunctionComponent<ExtensionDetailViewProps> = props => {
    const { extension, actions, onRemoveVersion, onVersionDeleted, onPurgeVersion } = props;
    const canPurge = !!onPurgeVersion;

    const [page, setPage] = useState(0);
    const [deleteDialogVersion, setDeleteDialogVersion] = useState<VersionTargetPlatforms | null>(null);
    const [purgeDialogVersion, setPurgeDialogVersion] = useState<VersionTargetPlatforms | null>(null);
    const [deleteAllOpen, setDeleteAllOpen] = useState(false);
    const [purgeAllOpen, setPurgeAllOpen] = useState(false);

    useEffect(() => {
        setPage(0);
    }, [extension]);

    const publicRoute = createRoute([ExtensionDetailRoutes.ROOT, extension.namespace, extension.name]);
    const allVersions = (extension.allTargetPlatformVersions ?? []).filter(v => !VERSION_ALIASES.includes(v.version));
    // Versions the current user is allowed to delete. canDelete is only populated in the user settings
    // view (undefined means unrestricted, e.g. admin/purge); a non-owner member may only delete the
    // versions they published themselves. "Delete All Versions" therefore operates on this subset.
    const deletableVersions = canPurge ? allVersions : allVersions.filter(v => v.canDelete !== false);
    // A version can still be (soft-)deleted while it has at least one target platform that is not removed.
    const hasDeletableVersions = deletableVersions.some(v => v.targetPlatforms.some(tp => !tp.removed));

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
                {extension.active && (
                    <Button variant='outlined' component={RouteLink} to={publicRoute}>
                        View in Marketplace
                    </Button>
                )}
                <Button
                    variant='outlined'
                    color='error'
                    onClick={() => setDeleteAllOpen(true)}
                    disabled={!hasDeletableVersions}>
                    Delete All Versions
                </Button>
                {canPurge && (
                    <Button
                        variant='contained'
                        color='error'
                        onClick={() => setPurgeAllOpen(true)}
                        disabled={allVersions.length === 0}>
                        Purge All Versions
                    </Button>
                )}
                {actions}
            </Stack>
            <ExtensionTrustedPublishers namespace={extension.namespace} extension={extension.name} />
            <Typography variant='h6' gutterBottom sx={{ mt: 4 }}>
                Versions
            </Typography>
            <ExtensionVersionTable
                versions={allVersions}
                page={page}
                onPageChange={setPage}
                onDeleteVersion={setDeleteDialogVersion}
                onPurgeVersion={canPurge ? setPurgeDialogVersion : undefined}
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
            {purgeDialogVersion && onPurgeVersion && (
                <DeleteVersionDialog
                    open={true}
                    mode='purge'
                    onClose={() => setPurgeDialogVersion(null)}
                    extension={extension}
                    version={purgeDialogVersion}
                    onRemove={onPurgeVersion}
                    onDeleted={onVersionDeleted}
                />
            )}
            {deleteAllOpen && (
                <DeleteAllVersionsDialog
                    open={true}
                    onClose={() => setDeleteAllOpen(false)}
                    extension={extension}
                    versions={deletableVersions}
                    onRemove={onRemoveVersion}
                    onDeleted={onVersionDeleted}
                />
            )}
            {purgeAllOpen && onPurgeVersion && (
                <DeleteAllVersionsDialog
                    open={true}
                    mode='purge'
                    onClose={() => setPurgeAllOpen(false)}
                    extension={extension}
                    versions={allVersions}
                    onRemove={onPurgeVersion}
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
    // When provided (admin only), enables the permanent-purge affordances.
    onPurgeVersion?: (targets: VersionDeleteTarget[]) => Promise<unknown>;
}
