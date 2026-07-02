/********************************************************************************
 * Copyright (c) 2019 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import { FunctionComponent, useContext, useRef, useState } from 'react';
import { Avatar, Box, IconButton, Link, Menu, MenuItem, Typography } from '@mui/material';
import { Link as RouteLink } from 'react-router-dom';
import PersonIcon from '@mui/icons-material/Person';
import SettingsIcon from '@mui/icons-material/Settings';
import AdminPanelSettingsIcon from '@mui/icons-material/AdminPanelSettings';
import LogoutIcon from '@mui/icons-material/Logout';
import { UserSettingsRoutes } from './user-settings-routes';
import { AdminDashboardRoutes } from '../admin-dashboard/admin-dashboard-routes';
import { MainContext } from '../../context';
import { LogoutForm } from './logout';

const menuItemSx = {
    borderRadius: '9px',
    fontSize: '14px',
    fontWeight: 500,
    py: '8px',
    px: '10px',
    gap: '10px',
    color: 'text.primary',
    minHeight: '36px',
    display: 'flex',
    alignItems: 'center'
} as const;

const iconSx = { fontSize: 17, color: 'text.disabled', flexShrink: 0 };

export const UserAvatar: FunctionComponent = () => {
    const [open, setOpen] = useState(false);
    const context = useContext(MainContext);
    const anchorRef = useRef<HTMLButtonElement>(null);
    const logoutFormRef = useRef<HTMLFormElement>(null);

    const user = context.user;
    if (!user) return null;

    const initials = user.loginName.slice(0, 2).toUpperCase();

    return (
        <>
            <IconButton
                ref={anchorRef}
                title={`Logged in as ${user.loginName}`}
                aria-label='User menu'
                onClick={() => setOpen(true)}
                sx={{ p: '5px' }}>
                <Avatar
                    src={user.avatarUrl}
                    alt={user.loginName}
                    sx={{
                        width: 32,
                        height: 32,
                        bgcolor: 'accentSoft',
                        color: 'secondary.light',
                        fontSize: '12px',
                        fontWeight: 700,
                        borderRadius: '8px'
                    }}>
                    {initials}
                </Avatar>
            </IconButton>
            <Menu
                open={open}
                anchorEl={anchorRef.current}
                anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
                transformOrigin={{ vertical: 'top', horizontal: 'right' }}
                onClose={() => setOpen(false)}>
                {/* User header */}
                <Box sx={{ px: '14px', py: '14px', display: 'flex', alignItems: 'center', gap: '12px' }}>
                    <Avatar
                        src={user.avatarUrl}
                        sx={{
                            width: 40,
                            height: 40,
                            bgcolor: 'accentSoft',
                            color: 'secondary.light',
                            fontSize: '15px',
                            fontWeight: 700,
                            borderRadius: '10px',
                            flexShrink: 0
                        }}>
                        {initials}
                    </Avatar>
                    <Box sx={{ minWidth: 0 }}>
                        <Typography
                            sx={{
                                fontSize: '11px',
                                fontWeight: 700,
                                textTransform: 'uppercase',
                                letterSpacing: '0.07em',
                                color: 'text.disabled',
                                lineHeight: 1.3
                            }}>
                            Logged in as
                        </Typography>
                        <Typography
                            sx={{
                                fontSize: '14.5px',
                                fontWeight: 700,
                                lineHeight: 1.3,
                                color: 'text.primary',
                                overflow: 'hidden',
                                textOverflow: 'ellipsis',
                                whiteSpace: 'nowrap'
                            }}>
                            {user.loginName}
                        </Typography>
                    </Box>
                </Box>
                <MenuItem
                    component={Link}
                    href={user.homepage}
                    target='_blank'
                    onClick={() => setOpen(false)}
                    sx={{ ...menuItemSx, textDecoration: 'none' }}>
                    <PersonIcon sx={iconSx} />
                    Your profile
                </MenuItem>
                <MenuItem
                    component={RouteLink}
                    to={UserSettingsRoutes.PROFILE}
                    onClick={() => setOpen(false)}
                    sx={{ ...menuItemSx, textDecoration: 'none' }}>
                    <SettingsIcon sx={iconSx} />
                    Settings
                </MenuItem>
                {user.role === 'admin' && (
                    <MenuItem
                        component={RouteLink}
                        to={AdminDashboardRoutes.MAIN}
                        onClick={() => setOpen(false)}
                        sx={{ ...menuItemSx, textDecoration: 'none' }}>
                        <AdminPanelSettingsIcon sx={iconSx} />
                        Admin Dashboard
                    </MenuItem>
                )}
                <MenuItem onClick={() => logoutFormRef.current?.submit()} sx={menuItemSx}>
                    <LogoutForm ref={logoutFormRef}>
                        <LogoutIcon sx={iconSx} />
                        Log out
                    </LogoutForm>
                </MenuItem>
            </Menu>
        </>
    );
};
