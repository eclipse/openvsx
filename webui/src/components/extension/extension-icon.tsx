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

import { FunctionComponent, useContext } from 'react';
import { Box, SxProps, Theme } from '@mui/material';
import { MainContext } from '../../context';
import { Extension, SearchEntry } from '../../extension-registry-types';
import { useExtensionIcon } from './use-extension-icon';

/**
 * Renders an extension's icon, falling back to the configured default icon
 * while the real one loads or when the extension has none.
 */
export const ExtensionIcon: FunctionComponent<ExtensionIconProps> = ({ extension, alt, sx }) => {
    const { pageSettings } = useContext(MainContext);
    const icon = useExtensionIcon(extension);

    return (
        <Box
            component='img'
            src={icon ?? pageSettings.urls.extensionDefaultIcon}
            alt={alt ?? extension.displayName ?? extension.name}
            sx={sx}
        />
    );
};

export interface ExtensionIconProps {
    extension: Extension | SearchEntry;
    alt?: string;
    sx?: SxProps<Theme>;
}
