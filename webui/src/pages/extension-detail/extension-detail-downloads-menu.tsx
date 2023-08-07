/** ******************************************************************************
 * Copyright (c) 2022 Precies. Software and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */

import React, { FunctionComponent, MouseEvent, useState } from 'react';
import { Box, Button, Menu, MenuItem, Typography, Link } from '@mui/material';
import { getTargetPlatformDisplayName } from '../../utils';
import { UrlString } from '../..';

export const ExtensionDetailDownloadsMenu: FunctionComponent<ExtensionDetailDownloadsMenuProps> = props => {
    const [anchorEl, setAnchorEl] = useState<HTMLElement | null>(null);

    const handleClick = (event: MouseEvent<HTMLButtonElement>) => {
        setAnchorEl(event.currentTarget);
    };

    const handleClose = () => {
        setAnchorEl(null);
    };

    return <Box sx={{ display: 'flex', alignItems: 'center' }}>
        <Button
            variant='contained'
            color='secondary'
            sx={{ mt: 2 }}
            title='Download'
            aria-label='Download'
            onClick={handleClick}
        >
            Download
        </Button>
        <Menu
            open={Boolean(anchorEl)}
            anchorEl={anchorEl}
            transformOrigin={{ vertical: 'top', horizontal: 'left' }}
            sx={{ mt: 1 }}
            onClose={handleClose}>
                {
                    Object.keys(props.downloads).map((targetPlatform) => {
                        const downloadLink = props.downloads[targetPlatform];
                        return <MenuItem key={targetPlatform} sx={{ cursor: 'auto' }}>
                            <Link onClick={handleClose} href={downloadLink} sx={{ cursor: 'pointer', textDecoration: 'none' }}>
                                <Typography variant='body2' color='text.primary'>
                                    {getTargetPlatformDisplayName(targetPlatform)}
                                </Typography>
                            </Link>
                        </MenuItem>;
                    })
                }
        </Menu>
    </Box>;
};

export interface ExtensionDetailDownloadsMenuProps {
    downloads: {[targetPlatform: string]: UrlString};
}