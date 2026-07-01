/******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/

import { FunctionComponent, useCallback, useContext, useEffect, useState } from 'react';
import { flushSync } from 'react-dom';
import { AppBar, Box, Toolbar } from '@mui/material';
import { styled, useTheme } from '@mui/material/styles';
import { Link as RouteLink, useNavigate } from 'react-router-dom';
import { HeaderMenu } from '../header-menu';
import { MainContext } from '../context';
import { useNavSearch } from '../nav-search-context';
import { OpenVsxMark } from '../components/openvsx-mark';
import { NavSearchField } from './nav-search-field';

const ToolbarItem = styled(Box)({
    display: 'flex',
    alignItems: 'center'
});

const BLUR_LAYERS = [
    { blur: '22px', sat: 2.0, stops: 'transparent 87.5%, #000 100%' },
    { blur: '14px', sat: 1.7, stops: 'transparent 75%, #000 87.5%, #000 100%' },
    { blur: '8px', sat: 1.45, stops: 'transparent 62.5%, #000 75%, #000 87.5%, transparent 100%' },
    { blur: '5px', sat: 1.4, stops: 'transparent 50%, #000 62.5%, #000 75%, transparent 87.5%' },
    { blur: '3px', sat: 1.35, stops: 'transparent 37.5%, #000 50%, #000 62.5%, transparent 75%' },
    { blur: '2px', sat: 1.3, stops: 'transparent 25%, #000 37.5%, #000 50%, transparent 62.5%' },
    { blur: '1px', sat: 1, stops: 'transparent 12.5%, #000 25%, #000 37.5%, transparent 50%' },
    { blur: '0.5px', sat: 1, stops: '#000 0%, #000 12.5%, #000 25%, transparent 37.5%' }
];

export const AppNavbar: FunctionComponent = () => {
    const { pageSettings } = useContext(MainContext);
    const { toolbarContent: ToolbarContent } = pageSettings.elements;
    const { isHeroPage } = useNavSearch();
    const theme = useTheme();
    const navigate = useNavigate();
    const navbg = theme.palette.mode === 'dark' ? 'rgba(14, 14, 20, 0.74)' : 'rgba(255, 255, 255, 0.78)';
    const [scrolled, setScrolled] = useState(false);

    // Trigger the reverse view-transition (nav search → hero) when navigating home
    const handleHomeClick = useCallback(
        (e: { preventDefault(): void; stopPropagation(): void }) => {
            if (isHeroPage) return;
            if (!('startViewTransition' in document)) return;
            e.preventDefault();
            e.stopPropagation();
            (document as any).startViewTransition(() => {
                flushSync(() => navigate('/'));
            });
        },
        [isHeroPage, navigate]
    );

    useEffect(() => {
        const onScroll = () => setScrolled(window.scrollY > 20);
        window.addEventListener('scroll', onScroll, { passive: true });
        return () => window.removeEventListener('scroll', onScroll);
    }, []);

    const showSolid = !scrolled;
    const showFan = scrolled;

    return (
        <AppBar position='sticky' color='transparent' elevation={0} sx={{ overflow: 'visible !important', zIndex: 50 }}>
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
                        bottom: '-100px',
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
                    bottom: '-100px',
                    pointerEvents: 'none',
                    zIndex: 0,
                    opacity: showFan ? 1 : 0,
                    transition: 'opacity 0.35s ease',
                    background: `linear-gradient(to bottom, ${navbg} 0%, ${navbg} 38%, transparent 100%)`
                }}
            />
            <Toolbar
                sx={{
                    justifyContent: 'space-between',
                    minHeight: '62px !important',
                    px: '28px !important',
                    position: 'relative',
                    zIndex: 1
                }}>
                <ToolbarItem>
                    {/* Mobile compact icon — shown on non-home pages */}
                    <Box sx={{ display: { xs: isHeroPage ? 'none' : 'flex', md: 'none' }, alignItems: 'center' }}>
                        <RouteLink
                            to='/'
                            aria-label='Home'
                            onClick={handleHomeClick}
                            style={{ display: 'flex', textDecoration: 'none' }}>
                            <OpenVsxMark />
                        </RouteLink>
                    </Box>
                    {/* Full logo — desktop always, mobile only on home page.
                        onClickCapture intercepts the inner RouteLink so we can wrap it in startViewTransition. */}
                    <Box
                        sx={{ display: { xs: isHeroPage ? 'flex' : 'none', md: 'flex' } }}
                        onClickCapture={handleHomeClick}>
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
