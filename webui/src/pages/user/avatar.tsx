/********************************************************************************
 * Copyright (c) 2019 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import React, { FunctionComponent, useContext, useEffect, useRef, useState } from 'react';
import { styled } from '@mui/material/styles';
import { Avatar, Button, Menu, Typography, MenuItem, Link, Divider, IconButton } from '@mui/material';
import { Link as RouteLink } from 'react-router-dom';
import { isError, CsrfTokenJson } from '../../extension-registry-types';
import { UserSettingsRoutes } from './user-settings';
import { AdminDashboardRoutes } from '../admin-dashboard/admin-dashboard';
import { MainContext } from '../../context';

const link = {
    cursor: 'pointer',
    textDecoration: 'none'
};

const AvatarRouteLink = styled(RouteLink)(link);
const AvatarMenuItem = styled(MenuItem)({ cursor: 'auto' });
const LogoutButton = styled(Button)({
    ...link,
    border: 'none',
    background: 'none',
    padding: 0
});

export const UserAvatar: FunctionComponent = () => {
    const [open, setOpen] = useState<boolean>(false);
    const [csrf, setCsrf] = useState<string>();
    const context = useContext(MainContext);
    const avatarButton = useRef<any>();

    const abortController = new AbortController();
    useEffect(() => {
        updateCsrf();
        return () => abortController.abort();
    }, []);

    const updateCsrf = async () => {
        try {
            const csrfResponse = await context.service.getCsrfToken(abortController);
            if (!isError(csrfResponse)) {
                const csrfToken = csrfResponse as CsrfTokenJson;
                setCsrf(csrfToken.value);
            }
        } catch (err) {
            context.handleError(err);
        }
    };

    const handleAvatarClick = () => {
        setOpen(!open);
    };

    const handleClose = () => {
        setOpen(false);
    };

    const user = context.user;
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
                <form method='post' action={context.service.getLogoutUrl()}>
                    {csrf ? <input name='_csrf' type='hidden' value={csrf} /> : null}
                    <LogoutButton type='submit'>
                        <Typography variant='button' sx={{ color: 'primary.dark' }}>
                            Log Out
                        </Typography>
                    </LogoutButton>
                </form>
            </AvatarMenuItem>
        </Menu>
    </>;
};