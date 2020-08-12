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
import { Theme, WithStyles, Typography, MenuItem, Link } from '@material-ui/core';
import GitHubIcon from '@material-ui/icons/GitHub';
import MenuBookIcon from '@material-ui/icons/MenuBook';
import ForumIcon from '@material-ui/icons/Forum';

//-------------------- Mobile View --------------------//

const menuContentStyle = (theme: Theme) => createStyles({
    headerItem: {
        margin: theme.spacing(2)
    },
    headerLink: {
        '&:hover': {
            color: theme.palette.secondary.main
        }
    },
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

export class MobileMenuContentComponent extends React.Component<WithStyles<typeof menuContentStyle>> {
    render(): React.ReactElement {
        const classes = this.props.classes;
        return <React.Fragment>
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
        </React.Fragment>;
    }
}

export const MobileMenuContent = withStyles(menuContentStyle)(MobileMenuContentComponent);


//-------------------- Default View --------------------//

export class DefaultMenuConentComponent extends React.Component<WithStyles<typeof menuContentStyle>> {
    render(): React.ReactElement {
        const classes = this.props.classes;
        return <React.Fragment>
            <Link href='https://github.com/eclipse/openvsx/wiki' className={classes.headerItem}>
                <Typography variant='body1' color='textPrimary' className={classes.headerLink}>
                    Documentation
                </Typography>
            </Link>
            <Link href='https://gitter.im/eclipse/openvsx' className={classes.headerItem}>
                <Typography variant='body1' color='textPrimary' className={classes.headerLink}>
                    Community Chat
                </Typography>
            </Link>
        </React.Fragment>;
    }
}

export const DefaultMenuContent = withStyles(menuContentStyle)(DefaultMenuConentComponent);
