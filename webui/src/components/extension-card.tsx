/******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/

import { FunctionComponent, useContext, useState, useEffect, useRef } from 'react';
import { flushSync } from 'react-dom';
import { Link as RouteLink, useNavigate } from 'react-router-dom';
import { Paper, Typography, Box, Fade } from '@mui/material';
import { styled, alpha } from '@mui/material/styles';
import SaveAltIcon from '@mui/icons-material/SaveAlt';
import { MainContext } from '../context';
import { ExtensionDetailRoutes } from '../pages/extension-detail/extension-detail-routes';
import { SearchEntry } from '../extension-registry-types';
import { ExtensionRatingStars } from '../pages/extension-detail/extension-rating-stars';
import { createRoute } from '../utils';
import { MONO_FONT } from '../default/theme';

const CardRoot = styled(Paper)(({ theme }) => ({
    padding: '22px 16px',
    [theme.breakpoints.down('sm')]: { padding: '14px 10px' },
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    textAlign: 'center',
    height: '100%',
    minHeight: '206px',
    border: `1px solid ${theme.palette.divider}`,
    borderRadius: theme.shape.borderRadiusCard,
    backgroundColor: theme.palette.background.paper,
    cursor: 'pointer',
    transition: 'border-color 0.15s, box-shadow 0.15s, transform 0.15s',
    '&:hover': {
        borderColor: theme.palette.secondary.main,
        boxShadow: 'var(--shadow)',
        transform: 'translateY(-2px)'
    },
    // Keyboard focus ring — mirrors the search field's :focus-within style
    'a:focus-visible &': {
        borderColor: theme.palette.secondary.main,
        boxShadow: `0 0 0 3px ${alpha(theme.palette.secondary.main, 0.16)}`
    }
}));

export const ExtensionCard: FunctionComponent<ExtensionCardProps> = props => {
    const [icon, setIcon] = useState<string>();
    const context = useContext(MainContext);
    const abortController = useRef<AbortController>(new AbortController());

    useEffect(() => {
        updateChanges();
        return () => {
            abortController.current.abort();
            if (icon) URL.revokeObjectURL(icon);
        };
    }, []);

    useEffect(() => {
        updateChanges();
    }, [props.extension.namespace, props.extension.name, props.extension.version]);

    const updateChanges = async (): Promise<void> => {
        if (icon) URL.revokeObjectURL(icon);
        try {
            const icon = await context.service.getExtensionIcon(abortController.current, props.extension);
            setIcon(icon);
        } catch (err) {
            context.handleError(err);
        }
    };

    const { extension, filterSize, idx } = props;
    const navigate = useNavigate();
    const route = createRoute([ExtensionDetailRoutes.ROOT, extension.namespace, extension.name]);
    const numberFormat = new Intl.NumberFormat(undefined, { notation: 'compact', compactDisplay: 'short' } as any);
    const downloadCountFormatted = numberFormat.format(extension.downloadCount ?? 0);
    const vtName = `ext-${extension.namespace}-${extension.name}`.replace(/[^a-zA-Z0-9-]/g, '-');

    const handleCardClick = (e: React.MouseEvent) => {
        if (!('startViewTransition' in document)) return;
        e.preventDefault();
        const img = (e.currentTarget as HTMLElement).querySelector('img');
        if (img) img.style.viewTransitionName = vtName;
        (document as any).startViewTransition(() => {
            flushSync(() => navigate(route));
        });
    };

    return (
        <Fade in={true} timeout={{ enter: ((filterSize + idx) % filterSize) * 200 }}>
            <Box title={extension.displayName ?? extension.name} sx={{ height: '100%' }}>
                <RouteLink
                    to={route}
                    data-ext-card
                    aria-label={extension.displayName ?? extension.name}
                    style={{ textDecoration: 'none', height: '100%', display: 'block', outline: 'none' }}
                    onClick={handleCardClick}>
                    <CardRoot
                        elevation={0}
                        sx={extension.deprecated ? { opacity: 0.5, filter: 'grayscale(100%)' } : undefined}>
                        <Box
                            display='flex'
                            justifyContent='center'
                            alignItems='center'
                            flexShrink={0}
                            sx={{
                                width: { xs: 40, sm: 54 },
                                height: { xs: 40, sm: 54 },
                                mb: { xs: '10px', sm: '14px' }
                            }}>
                            <Box
                                component='img'
                                src={icon ?? context.pageSettings.urls.extensionDefaultIcon}
                                alt={extension.displayName ?? extension.name}
                                sx={{ width: { xs: 40, sm: 54 }, maxHeight: { xs: 40, sm: 54 }, objectFit: 'contain' }}
                            />
                        </Box>
                        <Typography
                            sx={{
                                fontSize: { xs: '13px', sm: '14.5px' },
                                fontWeight: 700,
                                lineHeight: 1.3,
                                width: '100%',
                                minHeight: { xs: '34px', sm: '38px' },
                                display: '-webkit-box',
                                WebkitLineClamp: 2,
                                WebkitBoxOrient: 'vertical',
                                overflow: 'hidden'
                            }}>
                            {extension.displayName ?? extension.name}
                        </Typography>
                        <Box
                            sx={{
                                width: '100%',
                                mt: '14px',
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'space-between',
                                gap: 1
                            }}>
                            <Typography
                                component='div'
                                noWrap
                                sx={{ fontSize: '12px', color: 'text.disabled', minWidth: 0, textAlign: 'left' }}>
                                {extension.namespace}
                            </Typography>
                            <Typography
                                component='div'
                                noWrap
                                sx={{ fontSize: '11px', color: 'text.disabled', flexShrink: 0, fontFamily: MONO_FONT }}>
                                {extension.version}
                            </Typography>
                        </Box>
                        <Box
                            sx={{
                                width: '100%',
                                mt: 'auto',
                                pt: '11px',
                                borderTop: '1px solid',
                                borderColor: 'border2',
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'space-between',
                                fontSize: '12.5px'
                            }}>
                            <Box sx={{ display: 'flex', fontSize: { xs: '14px', sm: '20px' } }}>
                                <ExtensionRatingStars number={extension.averageRating ?? 0} fontSize='inherit' />
                            </Box>
                            {downloadCountFormatted !== '0' && (
                                <Box
                                    component='span'
                                    sx={{
                                        display: 'flex',
                                        alignItems: 'center',
                                        gap: '4px',
                                        fontFamily: MONO_FONT,
                                        fontSize: '11px',
                                        color: 'text.disabled'
                                    }}>
                                    <SaveAltIcon sx={{ fontSize: '13px' }} />
                                    {downloadCountFormatted}
                                </Box>
                            )}
                        </Box>
                    </CardRoot>
                </RouteLink>
            </Box>
        </Fade>
    );
};

export interface ExtensionCardProps {
    extension: SearchEntry;
    idx: number;
    filterSize: number;
}
