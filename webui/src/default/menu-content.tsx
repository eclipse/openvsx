/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import { FunctionComponent, PropsWithChildren, useContext, useRef, useState } from 'react';
import { Typography, MenuItem, Link, Button, IconButton, Avatar, Menu } from '@mui/material';
import { useLocation, useNavigate, Link as RouteLink } from 'react-router-dom';
import MoreVertIcon from '@mui/icons-material/MoreVert';
import GitHubIcon from '@mui/icons-material/GitHub';
import MenuBookIcon from '@mui/icons-material/MenuBook';
import InfoIcon from '@mui/icons-material/Info';
import PublishIcon from '@mui/icons-material/Publish';
import AccountBoxIcon from '@mui/icons-material/AccountBox';
import { UserAvatar } from '../pages/user/avatar';
import { UserSettingsRoutes } from '../pages/user/user-settings-routes';
import { alpha, styled, Theme } from '@mui/material/styles';
import { MainContext } from '../context';
import { KbdKey } from '../components/kbd-key';
import { useShortcut } from '../hooks/use-shortcut';
import { focusOutline } from '../components/page-primitives';
import SettingsIcon from '@mui/icons-material/Settings';
import AdminPanelSettingsIcon from '@mui/icons-material/AdminPanelSettings';
import LogoutIcon from '@mui/icons-material/Logout';
import { AdminDashboardRoutes } from '../pages/admin-dashboard/admin-dashboard-routes';
import { LogoutForm } from '../pages/user/logout';
import { LoginComponent } from './login';

// Shared destinations so a menu item's link, its keyboard shortcut, and its hint
// can't drift apart. External docs open in the same tab, matching the link click.
const DOCS_URL = 'https://github.com/eclipse/openvsx/wiki';

//-------------------- Mobile View --------------------//
// eslint-disable-next-line react-refresh/only-export-components
export const itemIcon = {
    mr: 1,
    width: '16px',
    height: '16px'
};

export const MenuItemText: FunctionComponent<PropsWithChildren> = ({ children }) => {
    return (
        <Typography
            variant='body2'
            color='text.primary'
            sx={{ display: 'flex', alignItems: 'center', textTransform: 'none' }}>
            {children}
        </Typography>
    );
};

export const MobileUserAvatar: FunctionComponent = () => {
    const { user } = useContext(MainContext);
    const logoutFormRef = useRef<HTMLFormElement>(null);
    const anchorRef = useRef<HTMLLIElement | null>(null);
    const [open, setOpen] = useState(false);
    const close = () => setOpen(false);
    if (!user) {
        return null;
    }

    return (
        <>
            <MenuItem ref={anchorRef} onClick={() => setOpen(true)} aria-haspopup='menu' aria-expanded={open}>
                <MenuItemText>
                    <Avatar src={user.avatarUrl} alt={user.loginName} variant='rounded' sx={itemIcon} />
                    {user.loginName}
                </MenuItemText>
                <MoreVertIcon fontSize='small' sx={{ ml: 'auto', color: 'text.secondary' }} />
            </MenuItem>
            <Menu
                open={open}
                onClose={close}
                anchorEl={anchorRef.current}
                anchorOrigin={{ vertical: 'top', horizontal: 'left' }}
                transformOrigin={{ vertical: 'top', horizontal: 'right' }}>
                <MenuItem component={Link} href={user.homepage} onClick={close}>
                    <MenuItemText>
                        <GitHubIcon sx={itemIcon} />
                        {user.loginName}
                    </MenuItemText>
                </MenuItem>
                <MenuItem component={RouteLink} to={UserSettingsRoutes.PROFILE} onClick={close}>
                    <MenuItemText>
                        <SettingsIcon sx={itemIcon} />
                        Settings
                    </MenuItemText>
                </MenuItem>
                {user.role === 'admin' ? (
                    <MenuItem component={RouteLink} to={AdminDashboardRoutes.MAIN} onClick={close}>
                        <MenuItemText>
                            <AdminPanelSettingsIcon sx={itemIcon} />
                            Admin Dashboard
                        </MenuItemText>
                    </MenuItem>
                ) : null}
                <MenuItem onClick={() => logoutFormRef.current?.submit()}>
                    <LogoutForm ref={logoutFormRef}>
                        <MenuItemText>
                            <LogoutIcon sx={itemIcon} />
                            Log Out
                        </MenuItemText>
                    </LogoutForm>
                </MenuItem>
            </Menu>
        </>
    );
};

