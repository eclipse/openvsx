/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import React, { FunctionComponent, PropsWithChildren } from 'react';
import { Typography, MenuItem, Link, Button } from '@mui/material';
import { useLocation } from 'react-router-dom';
import { Link as RouteLink } from 'react-router-dom';
import GitHubIcon from '@mui/icons-material/GitHub';
import MenuBookIcon from '@mui/icons-material/MenuBook';
import ForumIcon from '@mui/icons-material/Forum';
import InfoIcon from '@mui/icons-material/Info';
import PublishIcon from '@mui/icons-material/Publish';
import { UserSettingsRoutes } from '../pages/user/user-settings';
import { styled, Theme } from '@mui/material/styles';

//-------------------- Mobile View --------------------//

const MobileMenuItem = styled(MenuItem)({
    cursor: 'auto',
    '&>a': {
        textDecoration: 'none'
    }
});

const itemIcon = {
    mr: 1,
    width: '16px',
    height: '16px',
};

const MobileMenuItemText: FunctionComponent<PropsWithChildren> = ({ children }) => {
    return (
        <Typography variant='body2' color='text.primary' sx={{ display: 'flex', alignItems: 'center' }}>
            {children}
        </Typography>
    );
};

export const MobileMenuContent: FunctionComponent = () => {

    const location = useLocation();

    return <>
        <MobileMenuItem>
            <Link target='_blank' href='https://github.com/eclipse/openvsx'>
                <MobileMenuItemText>
                    <GitHubIcon sx={itemIcon} />
                    Source Code
                </MobileMenuItemText>
            </Link>
        </MobileMenuItem>
        <MobileMenuItem>
            <Link href='https://github.com/eclipse/openvsx/wiki'>
                <MobileMenuItemText>
                    <MenuBookIcon sx={itemIcon} />
                    Documentation
                </MobileMenuItemText>
            </Link>
        </MobileMenuItem>
        <MobileMenuItem>
            <Link href='https://gitter.im/eclipse/openvsx'>
                <MobileMenuItemText>
                    <ForumIcon sx={itemIcon} />
                    Community Chat
                </MobileMenuItemText>
            </Link>
        </MobileMenuItem>
        <MobileMenuItem>
            <RouteLink to='/about'>
                <MobileMenuItemText>
                    <InfoIcon sx={itemIcon} />
                    About This Service
                </MobileMenuItemText>
            </RouteLink>
        </MobileMenuItem>
        {
            !location.pathname.startsWith(UserSettingsRoutes.ROOT)
            ? <MobileMenuItem>
                <RouteLink to='/user-settings/extensions'>
                    <MobileMenuItemText>
                        <PublishIcon sx={itemIcon} />
                        Publish Extension
                    </MobileMenuItemText>
                </RouteLink>
            </MobileMenuItem>
            : null
        }
    </>;
};

//-------------------- Default View --------------------//

const headerItem = ({ theme }: { theme: Theme }) => ({
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
});

const MenuLink = styled(Link)(headerItem);
const MenuRouteLink = styled(RouteLink)(headerItem);

export const DefaultMenuContent: FunctionComponent = () => {
    return <>
        <MenuLink href='https://github.com/eclipse/openvsx/wiki'>
            Documentation
        </MenuLink>
        <MenuLink href='https://gitter.im/eclipse/openvsx'>
            Community
        </MenuLink>
        <MenuRouteLink to='/about'>
            About
        </MenuRouteLink>
        <Button variant='contained' color='secondary' href='/user-settings/extensions' sx={{ mx: 2.5 }}>
            Publish
        </Button>
    </>;
};
