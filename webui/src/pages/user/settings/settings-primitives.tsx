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

import { Box, Button, Typography } from '@mui/material';
import { styled } from '@mui/material/styles';

/** Section heading inside a settings tab (Details, Members, Extensions, …). */
export const SettingsSectionTitle = styled(Typography)({
    fontSize: '1rem',
    fontWeight: 700,
    letterSpacing: '-0.01em',
    margin: '0 0 0.875rem'
}) as typeof Typography;

/** Row pairing a {@link SettingsSectionTitle} with its action button, above the section's card. */
export const SectionTitleRow = styled(Box)({
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: '0.875rem',
    marginBottom: '0.875rem'
});

/** Quiet back-navigation affordance above detail views: no hover surface, just a color shift. */
export const BackButton = styled(Button)(({ theme }) => ({
    color: theme.palette.text.disabled,
    fontWeight: 600,
    padding: 0,
    minWidth: 0,
    justifyContent: 'flex-start',
    '&:hover': {
        backgroundColor: 'transparent',
        color: theme.palette.text.secondary
    }
}));

/** Square tinted tile holding a leading icon in list rows and callout cards. */
export const IconTile = styled(Box)(({ theme }) => ({
    flexShrink: 0,
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: theme.palette.surface3,
    color: theme.palette.text.secondary,
    borderRadius: '0.5625rem'
}));

/** Dashed placeholder shown when a settings list has no entries. */
export const EmptyPlaceholder = styled(Box)(({ theme }) => ({
    border: `1.5px dashed ${theme.palette.divider}`,
    borderRadius: theme.shape.borderRadiusCard,
    padding: '2.5rem',
    textAlign: 'center',
    color: theme.palette.text.disabled,
    fontSize: '0.84375rem'
}));

/** Responsive grid the manage-extension cards render into. */
export const ManageExtensionGrid = styled(Box)(({ theme }) => ({
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fill, minmax(170px, 1fr))',
    gap: '0.875rem',
    [theme.breakpoints.down('sm')]: {
        gridTemplateColumns: 'repeat(2, minmax(0, 1fr))'
    }
}));
