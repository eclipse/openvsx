/********************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import { FunctionComponent, useContext, useEffect, useState } from 'react';
import { AppBar, Box, Toolbar } from '@mui/material';
import { alpha, styled, useTheme } from '@mui/material/styles';
import { Link as RouteLink } from 'react-router-dom';
import { HeaderMenu } from '../header-menu';
import { MainContext } from '../context';
import { useSearchFocus } from '../context/search/search-focus-context';
import { OpenVsxMark } from '../components/openvsx-mark';
import { NavSearchField } from './nav-search-field';

const ToolbarItem = styled(Box)({
    display: 'flex',
    alignItems: 'center'
});

// Alpha multipliers for the fades below, sampled from smootherstep. A two-stop
// linear fade changes slope abruptly at its endpoints and the eye amplifies
// that into a visible line (Mach banding); these stops land on transparent
// with zero slope so the fade has no perceivable end.
const FADE_EASE = [1, 0.984, 0.897, 0.725, 0.5, 0.275, 0.103, 0.016, 0];

// Progressive ("gradient") blur, cumulative form: every layer is anchored to
// the top edge and fades out at a progressively deeper point, so a row's blur
// is the compound of all layers still active there (stacked Gaussian blurs
// compose in quadrature — σ² adds). The strengths below are the quadrature
// differences between consecutive steps of a ~√2 falloff ramp, compounding to
// ~6px at the toolbar and ~0 at the fan's bottom edge. Unlike side-by-side
// banded masks — whose shared boundaries put two blur levels in full contact
// and read as lines — each mask here has a single smootherstep fade edge, and
// the edges are staggered 12.5% apart, so blur strictly and smoothly decreases.
const BLUR_LAYERS = ['4.25px', '3px', '2.15px', '1.5px', '1.1px', '0.65px', '0.55px', '0.5px'].map((blur, i) => ({
    blur,
    mask: `linear-gradient(to bottom, ${FADE_EASE.map(
        (k, j) => `rgba(0, 0, 0, ${k}) ${12.5 * i + (25 * j) / (FADE_EASE.length - 1)}%`
    ).join(', ')})`
}));

export const AppNavbar: FunctionComponent = () => {
    const { pageSettings } = useContext(MainContext);
    const { toolbarContent: ToolbarContent } = pageSettings.elements;
    const { hasPageSearchBar } = useSearchFocus();
    const theme = useTheme();
    const baseAlpha = theme.palette.mode === 'dark' ? 0.74 : 0.78;
    const navbg = alpha(theme.palette.background.default, baseAlpha);
    // The scrolled tint is deliberately lighter than the solid glass: the blur fan
    // already provides legibility, so the tint only needs a faint wash of color.
    const tintAlpha = theme.palette.mode === 'dark' ? 0.3 : 0.35;
    const tintGradient = `linear-gradient(to bottom, ${FADE_EASE.map(
        (k, i) => `${alpha(theme.palette.background.default, tintAlpha * k)} ${(i * 100) / (FADE_EASE.length - 1)}%`
    ).join(', ')})`;
    const saturationMask = `linear-gradient(to bottom, ${FADE_EASE.map(
        (k, i) => `rgba(0, 0, 0, ${k}) ${(i * 100) / (FADE_EASE.length - 1)}%`
    ).join(', ')})`;
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
                        bottom: { xs: '-48px', sm: '-80px' },
                        pointerEvents: 'none',
                        zIndex: 0,
                        opacity: showFan ? 1 : 0,
                        transition: 'opacity 0.35s ease',
                        backdropFilter: `blur(${layer.blur})`,
                        WebkitBackdropFilter: `blur(${layer.blur})`,
                        WebkitMaskImage: layer.mask,
                        maskImage: layer.mask
                    }}
                />
            ))}
            {/* Saturation wash — one continuously-masked layer over the whole fan */}
            <Box
                aria-hidden='true'
                sx={{
                    position: 'absolute',
                    left: 0,
                    right: 0,
                    top: 0,
                    bottom: { xs: '-48px', sm: '-80px' },
                    pointerEvents: 'none',
                    zIndex: 0,
                    opacity: showFan ? 1 : 0,
                    transition: 'opacity 0.35s ease',
                    backdropFilter: 'saturate(2)',
                    WebkitBackdropFilter: 'saturate(2)',
                    WebkitMaskImage: saturationMask,
                    maskImage: saturationMask
                }}
            />
            <Box
                aria-hidden='true'
                sx={{
                    position: 'absolute',
                    left: 0,
                    right: 0,
                    top: 0,
                    bottom: { xs: '-48px', sm: '-150px' },
                    pointerEvents: 'none',
                    zIndex: 0,
                    opacity: showFan ? 1 : 0,
                    transition: 'opacity 0.35s ease',
                    background: tintGradient
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
                    {/* Mobile compact icon — shown while the nav search field is visible. The right
                        margin balances the burger button's own padding so the search field sits centered. */}
                    <Box
                        sx={{
                            display: { xs: hasPageSearchBar ? 'none' : 'flex', md: 'none' },
                            alignItems: 'center',
                            mr: '0.5rem'
                        }}>
                        <RouteLink to='/' aria-label='Home' style={{ display: 'flex', textDecoration: 'none' }}>
                            {/* Same mark height as the full toolbar logo */}
                            <OpenVsxMark style={{ height: '2.5rem' }} />
                        </RouteLink>
                    </Box>
                    {/* Full logo — desktop always, mobile only while the nav search field is hidden. */}
                    <Box sx={{ display: { xs: hasPageSearchBar ? 'flex' : 'none', md: 'flex' } }}>
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
