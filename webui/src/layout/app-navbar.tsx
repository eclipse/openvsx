/******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/

import { FunctionComponent, useContext, useEffect, useState } from 'react';
import { AppBar, Box, Toolbar } from '@mui/material';
import { alpha, styled, useTheme } from '@mui/material/styles';
import { Link as RouteLink, useLocation } from 'react-router-dom';
import { HeaderMenu } from '../header-menu';
import { MainContext } from '../context';
import { ExtensionListRoutes } from '../pages/extension-list/extension-list-routes';
import { OpenVsxMark } from '../components/openvsx-mark';
import { NavSearchField } from './nav-search-field';

const ToolbarItem = styled(Box)({
    display: 'flex',
    alignItems: 'center'
});

// Progressive ("gradient") blur: blur doubles each layer and every mask is an
// equal-width window that overlaps its neighbours by a constant 12.5% step, so
// the layers blend into one continuous ramp instead of visible bands.
const BLUR_LAYERS = [
    { blur: '64px', sat: 1.3, stops: 'transparent 75%, #000 87.5%, #000 100%' },
    { blur: '32px', sat: 1.3, stops: 'transparent 62.5%, #000 75%, #000 87.5%, transparent 100%' },
    { blur: '16px', sat: 1.3, stops: 'transparent 50%, #000 62.5%, #000 75%, transparent 87.5%' },
    { blur: '8px', sat: 1.3, stops: 'transparent 37.5%, #000 50%, #000 62.5%, transparent 75%' },
    { blur: '4px', sat: 1.3, stops: 'transparent 25%, #000 37.5%, #000 50%, transparent 62.5%' },
    { blur: '2px', sat: 1, stops: 'transparent 12.5%, #000 25%, #000 37.5%, transparent 50%' },
    { blur: '1px', sat: 1, stops: 'transparent 0%, #000 12.5%, #000 25%, transparent 37.5%' },
    { blur: '0.5px', sat: 1, stops: '#000 0%, #000 12.5%, transparent 25%' }
];

export const AppNavbar: FunctionComponent = () => {
    const { pageSettings } = useContext(MainContext);
    const { toolbarContent: ToolbarContent } = pageSettings.elements;
    const isHeroPage = useLocation().pathname === ExtensionListRoutes.MAIN;
    const theme = useTheme();
    const navbg = alpha(theme.palette.background.default, theme.palette.mode === 'dark' ? 0.74 : 0.78);
    const [scrolled, setScrolled] = useState(false);

    useEffect(() => {
        const onScroll = () => setScrolled(window.scrollY > 20);
        window.addEventListener('scroll', onScroll, { passive: true });
        return () => window.removeEventListener('scroll', onScroll);
    }, []);

    const showSolid = !scrolled;
    const showFan = scrolled;

    return (
        <AppBar position='sticky' color='transparent' elevation={0} sx={{ overflow: 'visible', zIndex: 50 }}>
            {/* Solid frosted glass — visible when not scrolled */}
            <Box
                aria-hidden='true'
                sx={{
                    position: 'absolute',
                    inset: 0,
                    pointerEvents: 'none',
                    zIndex: 0,
                    background: navbg,
                    backdropFilter: 'blur(12px) saturate(1.3)',
                    WebkitBackdropFilter: 'blur(12px) saturate(1.3)',
                    borderBottom: '1px solid',
                    borderColor: 'divider',
                    opacity: showSolid ? 1 : 0,
                    transition: 'opacity 0.35s ease'
                }}
            />
            {/* Progressive gradient blur fan — fades in on scroll */}
            {BLUR_LAYERS.map((layer, i) => (
                <Box
                    key={i}
                    aria-hidden='true'
                    sx={{
                        position: 'absolute',
                        left: 0,
                        right: 0,
                        top: 0,
                        bottom: { xs: '-48px', sm: '-100px' },
                        pointerEvents: 'none',
                        zIndex: 0,
                        opacity: showFan ? 1 : 0,
                        transition: 'opacity 0.35s ease',
                        backdropFilter: `blur(${layer.blur}) saturate(${layer.sat})`,
                        WebkitBackdropFilter: `blur(${layer.blur}) saturate(${layer.sat})`,
                        WebkitMaskImage: `linear-gradient(to top, ${layer.stops})`,
                        maskImage: `linear-gradient(to top, ${layer.stops})`
                    }}
                />
            ))}
            <Box
                aria-hidden='true'
                sx={{
                    position: 'absolute',
                    left: 0,
                    right: 0,
                    top: 0,
                    bottom: { xs: '-48px', sm: '-100px' },
                    pointerEvents: 'none',
                    zIndex: 0,
                    opacity: showFan ? 1 : 0,
                    transition: 'opacity 0.35s ease',
                    background: `linear-gradient(to bottom, ${navbg} 0%, ${navbg} 38%, transparent 100%)`
                }}
            />
            <Toolbar
                disableGutters
                sx={{
                    justifyContent: 'space-between',
                    px: { xs: '0.625rem', sm: '1.75rem' },
                    position: 'relative',
                    zIndex: 1
                }}>
                <ToolbarItem>
                    {/* Mobile compact icon — shown on non-home pages */}
                    <Box sx={{ display: { xs: isHeroPage ? 'none' : 'flex', md: 'none' }, alignItems: 'center' }}>
                        <RouteLink to='/' aria-label='Home' style={{ display: 'flex', textDecoration: 'none' }}>
                            {/* Same mark height and optical offset as the full toolbar logo */}
                            <OpenVsxMark style={{ height: '2.5rem', marginTop: '0.5rem' }} />
                        </RouteLink>
                    </Box>
                    {/* Full logo — desktop always, mobile only on home page. */}
                    <Box sx={{ display: { xs: isHeroPage ? 'flex' : 'none', md: 'flex' } }}>
                        {ToolbarContent ? <ToolbarContent /> : null}
                    </Box>
                </ToolbarItem>
                <NavSearchField />
                <ToolbarItem>
                    <HeaderMenu />
                </ToolbarItem>
            </Toolbar>
        </AppBar>
    );
};
