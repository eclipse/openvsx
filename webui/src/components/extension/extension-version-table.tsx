/******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/

import { ChangeEvent, FunctionComponent, useState } from 'react';
import { Box, IconButton, InputBase, Typography } from '@mui/material';
import { alpha, styled } from '@mui/material/styles';
import ChevronLeftIcon from '@mui/icons-material/ChevronLeft';
import ChevronRightIcon from '@mui/icons-material/ChevronRight';
import DeleteForeverIcon from '@mui/icons-material/DeleteForever';
import SearchIcon from '@mui/icons-material/Search';
import { VersionTargetPlatforms } from '../../extension-registry-types';
import { DeleteIconButton } from '../delete-icon-button';
import { cardSurface, focusRing, TagChip } from '../page-primitives';
import { MONO_FONT } from '../../default/theme';

const PAGE_SIZE = 20;

const TableCard = styled(Box)(({ theme }) => ({
    ...cardSurface(theme),
    overflow: 'hidden'
}));

const FilterBox = styled(Box)(({ theme }) => ({
    display: 'flex',
    alignItems: 'center',
    gap: '0.5625rem',
    height: '2.375rem',
    padding: '0 0.75rem',
    backgroundColor: theme.palette.surface2,
    border: `1px solid ${theme.palette.divider}`,
    borderRadius: theme.shape.borderRadius,
    transition: 'border-color 0.15s, box-shadow 0.15s',
    '&:focus-within': focusRing(theme)
}));

const Row = styled(Box)(({ theme }) => ({
    display: 'grid',
    gap: '0.75rem',
    alignItems: 'center',
    padding: '0.375rem 1.125rem',
    borderBottom: `1px solid ${theme.palette.border2}`
}));

const VersionChip = styled(TagChip)({
    fontSize: '0.5625rem'
});

const ErrorChip = styled(VersionChip)(({ theme }) => ({
    backgroundColor: alpha(theme.palette.error.main, 0.12),
    color: theme.palette.error.main
}));

/** Circular twin of {@link DeleteIconButton} for the admin-only permanent purge. */
const PurgeIconButton = styled(IconButton)(({ theme }) => ({
    width: '1.875rem',
    height: '1.875rem',
    backgroundColor: theme.palette.surface2,
    border: `1px solid ${theme.palette.error.main}`,
    color: theme.palette.error.main,
    transition: 'background-color 0.15s',
    '&:hover': {
        backgroundColor: theme.palette.error.main,
        color: theme.palette.common.white
    }
}));

const PagerButton = styled(IconButton)(({ theme }) => ({
    width: '2rem',
    height: '2rem',
    borderRadius: theme.shape.borderRadius,
    border: `1px solid ${theme.palette.divider}`,
    backgroundColor: theme.palette.background.paper,
    color: theme.palette.text.secondary
}));

/** Clamps `page` to the filtered list and derives everything the pager needs. */
const paginate = (versions: VersionTargetPlatforms[], page: number) => {
    const pageCount = Math.max(1, Math.ceil(versions.length / PAGE_SIZE));
    const safePage = Math.min(page, pageCount - 1);
    return {
        pageCount,
        safePage,
        pagedVersions: versions.slice(safePage * PAGE_SIZE, (safePage + 1) * PAGE_SIZE),
        hasPrevPage: safePage > 0,
        hasNextPage: safePage < pageCount - 1,
        from: versions.length === 0 ? 0 : safePage * PAGE_SIZE + 1,
        to: Math.min(versions.length, (safePage + 1) * PAGE_SIZE)
    };
};

/** Why a version's delete action is unavailable, or `undefined` when it is available. */
const deleteBlockedReason = (version: VersionTargetPlatforms): string | undefined => {
    // canDelete is only populated in the user settings view; undefined means unrestricted.
    if (version.canDelete === false) {
        return 'Only the publisher or a namespace owner can delete this version';
    }
    if (version.targetPlatforms.length > 0 && version.targetPlatforms.every(tp => tp.removed)) {
        return 'Version already removed';
    }
    return undefined;
};

interface TimelineCellProps {
    index: number;
    rowCount: number;
    hasPrevPage: boolean;
    hasNextPage: boolean;
    /** Palette path for the bulb; see {@link bulbColor}. */
    color: string;
}

/** The bulb marks the latest version with the accent and a removed one in red. */
const bulbColor = (isLatest: boolean, removed: boolean): string => {
    if (removed) {
        return 'error.main';
    }
    return isLatest ? 'secondary.main' : 'text.disabled';
};