export const MobileMenuContent: FunctionComponent = () => {
    const location = useLocation();
    const { user, loginProviders } = useContext(MainContext);

    return (
        <>
            {loginProviders &&
                (user ? (
                    <MobileUserAvatar />
                ) : (
                    <LoginComponent
                        loginProviders={loginProviders}
                        renderButton={(href, onClick) => (
                            <MenuItem component={Link} href={href} onClick={onClick}>
                                <MenuItemText>
                                    <AccountBoxIcon sx={itemIcon} />
                                    Log In
                                </MenuItemText>
                            </MenuItem>
                        )}
                    />
                ))}
            {loginProviders && !location.pathname.startsWith(UserSettingsRoutes.ROOT) && (
                <MenuItem component={RouteLink} to={UserSettingsRoutes.EXTENSIONS}>
                    <MenuItemText>
                        <PublishIcon sx={itemIcon} />
                        Publish Extension
                    </MenuItemText>
                </MenuItem>
            )}
            <MenuItem component={Link} href='https://github.com/eclipse/openvsx' target='_blank'>
                <MenuItemText>
                    <GitHubIcon sx={itemIcon} />
                    Source Code
                </MenuItemText>
            </MenuItem>
            <MenuItem component={Link} href={DOCS_URL}>
                <MenuItemText>
                    <MenuBookIcon sx={itemIcon} />
                    Documentation
                </MenuItemText>
            </MenuItem>
            <MenuItem component={RouteLink} to='/about'>
                <MenuItemText>
                    <InfoIcon sx={itemIcon} />
                    About This Service
                </MenuItemText>
            </MenuItem>
        </>
    );
};

//-------------------- Default View --------------------//

// eslint-disable-next-line react-refresh/only-export-components
export const headerItem = ({ theme }: { theme: Theme }) => ({
    margin: theme.spacing(0, 0.5),
    padding: theme.spacing(1, 1.5),
    // Inherited color so the items follow the nav's content color over page
    // bands; opacity stands in for the secondary/primary text pair.
    color: 'inherit',
    opacity: 0.78,
    textDecoration: 'none',
    fontSize: '0.875rem',
    fontFamily: theme.typography.fontFamily,
    fontWeight: 500,
    letterSpacing: 0,
    borderRadius: `${theme.shape.borderRadius}px`,
    transition: 'background 0.14s, opacity 0.14s',
    '&:hover': {
        opacity: 1,
        backgroundColor: 'color-mix(in srgb, currentcolor 8%, transparent)',
        textDecoration: 'none'
    },
    ...focusOutline(theme)
});

// eslint-disable-next-line react-refresh/only-export-components
export const MenuLink = styled(Link)(headerItem);
// eslint-disable-next-line react-refresh/only-export-components
export const MenuRouteLink = styled(RouteLink)(headerItem);

export const DefaultMenuContent: FunctionComponent = () => {
    const { user, loginProviders } = useContext(MainContext);
    const navigate = useNavigate();

    // Register each shortcut next to the control it drives, sharing the same
    // destination so the hint, the click, and the keypress stay in sync.
    useShortcut({ key: 'd', label: 'Documentation', order: 2, callback: () => window.location.assign(DOCS_URL) });
    useShortcut({
        key: 'p',
        label: 'Publish',
        order: 3,
        callback: () => navigate(UserSettingsRoutes.EXTENSIONS),
        enabled: !!loginProviders
    });

    return (
        <>
            <MenuLink href={DOCS_URL} sx={{ display: 'inline-flex', alignItems: 'center', gap: '0.4375rem' }}>
                Documentation
                <KbdKey>d</KbdKey>
            </MenuLink>
            <MenuRouteLink to='/about'>About</MenuRouteLink>
            {loginProviders && (
                <>
                    <Button
                        variant='text'
                        color='secondary'
                        href={UserSettingsRoutes.EXTENSIONS}
                        sx={theme => ({
                            mx: 0.5,
                            px: 2.25,
                            py: 1,
                            fontWeight: 600,
                            fontSize: '0.8125rem',
                            borderRadius: `${theme.shape.borderRadius}px`,
                            display: 'inline-flex',
                            alignItems: 'center',
                            gap: '0.4375rem',
                            '&:hover': { backgroundColor: alpha(theme.palette.secondary.main, 0.08) }
                        })}>
                        Publish
                        <KbdKey>p</KbdKey>
                    </Button>
                    {user ? (
                        <UserAvatar />
                    ) : (
                        <LoginComponent
                            loginProviders={loginProviders}
                            renderButton={(href, onClick) => {
                                // The default `action.active` gray disappears on tinted chromes.
                                if (href) {
                                    return (
                                        <IconButton href={href} color='inherit' title='Log In' aria-label='Log In'>
                                            <AccountBoxIcon />
                                        </IconButton>
                                    );
                                } else {
                                    return (
                                        <IconButton
                                            onClick={onClick}
                                            color='inherit'
                                            title='Log In'
                                            aria-label='Log In'>
                                            <AccountBoxIcon />
                                        </IconButton>
                                    );
                                }
                            }}
                        />
                    )}
                </>
            )}
        </>
    );
};
