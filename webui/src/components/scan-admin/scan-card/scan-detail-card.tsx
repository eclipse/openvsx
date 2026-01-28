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

import React, { FunctionComponent, ReactNode } from 'react';
import { Box, Typography, Chip } from '@mui/material';
import { Info as InfoIcon } from '@mui/icons-material';
import { useTheme } from '@mui/material/styles';

interface DetailField {
    label: string;
    value: string | undefined;
}

interface ScanDetailCardProps {
    accentColor: string;
    isUnenforced?: boolean;
    chip?: {
        label: string;
        color: string;
        textColor: string;
    };
    description?: string;
    descriptionColor?: string;
    details: DetailField[];
    children?: ReactNode;
}

/**
 * A detail card component for displaying scan detail items like errors,
 * threats, and validation failures. Provides styling with a colored
 * left border, optional chip badge, and metadata fields.
 */
export const ScanDetailCard: FunctionComponent<ScanDetailCardProps> = ({
    accentColor,
    isUnenforced = false,
    chip,
    description,
    descriptionColor,
    details,
    children,
}) => {
    const theme = useTheme();

    return (
        <Box
            sx={{
                position: 'relative',
                p: 1.5,
                bgcolor: theme.palette.scanBackground.light,
                borderRadius: 1,
                paddingLeft: '20px',
                '&::before': {
                    content: '""',
                    position: 'absolute',
                    left: 0,
                    top: 0,
                    bottom: 0,
                    width: '8px',
                    background: isUnenforced
                        ? `${theme.palette.unenforced.stripe}, ${accentColor}`
                        : accentColor,
                    zIndex: 0,
                },
            }}
        >
            <Box sx={{ position: 'relative', zIndex: 1 }}>
                {/* Chip and unenforced indicator */}
                {(chip || isUnenforced) && (
                    <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-start', gap: 1, mb: 1 }}>
                        {isUnenforced && (
                            <Box sx={{
                                display: 'flex',
                                alignItems: 'center',
                                gap: 0.5,
                                px: 1,
                                py: 0.25,
                                backgroundColor: theme.palette.info.dark + '20',
                                borderRadius: 0.5,
                                border: `1px solid ${theme.palette.info.dark}40`,
                            }}>
                                <InfoIcon sx={{ fontSize: 14, color: theme.palette.info.main }} />
                                <Typography variant='caption' sx={{ color: theme.palette.info.main, fontSize: '0.7rem' }}>
                                    Not enforced
                                </Typography>
                            </Box>
                        )}
                        {chip && (
                            <Chip
                                label={chip.label}
                                size='small'
                                sx={{
                                    height: '20px',
                                    fontSize: '0.75rem',
                                    background: isUnenforced
                                        ? `${theme.palette.unenforced.stripe}, ${chip.color}`
                                        : chip.color,
                                    color: chip.textColor,
                                }}
                            />
                        )}
                    </Box>
                )}

                {/* Description */}
                {description && (
                    <Typography
                        variant='body2'
                        sx={{ mb: 1, color: descriptionColor || 'text.primary' }}
                    >
                        {description}
                    </Typography>
                )}

                {/* Detail fields */}
                {details.map((detail, index) => (
                    detail.value && (
                        <Typography
                            key={index}
                            variant='caption'
                            color='text.secondary'
                            display='block'
                            sx={{ mb: 0.5 }}
                        >
                            {detail.label}: {detail.value}
                        </Typography>
                    )
                ))}

                {/* Additional content */}
                {children}
            </Box>
        </Box>
    );
};