/**
 * Leading cell of a version row: a bulb on a vertical line running through the
 * row gaps. At page boundaries the line fades out instead of ending on the
 * bulb, hinting that more versions follow.
 */
const TimelineCell: FunctionComponent<TimelineCellProps> = ({ index, rowCount, hasPrevPage, hasNextPage, color }) => {
    const isTop = index === 0;
    const isBottom = index === rowCount - 1;
    const top = isTop ? (hasPrevPage ? '-0.625rem' : '50%') : '-0.875rem';
    const bottom = isBottom ? (hasNextPage ? '-0.625rem' : '50%') : '-0.875rem';
    const fade = isBottom && hasNextPage ? 'to bottom' : isTop && hasPrevPage ? 'to top' : undefined;

    return (
        <Box component='span' sx={{ position: 'relative', alignSelf: 'stretch', minHeight: '1.875rem' }}>
            <Box
                component='span'
                sx={theme => ({
                    position: 'absolute',
                    left: '50%',
                    transform: 'translateX(-50%)',
                    top,
                    bottom,
                    width: '2px',
                    background: fade
                        ? `linear-gradient(${fade}, ${theme.palette.divider} 40%, transparent)`
                        : theme.palette.divider
                })}
            />
            <Box
                component='span'
                sx={{
                    position: 'absolute',
                    left: '50%',
                    top: '50%',
                    transform: 'translate(-50%, -50%)',
                    zIndex: 1,
                    width: '0.625rem',
                    height: '0.625rem',
                    borderRadius: '50%',
                    bgcolor: color
                }}
            />
        </Box>
    );
};

