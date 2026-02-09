/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import React, {
    FunctionComponent,
    PropsWithChildren,
    useContext,
    useState
} from 'react';
import {
    Typography,
    MenuItem,
    Link,
    Button,
    IconButton,
    Accordion,
    AccordionSummary,
    Avatar,
    AccordionDetails,
    TextField,
    Box
} from '@mui/material';
import { useLocation, useNavigate } from 'react-router-dom';
import { Link as RouteLink } from 'react-router-dom';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import PublishIcon from '@mui/icons-material/Publish';
import AccountBoxIcon from '@mui/icons-material/AccountBox';
import SearchIcon from '@mui/icons-material/Search';
import SettingsIcon from '@mui/icons-material/Settings';
import LogoutIcon from '@mui/icons-material/Logout';
import { UserAvatar } from '../pages/user/avatar';
import { UserSettingsRoutes } from '../pages/user/user-settings';
import { styled, Theme } from '@mui/material/styles';
import { MainContext } from '../context';
import { LogoutForm } from '../pages/user/logout';
import { LoginComponent } from './login';
import { addQuery } from '../utils';
import { ExtensionListRoutes } from '../pages/extension-list/extension-list-container';

// -------------------- Shared Search -------------------- //

const GlobalSearch: FunctionComponent<{ fullWidth?: boolean }> = ({ fullWidth }) => {
    const [value, setValue] = useState('');
    const navigate = useNavigate();

    const submit = () => {
        if (!value.trim()) {
            return;
        }
        navigate(
            addQuery(ExtensionListRoutes.MAIN, [
                { key: 'query', value }
            ])
        );
        setValue('');
    };

    return (
        <TextField
            size='small'
            placeholder='Search extensions…'
            value={value}
            onChange={(e) => setValue(e.target.value)}
            onKeyDown={(e) => {
                if (e.key === 'Enter') {
                    submit();
                }
            }}
            InputProps={{
                startAdornment: <SearchIcon sx={{ mr: 1 }} />
            }}
            sx={{
                mx: 2,
                width: fullWidth ? '100%' : 260
            }}
        />
    );
};

// -------------------- Mobile View -------------------- //

export const MobileMenuItem = styled(MenuItem)({
    cursor: 'auto',
    '& > a': {
        textDecoration: 'none'
    }
});

export const itemIcon = {
    mr: 1,
    width: '16px',
    height: '16px'
};

export const MobileMenuItemText: FunctionComponent<PropsWithChildren> = ({ children }) => (
    <Typography
        variant='body2'
        color='text.primary'
        sx={{ display: 'flex', alignItems: 'center', textTransform: 'none' }}
    >
        {children}
    </Typography>
);

export const MobileUserAvatar: FunctionComponent = () => {
    const { user } = useContext(MainContext);
    if (!user) {
        return null;
    }

    return (
        <Accordion sx={{ border: 0, borderRadius: 0, boxShadow: 'none', background: 'transparent' }}>
            <AccordionSummary expandIcon={<ExpandMoreIcon />}>
                <MobileMenuItemText>
                    <Avatar src={user.avatarUrl} alt={user.loginName} sx={itemIcon} />
                    {user.loginName}
                </MobileMenuItemText>
            </AccordionSummary>
            <AccordionDetails>
                <MobileMenuItem>
                    <RouteLink to={UserSettingsRoutes.PROFILE}>
                        <MobileMenuItemText>
                            <SettingsIcon sx={itemIcon} />
                            Settings
                        </MobileMenuItemText>
                    </RouteLink>
                </MobileMenuItem>
                <MobileMenuItem>
                    <LogoutForm>
                        <MobileMenuItemText>
                            <LogoutIcon sx={itemIcon} />
                            Log Out
                        </MobileMenuItemText>
                    </LogoutForm>
                </MobileMenuItem>
            </AccordionDetails>
        </Accordion>
    );
};

export const MobileMenuContent: FunctionComponent = () => {
    const location = useLocation();
    const { user, loginProviders } = useContext(MainContext);

    return (
        <>
            <Box sx={{ px: 2, pb: 1 }}>
                <GlobalSearch fullWidth />
            </Box>

            {loginProviders && (
                user ? (
                    <MobileUserAvatar />
                ) : (
                    <MobileMenuItem>
                        <LoginComponent
                            loginProviders={loginProviders}
                            renderButton={(href, onClick) => (
                                <Link href={href} onClick={onClick}>
                                    <MobileMenuItemText>
                                        <AccountBoxIcon sx={itemIcon} />
                                        Log In
                                    </MobileMenuItemText>
                                </Link>
                            )}
                        />
                    </MobileMenuItem>
                )
            )}

            {!location.pathname.startsWith(UserSettingsRoutes.ROOT) && (
                <MobileMenuItem>
                    <RouteLink to='/user-settings/extensions'>
                        <MobileMenuItemText>
                            <PublishIcon sx={itemIcon} />
                            Publish Extension
                        </MobileMenuItemText>
                    </RouteLink>
                </MobileMenuItem>
            )}
        </>
    );
};

// -------------------- Default (Desktop) View -------------------- //

export const headerItem = ({ theme }: { theme: Theme }) => ({
    margin: theme.spacing(2.5),
    color: theme.palette.text.primary,
    textDecoration: 'none',
    fontSize: '1.1rem',
    letterSpacing: 1,
    '&:hover': {
        color: theme.palette.secondary.main
    }
});

export const MenuLink = styled(Link)(headerItem);
export const MenuRouteLink = styled(RouteLink)(headerItem);

export const DefaultMenuContent: FunctionComponent = () => {
    const { user, loginProviders } = useContext(MainContext);

    return (
        <>
            <GlobalSearch />

            <MenuLink href='https://github.com/eclipse/openvsx/wiki'>
                Documentation
            </MenuLink>

            <MenuRouteLink to='/about'>
                About
            </MenuRouteLink>

            {loginProviders && (
                <>
                    <Button
                        variant='contained'
                        color='secondary'
                        href='/user-settings/extensions'
                        sx={{ mx: 2.5 }}
                    >
                        Publish
                    </Button>

                    {user ? (
                        <UserAvatar />
                    ) : (
                        <LoginComponent
                            loginProviders={loginProviders}
                            renderButton={(href, onClick) => {
                                if (href) {
                                    return (
                                        <IconButton
                                            component='a'
                                            href={href}
                                            title='Log In'
                                            aria-label='Log In'
                                        >
                                            <AccountBoxIcon />
                                        </IconButton>
                                    );
                                }
                                return (
                                    <IconButton
                                        onClick={onClick}
                                        title='Log In'
                                        aria-label='Log In'
                                    >
                                        <AccountBoxIcon />
                                    </IconButton>
                                );
                            }}
                        />
                    )}
                </>
            )}
        </>
    );
};

