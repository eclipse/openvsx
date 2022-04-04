/** ******************************************************************************
 * Copyright (c) 2022 Precies. Software and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */

import * as React from 'react';
import { Box, Button, Menu, MenuItem, Theme, Typography, Link } from '@material-ui/core';
import { RouteComponentProps, withRouter } from 'react-router-dom';
import { WithStyles, withStyles, createStyles } from '@material-ui/styles';
import { getTargetPlatformDisplayName } from '../../utils';
import { UrlString } from '../..';

const downloadsMenuStyles = (theme: Theme) => createStyles({
    link: {
        cursor: 'pointer',
        textDecoration: 'none'
    },
    menuItem: {
        cursor: 'auto'
    },
    downloadButton: {
        marginTop: theme.spacing(2)
    },
    alignVertically: {
        display: 'flex',
        alignItems: 'center'
    }
});

class ExtensionDetailDownloadsMenuComponent extends React.Component<ExtensionDetailDownloadsMenu.Props, ExtensionDetailDownloadsMenu.State> {

    constructor(props: ExtensionDetailDownloadsMenu.Props) {
        super(props);
        this.state = { anchorEl: null };
    }

    render(): React.ReactElement {
        const open = Boolean(this.state.anchorEl);
        const handleClick = (event: React.MouseEvent<HTMLButtonElement>) => {
            this.setState({ anchorEl: event.currentTarget });
        };
        const handleClose = () => {
            this.setState({ anchorEl: null });
        };

        return <Box className={this.props.classes.alignVertically}>
            <Button
            variant='contained' color='secondary'
            className={this.props.classes.downloadButton}
            title='Download'
            aria-label='Download'
            onClick={handleClick}>
                Download
            </Button>
            <Menu
                open={open}
                anchorEl={this.state.anchorEl}
                transformOrigin={{ vertical: -50, horizontal: 'left' }}
                onClose={handleClose}>
                    {
                        Object.keys(this.props.downloads).map((targetPlatform) => {
                            const downloadLink = this.props.downloads[targetPlatform];
                            return <MenuItem key={targetPlatform} className={this.props.classes.menuItem}>
                                <Link onClick={handleClose} href={downloadLink} className={this.props.classes.link}>
                                    <Typography variant='body2' color='textPrimary'>
                                        {getTargetPlatformDisplayName(targetPlatform)}
                                    </Typography>
                                </Link>
                            </MenuItem>;
                        })
                    }
            </Menu>
        </Box>;
    }
}

export namespace ExtensionDetailDownloadsMenu {
    export interface Props extends WithStyles<typeof downloadsMenuStyles>, RouteComponentProps {
        downloads: {[targetPlatform: string]: UrlString};
    }
    export interface State {
        anchorEl: HTMLElement | null;
    }
}

export const ExtensionDetailDownloadsMenu = withStyles(downloadsMenuStyles)(withRouter(ExtensionDetailDownloadsMenuComponent));