export const ExtensionVersionTable: FunctionComponent<ExtensionVersionTableProps> = ({
    versions,
    latestVersion,
    rejected,
    page,
    onPageChange,
    onDeleteVersion,
    onPurgeVersion
}) => {
    const [filter, setFilter] = useState('');

    const onFilterChange = (event: ChangeEvent<HTMLInputElement>) => {
        setFilter(event.target.value);
        onPageChange(0);
    };

    const filtered = versions.filter(v => !filter.trim() || v.version.includes(filter.trim()));
    const { pageCount, safePage, pagedVersions, hasPrevPage, hasNextPage, from, to } = paginate(filtered, page);
    // Fixed end tracks: each row is its own grid, `auto` would misalign header and rows.
    const gridTemplateColumns = onPurgeVersion ? '1.875rem 1.5fr 1fr 1.875rem 1.875rem' : '1.875rem 1.5fr 1fr 1.875rem';

    return (
        <TableCard>
            <Box sx={{ p: '0.75rem 0.875rem', borderBottom: '1px solid', borderColor: 'border2' }}>
                <FilterBox>
                    <SearchIcon sx={{ fontSize: '0.9375rem', color: 'text.disabled', flexShrink: 0 }} />
                    <InputBase
                        value={filter}
                        onChange={onFilterChange}
                        placeholder='filter versions… e.g. 1.3'
                        inputProps={{ 'aria-label': 'Filter versions' }}
                        sx={{ flex: 1, fontFamily: MONO_FONT, fontSize: '0.84375rem' }}
                    />
                </FilterBox>
            </Box>
            <Row
                sx={{
                    gridTemplateColumns,
                    p: '0.6875rem 1.125rem',
                    bgcolor: 'surface2',
                    borderBottomColor: 'divider',
                    fontSize: '0.6875rem',
                    fontWeight: 700,
                    textTransform: 'uppercase',
                    letterSpacing: '0.05em',
                    color: 'text.disabled'
                }}>
                <span />
                <span>Version</span>
                <span>Target platforms</span>
                <span />
                {onPurgeVersion ? <span /> : null}
            </Row>
            {pagedVersions.map((v, index) => {
                const isLatest = v.version === latestVersion;
                const preRelease = v.version.includes('-');
                const blockedReason = deleteBlockedReason(v);
                const removed = v.targetPlatforms.length > 0 && v.targetPlatforms.every(tp => tp.removed);
                return (
                    <Row key={v.version} sx={{ gridTemplateColumns }}>
                        <TimelineCell
                            index={index}
                            rowCount={pagedVersions.length}
                            hasPrevPage={hasPrevPage}
                            hasNextPage={hasNextPage}
                            color={bulbColor(isLatest, removed)}
                        />
                        <Box
                            component='span'
                            sx={{ display: 'flex', alignItems: 'center', gap: '0.5rem', minWidth: 0 }}>
                            <Typography
                                component='code'
                                noWrap
                                sx={{
                                    fontFamily: MONO_FONT,
                                    fontSize: '0.8125rem',
                                    fontWeight: 600,
                                    color: removed ? 'error.main' : 'inherit',
                                    textDecoration: removed ? 'line-through' : 'none'
                                }}>
                                {v.version}
                            </Typography>
                            {removed ? (
                                <ErrorChip>Removed</ErrorChip>
                            ) : rejected ? (
                                <ErrorChip>Rejected</ErrorChip>
                            ) : isLatest ? (
                                <VersionChip accent>Latest</VersionChip>
                            ) : preRelease ? (
                                <VersionChip>Pre-release</VersionChip>
                            ) : null}
                        </Box>
                        <Typography component='span' sx={{ fontSize: '0.78125rem', color: 'text.secondary' }}>
                            {v.targetPlatforms.map((tp, tpIndex) => (
                                <Typography key={tp.targetPlatform} component='span' sx={{ fontSize: 'inherit' }}>
                                    <Typography
                                        component='span'
                                        title={tp.removed ? 'Removed' : tp.active ? undefined : 'Inactive'}
                                        sx={{
                                            fontSize: 'inherit',
                                            // Red is carried by the version, pill and dot; the platform
                                            // list just recedes so the struck-through text stays quiet.
                                            color: tp.removed ? 'text.disabled' : 'inherit',
                                            textDecoration: tp.active ? 'none' : 'line-through'
                                        }}>
                                        {tp.targetPlatform}
                                    </Typography>
                                    {tpIndex < v.targetPlatforms.length - 1 ? ', ' : ''}
                                </Typography>
                            ))}
                        </Typography>
                        <DeleteIconButton
                            title={blockedReason ?? 'Delete this version permanently'}
                            aria-label={`Delete version ${v.version}`}
                            disabled={blockedReason != null}
                            onClick={() => onDeleteVersion(v)}
                            sx={{ justifySelf: 'end' }}
                        />
                        {onPurgeVersion ? (
                            <PurgeIconButton
                                title='Purge version permanently'
                                aria-label={`Purge version ${v.version}`}
                                onClick={() => onPurgeVersion(v)}
                                sx={{ justifySelf: 'end' }}>
                                <DeleteForeverIcon sx={{ fontSize: '0.9375rem' }} />
                            </PurgeIconButton>
                        ) : null}
                    </Row>
                );
            })}
            {filtered.length === 0 ? (
                <Typography
                    sx={{
                        p: '1.625rem 1.125rem',
                        textAlign: 'center',
                        fontSize: '0.84375rem',
                        color: 'text.disabled'
                    }}>
                    {versions.length === 0 ? 'No version information available.' : 'No versions match that filter.'}
                </Typography>
            ) : null}
            {pageCount > 1 ? (
                <Box
                    sx={{
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'space-between',
                        gap: '0.75rem',
                        p: '0.6875rem 1.125rem',
                        bgcolor: 'surface2'
                    }}>
                    <Typography sx={{ fontSize: '0.78125rem', color: 'text.disabled' }}>
                        Showing {from}–{to} of {filtered.length} versions
                    </Typography>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: '0.375rem' }}>
                        <PagerButton
                            aria-label='Previous page'
                            disabled={!hasPrevPage}
                            onClick={() => onPageChange(safePage - 1)}>
                            <ChevronLeftIcon sx={{ fontSize: '0.9375rem' }} />
                        </PagerButton>
                        <Typography
                            sx={{ fontSize: '0.78125rem', fontWeight: 600, color: 'text.secondary', px: '0.375rem' }}>
                            {safePage + 1} / {pageCount}
                        </Typography>
                        <PagerButton
                            aria-label='Next page'
                            disabled={!hasNextPage}
                            onClick={() => onPageChange(safePage + 1)}>
                            <ChevronRightIcon sx={{ fontSize: '0.9375rem' }} />
                        </PagerButton>
                    </Box>
                </Box>
            ) : null}
        </TableCard>
    );
};

export interface ExtensionVersionTableProps {
    versions: VersionTargetPlatforms[];
    /** Version that gets the "Latest" chip and accent bulb. */
    latestVersion?: string;
    /** The review rejected the extension, which blocks every version it has. */
    rejected?: boolean;
    page: number;
    onPageChange: (page: number) => void;
    onDeleteVersion: (version: VersionTargetPlatforms) => void;
    // When provided (admin only), each row shows a permanent-purge action.
    onPurgeVersion?: (version: VersionTargetPlatforms) => void;
}
