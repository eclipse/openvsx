/********************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import { FunctionComponent, ReactNode } from 'react';
import { Typography } from '@mui/material';
import SettingsOutlinedIcon from '@mui/icons-material/SettingsOutlined';
import WarningAmberIcon from '@mui/icons-material/WarningAmber';
import { Extension } from '../../extension-registry-types';
import { ExtensionCard } from '../extension-card';
import { getExtensionStatus } from './extension-status';
import { createRoute } from '../../utils';
import { ExtensionSettingsBackState } from '../../pages/user/extensions/extension-settings';

/**
 * The public {@link ExtensionCard} pointed at a management page instead of the marketplace: same
 * card, plus a gear affordance and — where the extension is not simply public — its publishing
 * state in place of the rating.
 */
export const ManageExtensionCard: FunctionComponent<ManageExtensionCardProps> = ({
    extension,
    routePrefix,
    linkState,
    iconPending,
    footerStart
}) => {
    const status = getExtensionStatus(extension);
    // The namespace is the user's to claim, so the card flags it rather than just looking switched off.
    const needsAttention = Boolean(extension.namespaceOwnershipConflict);
    return (
        <ExtensionCard
            extension={extension}
            to={createRoute([routePrefix, extension.namespace, extension.name])}
            linkState={linkState}
            // Greyscale would swallow the warning colour, and this state is the user's to fix —
            // it reads as actionable rather than switched off.
            dimmed={extension.active === false && !extension.namespaceOwnershipConflict}
            iconPending={iconPending}
            tone={needsAttention ? 'warning' : undefined}
            overlay={
                needsAttention ? (
                    <WarningAmberIcon titleAccess='Needs attention' sx={{ fontSize: '0.9375rem' }} />
                ) : (
                    <SettingsOutlinedIcon sx={{ fontSize: '0.9375rem' }} />
                )
            }
            footerStart={
                footerStart ??
                (status ? (
                    <Typography component='span' sx={{ fontSize: '0.75rem', fontWeight: 600, color: status.color }}>
                        {status.label}
                    </Typography>
                ) : undefined)
            }
        />
    );
};

export interface ManageExtensionCardProps {
    extension: Extension;
    /** Base route the card links to, e.g. the user settings or the admin extension route. */
    routePrefix: string;
    /** Router state passed to the extension settings page, e.g. its back-navigation target. */
    linkState?: ExtensionSettingsBackState;
    /** The icon is still being produced (e.g. a just-published package), so keep its skeleton. */
    iconPending?: boolean;
    /** Replaces the publishing-state footer, e.g. while a package is still being reviewed. */
    footerStart?: ReactNode;
}
