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

import { FunctionComponent, ReactNode, useContext } from 'react';
import { Alert, AlertTitle, Box, Link } from '@mui/material';
import type { SxProps, Theme } from '@mui/material/styles';
import ArrowForwardIcon from '@mui/icons-material/ArrowForward';
import { MainContext } from '../../context';
import { Extension } from '../../extension-registry-types';

/**
 * The warning shown wherever an unverified namespace is holding something back — the extension
 * settings page and the namespace settings page share it, so both read the same.
 *
 * The action is the deployment's `elements.claimNamespace` (typically an issue template), falling
 * back to the namespace-access docs. It is a link rather than a button: the theme already styles
 * links inside an alert, so it sits in the notice instead of competing with it.
 */
export const NamespaceClaimNotice: FunctionComponent<NamespaceClaimNoticeProps> = ({
    namespace,
    extension,
    showAction = false,
    sx,
    children
}) => {
    const { pageSettings } = useContext(MainContext);
    const ClaimNamespace = pageSettings.elements?.claimNamespace;

    return (
        <Alert severity='warning' sx={sx}>
            <AlertTitle sx={{ fontWeight: 700 }}>Namespace not verified</AlertTitle>
            {children}
            {showAction ? (
                <Box sx={{ mt: '0.75rem' }}>
                    {ClaimNamespace ? (
                        <ClaimNamespace namespace={namespace} extension={extension} />
                    ) : (
                        // Nothing configured for this deployment: point at the generic docs.
                        <Link href={pageSettings.urls.namespaceAccessInfo} target='_blank' rel='noopener'>
                            Claim ownership
                            <ArrowForwardIcon sx={{ fontSize: '1.125rem' }} />
                        </Link>
                    )}
                </Box>
            ) : null}
        </Alert>
    );
};

export interface NamespaceClaimNoticeProps {
    namespace: string;
    /** Only the extension surface has one; forwarded to the deployment's claim element. */
    extension?: Extension;
    /** Claiming is the publisher's action, so a surface opts in; admin views show the explanation alone. */
    showAction?: boolean;
    sx?: SxProps<Theme>;
    /** What being unverified means on this surface. */
    children: ReactNode;
}
