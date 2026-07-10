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
import { Box, Skeleton, SxProps, Theme } from '@mui/material';
import { MainContext } from '../../context';
import { Extension, SearchEntry } from '../../extension-registry-types';
import { useExtensionIcon } from './use-extension-icon';

/** Renders an extension's icon: a skeleton while loading, then the icon or the configured default. */
export const ExtensionIcon: FunctionComponent<ExtensionIconProps> = ({ extension, alt, sx }) => {
    const { pageSettings } = useContext(MainContext);
    const { data: icon, isLoading } = useExtensionIcon(extension);

    if (isLoading) {
        // Reset the Skeleton's default height so `aspectRatio` squares it from
        // the width; an explicit height in `sx` still wins.
        return (
            <Skeleton
                variant='rounded'
                sx={[{ height: 'auto', aspectRatio: '1 / 1' }, ...(Array.isArray(sx) ? sx : [sx])]}
            />
        );
    }

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
