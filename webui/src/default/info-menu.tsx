/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import * as React from 'react';
import { withStyles, createStyles } from '@material-ui/styles';
import { Theme, WithStyles, Menu, Typography, MenuItem, Link, IconButton } from '@material-ui/core';
import InfoIcon from '@material-ui/icons/Info';
import GitHubIcon from '@material-ui/icons/GitHub';
import MenuBookIcon from '@material-ui/icons/MenuBook';
import ForumIcon from '@material-ui/icons/Forum';

const infoMenuStyle = (theme: Theme) => createStyles({
    menuItem: {
        cursor: 'auto'
    },
    itemIcon: {
        marginRight: theme.spacing(1),
        width: '16px',
        height: '16px',
    },
    alignVertically: {
        display: 'flex',
        alignItems: 'center'
    }
});

class InfoMenuComponent extends React.Component<InfoMenuComponent.Props, InfoMenuComponent.State> {

    protected infoButton: HTMLElement | null;

    constructor(props: InfoMenuComponent.Props) {
        super(props);

        this.state = {
            open: false
        };
    }

    protected readonly handleInfoClick = () => {
        this.setState({ open: !this.state.open });
    };
    protected readonly handleClose = () => {
        this.setState({ open: false });
    };

    render(): React.ReactElement {
        const classes = this.props.classes;
        return <React.Fragment>
            <IconButton
                title='More Information'
                aria-label='More Information'
                onClick={this.handleInfoClick}
                ref={ref => this.infoButton = ref} >
                <InfoIcon />
            </IconButton>
            <Menu
                open={this.state.open}
                anchorEl={this.infoButton}
                transformOrigin={{ vertical: 'top', horizontal: 'right' }}
                onClose={this.handleClose} >
                <MenuItem className={classes.menuItem}>
                    <Link target='_blank' href='https://github.com/eclipse/openvsx'>
                        <Typography variant='body2' color='textPrimary' className={classes.alignVertically}>
                            <GitHubIcon className={classes.itemIcon} />
                            Repository
                        </Typography>
                    </Link>
                </MenuItem>
                <MenuItem className={classes.menuItem}>
                    <Link href='https://github.com/eclipse/openvsx/wiki'>
                        <Typography variant='body2' color='textPrimary' className={classes.alignVertically}>
                            <MenuBookIcon className={classes.itemIcon} />
                            Documentation
                        </Typography>
                    </Link>
                </MenuItem>
                <MenuItem className={classes.menuItem}>
                    <Link href='https://gitter.im/eclipse/openvsx'>
                        <Typography variant='body2' color='textPrimary' className={classes.alignVertically}>
                            <ForumIcon className={classes.itemIcon} />
                            Community Chat
                        </Typography>
                    </Link>
                </MenuItem>
            </Menu>
        </React.Fragment>;
    }
}

export namespace InfoMenuComponent {
    export interface Props extends WithStyles<typeof infoMenuStyle> {
    }

    export interface State {
        open: boolean;
    }
}

export const InfoMenu = withStyles(infoMenuStyle)(InfoMenuComponent);
