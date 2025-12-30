/********************************************************************************
 * Copyright (c) 2019 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import React, { FunctionComponent, useRef, useState } from 'react';
import { styled } from '@mui/material/styles';
import { Avatar, Menu, Typography, MenuItem, Link, Divider, IconButton } from '@mui/material';
import { Link as RouteLink } from 'react-router-dom';
import { UserSettingsRoutes } from './user-settings';
import { AdminDashboardRoutes } from '../admin-dashboard/admin-dashboard';
import { LogoutForm } from './logout';
import { useGetUserQuery } from '../../store/api';

const AvatarRouteLink = styled(RouteLink)({
    cursor: 'pointer',
    textDecoration: 'none'
});

const AvatarMenuItem = styled(MenuItem)({ cursor: 'auto' });


export const UserAvatar: FunctionComponent = () => {
    const [open, setOpen] = useState<boolean>(false);
    const avatarButton = useRef<any>();
    const { data: user } = useGetUserQuery();

    const handleAvatarClick = () => {
        setOpen(!open);
    };

    const handleClose = () => {
        setOpen(false);
    };

    if (!user) {
        return null;
    }
    return <>
        <IconButton
            title={`Logged in as ${user.loginName}`}
            aria-label='User Info'
            onClick={handleAvatarClick}
            ref={(ref: any) => avatarButton.current = ref} >
            <Avatar
                src={user.avatarUrl}
                alt={user.loginName}
                variant='rounded'
                sx={{ width: '30px', height: '30px' }} />
        </IconButton>
        <Menu
            open={open}
            anchorEl={avatarButton.current}
            anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
            transformOrigin={{ vertical: 'top', horizontal: 'right' }}
            onClose={handleClose} >
            <AvatarMenuItem>
                <Link href={user.homepage} underline='hover'>
                    <Typography variant='body2' color='text.primary'>
                        Logged in as
                    </Typography>
                    <Typography variant='overline' color='text.primary'>
                        {user.loginName}
                    </Typography>
                </Link>
            </AvatarMenuItem>
            <Divider />
            <AvatarMenuItem>
                <AvatarRouteLink onClick={handleClose} to={UserSettingsRoutes.PROFILE}>
                    <Typography variant='button' color='text.primary'>
                        Settings
                    </Typography>
                </AvatarRouteLink>
            </AvatarMenuItem>
            {
                user.role && user.role === 'admin' ?
                    <AvatarMenuItem>
                        <AvatarRouteLink onClick={handleClose} to={AdminDashboardRoutes.MAIN}>
                            <Typography variant='button' color='text.primary'>
                                Admin Dashboard
                            </Typography>
                        </AvatarRouteLink>
                    </AvatarMenuItem>
                    :
                    ''
            }
            <AvatarMenuItem>
                <LogoutForm>
                    <Typography variant='button' sx={{ color: 'primary.dark' }}>
                        Log Out
                    </Typography>
                </LogoutForm>
            </AvatarMenuItem>
        </Menu>
    </>;
};