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
import { Theme, WithStyles, Typography, MenuItem, Link, Button } from '@material-ui/core';
import { RouteComponentProps, withRouter } from 'react-router-dom';
import { Link as RouteLink } from 'react-router-dom';
import GitHubIcon from '@material-ui/icons/GitHub';
import MenuBookIcon from '@material-ui/icons/MenuBook';
import ForumIcon from '@material-ui/icons/Forum';
import InfoIcon from '@material-ui/icons/Info';
import PublishIcon from '@material-ui/icons/Publish';
import { UserSettingsRoutes } from '../pages/user/user-settings';

const menuContentStyle = (theme: Theme) => createStyles({
    headerItem: {
        margin: theme.spacing(2.5),
        color: theme.palette.text.primary,
        textDecoration: 'none',
        fontSize: '1.1rem',
        fontFamily: theme.typography.fontFamily,
        fontWeight: theme.typography.fontWeightLight,
        letterSpacing: 1,
        '&:hover': {
            color: theme.palette.secondary.main,
            textDecoration: 'none'
        }
    },
    publishButton: {
        marginLeft: theme.spacing(2.5),
        marginRight: theme.spacing(2.5)
    },
    menuItem: {
        cursor: 'auto',
        '&>a': {
            textDecoration: 'none'
        }
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


//-------------------- Mobile View --------------------//

export class MobileMenuContentComponent extends React.Component<MobileMenuContent.Props> {
    render(): React.ReactElement {
        const classes = this.props.classes;
        return <React.Fragment>
            <MenuItem className={classes.menuItem}>
                <Link target='_blank' href='https://github.com/eclipse/openvsx'>
                    <Typography variant='body2' color='textPrimary' className={classes.alignVertically}>
                        <GitHubIcon className={classes.itemIcon} />
                        Source Code
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
            <MenuItem className={classes.menuItem}>
                <RouteLink to='/about'>
                    <Typography variant='body2' color='textPrimary' className={classes.alignVertically}>
                        <InfoIcon className={classes.itemIcon} />
                        About This Service
                    </Typography>
                </RouteLink>
            </MenuItem>
            {
                !this.props.location.pathname.startsWith(UserSettingsRoutes.ROOT)
                ? <MenuItem className={classes.menuItem}>
                    <RouteLink to='/user-settings/extensions'>
                        <Typography variant='body2' color='textPrimary' className={classes.alignVertically}>
                            <PublishIcon className={classes.itemIcon} />
                            Publish Extension
                        </Typography>
                    </RouteLink>
                </MenuItem>
                : null
            }
        </React.Fragment>;
    }
}

export namespace MobileMenuContent {
    export interface Props extends WithStyles<typeof menuContentStyle>, RouteComponentProps {

    }
}

export const MobileMenuContent = withStyles(menuContentStyle)(withRouter(MobileMenuContentComponent));


//-------------------- Default View --------------------//

export class DefaultMenuContentComponent extends React.Component<DefaultMenuContent.Props> {
    render(): React.ReactElement {
        const classes = this.props.classes;
        return <React.Fragment>
            <Link href='https://github.com/eclipse/openvsx/wiki' className={classes.headerItem}>
                Documentation
            </Link>
            <Link href='https://gitter.im/eclipse/openvsx' className={classes.headerItem}>
                Community
            </Link>
            <RouteLink to='/about' className={classes.headerItem}>
                About
            </RouteLink>
            <Button variant='contained' color='secondary' href='/user-settings/extensions' className={classes.publishButton}>
                Publish
            </Button>
        </React.Fragment>;
    }
}

export namespace DefaultMenuContent {
    export interface Props extends WithStyles<typeof menuContentStyle>, RouteComponentProps {

    }
}

export const DefaultMenuContent = withStyles(menuContentStyle)(withRouter(DefaultMenuContentComponent));
