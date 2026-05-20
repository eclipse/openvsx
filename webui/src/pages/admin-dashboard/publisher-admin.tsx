/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import { FunctionComponent, createContext, useContext, useMemo, useRef, useState } from 'react';
import {
    Accordion,
    AccordionDetails,
    AccordionSummary,
    Alert,
    Avatar,
    Badge,
    Box,
    Chip,
    CircularProgress,
    Divider,
    FormControl,
    IconButton,
    InputLabel,
    MenuItem,
    Paper,
    Popover,
    Select,
    Stack,
    Tooltip,
    Typography
} from '@mui/material';
import { useQueryClient } from '@tanstack/react-query';
import InfiniteScroll from 'react-infinite-scroller';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import FilterListIcon from '@mui/icons-material/FilterList';
import GitHubIcon from '@mui/icons-material/GitHub';
import AdminPanelSettingsIcon from '@mui/icons-material/AdminPanelSettings';
import PersonIcon from '@mui/icons-material/Person';
import FolderSharedIcon from '@mui/icons-material/FolderShared';
import BusinessIcon from '@mui/icons-material/Business';
import { useParams, useNavigate, Link as RouterLink } from 'react-router-dom';
import { ButtonWithProgress } from '../../components/button-with-progress';
import { AdminUser as PublisherRelationships } from '../../extension-registry-types';
import { ErrorResponse } from '../../server-request';
import { MainContext } from '../../context';
import { PublisherDetails } from './publisher-details';
import { StyledInput } from './namespace-input';
import { SearchListContainer } from './search-list-container';
import { handleError as formatError } from '../../utils';
import { AdminDashboardRoutes } from './admin-dashboard-routes';
import { useDebouncedCallback } from '../../hooks/use-debounced-callback';
import {
    type PublisherRole,
    useInfinitePublishers,
    usePublisherInfo,
    useUpdatePublisherRole
} from './use-publisher-admin';

// eslint-disable-next-line react-refresh/only-export-components
export const UpdateContext = createContext({ handleUpdate: () => {} });

const ROLE_FILTER_OPTIONS = [
    { value: '', label: 'Any role' },
    { value: 'admin', label: 'Admin' },
    { value: 'privileged', label: 'Privileged' },
    { value: 'none', label: 'No role' }
];

const ROLE_EDITOR_OPTIONS: { value: PublisherRole; label: string }[] = [
    { value: 'none', label: 'No role' },
    { value: 'admin', label: 'Admin' },
    { value: 'privileged', label: 'Privileged' }
];

const getPublisherKey = (entry: PublisherRelationships) => `${entry.user.provider}/${entry.user.loginName}`;

const providerIcon = (provider: string | undefined) =>
    provider === 'github' ? <GitHubIcon fontSize='small' /> : <PersonIcon fontSize='small' />;

const roleIcon = (role: string | undefined) =>
    role ? <AdminPanelSettingsIcon fontSize='small' /> : <PersonIcon fontSize='small' />;

function findScrollableAncestor(el: HTMLElement | null): HTMLElement | null {
    while (el) {
        const { overflowY } = getComputedStyle(el);
        if (overflowY === 'auto' || overflowY === 'scroll') return el;
        el = el.parentElement;
    }
    return null;
}

const FiltersPopover: FunctionComponent<{
    anchorEl: HTMLElement | null;
    onClose: () => void;
    roleFilter: string;
    onRoleChange: (role: string) => void;
}> = ({ anchorEl, onClose, roleFilter, onRoleChange }) => (
    <Popover
        open={Boolean(anchorEl)}
        anchorEl={anchorEl}
        onClose={onClose}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'left' }}
        transformOrigin={{ vertical: 'top', horizontal: 'left' }}>
        <Box sx={{ p: 2, width: 220 }}>
            <Typography variant='subtitle2' sx={{ mb: 1.5 }}>
                Filters
            </Typography>
            <FormControl size='small' fullWidth>
                <InputLabel id='role-filter-label'>Role</InputLabel>
                <Select
                    labelId='role-filter-label'
                    value={roleFilter}
                    label='Role'
                    onChange={e => onRoleChange(e.target.value)}>
                    {ROLE_FILTER_OPTIONS.map(o => (
                        <MenuItem key={o.value} value={o.value}>
                            {o.label}
                        </MenuItem>
                    ))}
                </Select>
            </FormControl>
        </Box>
    </Popover>
);

const RoleEditor: FunctionComponent<{
    roleDraft: PublisherRole;
    onChange: (role: PublisherRole) => void;
    onSave: () => void;
    saving: boolean;
}> = ({ roleDraft, onChange, onSave, saving }) => (
    <Stack spacing={1} sx={{ minWidth: 200 }}>
        <Typography variant='subtitle2'>Role</Typography>
        <Stack direction='row' spacing={1} alignItems='center'>
            <FormControl size='small' sx={{ minWidth: 140 }}>
                <Select value={roleDraft} onChange={e => onChange(e.target.value as PublisherRole)} displayEmpty>
                    {ROLE_EDITOR_OPTIONS.map(o => (
                        <MenuItem key={o.value} value={o.value}>
                            {o.label}
                        </MenuItem>
                    ))}
                </Select>
            </FormControl>
            <ButtonWithProgress working={saving} onClick={onSave} title='Save role'>
                Save
            </ButtonWithProgress>
        </Stack>
    </Stack>
);

const PublisherDetailsSection: FunctionComponent<{ provider: string | undefined; loginName: string }> = ({
    provider,
    loginName
}) => {
    const { data, isLoading, error } = usePublisherInfo(loginName, provider ?? 'github');
    return (
        <>
            <Divider sx={{ my: 2 }} />
            {error && (
                <Alert severity='error' sx={{ mb: 2 }}>
                    {formatError(error as Error | Partial<ErrorResponse>)}
                </Alert>
            )}
            {isLoading && (
                <Box sx={{ display: 'flex', justifyContent: 'center', py: 3 }}>
                    <CircularProgress size={24} />
                </Box>
            )}
            {data && <PublisherDetails publisherInfo={data} />}
        </>
    );
};

interface PublisherListItemProps {
    entry: PublisherRelationships;
    index: number;
    expanded: boolean;
    onToggle: (loginName: string, isExpanded: boolean) => void;
}

const PublisherListItem: FunctionComponent<PublisherListItemProps> = ({ entry, index, expanded, onToggle }) => {
    const { user } = entry;
    const { user: currentUser } = useContext(MainContext);
    const isCurrentUser = currentUser?.loginName === user.loginName && currentUser?.provider === user.provider;

    const [roleDraft, setRoleDraft] = useState<PublisherRole>(() => (user.role as PublisherRole) ?? 'none');
    const updateRole = useUpdatePublisherRole();

    const handleSaveRole = () => {
        if (roleDraft === (user.role ?? 'none') || !user.provider) {
            return;
        }
        updateRole.mutate({ provider: user.provider, login: user.loginName, role: roleDraft });
    };

    return (
        <Accordion
            expanded={expanded}
            onChange={(_, isExpanded) => onToggle(user.loginName, isExpanded)}
            disableGutters
            elevation={0}
            slotProps={{ transition: { unmountOnExit: true } }}
            sx={{ '&:before': { display: 'none' }, borderTop: index === 0 ? 'none' : 1, borderColor: 'divider' }}>
            <AccordionSummary expandIcon={<ExpandMoreIcon />} sx={{ px: 0 }}>
                <Stack direction='row' spacing={2} alignItems='center' sx={{ minWidth: 0, width: '100%' }}>
                    <Avatar variant='rounded' src={user.avatarUrl} sx={{ width: 36, height: 36 }} />
                    <Box sx={{ minWidth: 0, flex: 1 }}>
                        <Stack direction='row' spacing={0.5} alignItems='center'>
                            <Typography variant='subtitle2' noWrap>
                                {user.loginName}
                            </Typography>
                            {isCurrentUser && (
                                <Chip
                                    label='you'
                                    size='small'
                                    color='info'
                                    variant='outlined'
                                    sx={{ height: 18, '& .MuiChip-label': { px: 0.5, fontSize: '0.65rem' } }}
                                />
                            )}
                        </Stack>
                        <Typography variant='caption' color='text.secondary' noWrap>
                            {user.fullName || '—'}
                        </Typography>
                    </Box>
                    <Stack direction='row' spacing={0.5} sx={{ flexShrink: 0 }}>
                        <Chip
                            icon={providerIcon(user.provider)}
                            label={user.provider ?? '—'}
                            size='small'
                            variant='outlined'
                        />
                        <Chip
                            icon={roleIcon(user.role)}
                            label={user.role || 'none'}
                            size='small'
                            color={user.role ? 'primary' : 'default'}
                            variant={user.role ? 'filled' : 'outlined'}
                        />
                    </Stack>
                </Stack>
            </AccordionSummary>
            <AccordionDetails sx={{ pt: 0, px: 0 }}>
                {updateRole.isError && (
                    <Alert severity='error' sx={{ mb: 2 }} onClose={() => updateRole.reset()}>
                        {formatError(updateRole.error as Error | Partial<ErrorResponse>)}
                    </Alert>
                )}
                <Box sx={{ display: 'flex', gap: 3, flexWrap: 'wrap', alignItems: 'flex-start' }}>
                    <RoleEditor
                        roleDraft={roleDraft}
                        onChange={setRoleDraft}
                        onSave={handleSaveRole}
                        saving={updateRole.isPending}
                    />
                    {entry.namespaces.length > 0 && (
                        <Stack spacing={1} sx={{ minWidth: 150 }}>
                            <Stack direction='row' spacing={0.5} alignItems='center'>
                                <FolderSharedIcon fontSize='small' color='action' />
                                <Typography variant='subtitle2'>Namespaces</Typography>
                            </Stack>
                            <Stack direction='row' spacing={0.5} sx={{ flexWrap: 'wrap', gap: 0.5 }}>
                                {entry.namespaces.map(ns => (
                                    <Chip
                                        key={ns.name}
                                        label={ns.name}
                                        size='small'
                                        component={RouterLink}
                                        to={`${AdminDashboardRoutes.NAMESPACE_ADMIN}/${encodeURIComponent(ns.name)}`}
                                        clickable
                                    />
                                ))}
                            </Stack>
                        </Stack>
                    )}
                    {entry.customers.length > 0 && (
                        <Stack spacing={1} sx={{ minWidth: 150 }}>
                            <Stack direction='row' spacing={0.5} alignItems='center'>
                                <BusinessIcon fontSize='small' color='action' />
                                <Typography variant='subtitle2'>Customers</Typography>
                            </Stack>
                            <Stack spacing={0.5}>
                                {entry.customers.map(cust => {
                                    const tier = cust?.tier;
                                    return (
                                        <Stack key={cust.name} direction='row' spacing={0.5} alignItems='center'>
                                            <Chip
                                                label={cust.name}
                                                size='small'
                                                component={RouterLink}
                                                to={`${AdminDashboardRoutes.CUSTOMERS}/${encodeURIComponent(cust.name)}`}
                                                clickable
                                            />
                                            {tier && (
                                                <Tooltip
                                                    title={`${tier.capacity} req / ${tier.duration}s (${tier.refillStrategy.toLowerCase()})`}>
                                                    <Chip
                                                        label={tier.name}
                                                        size='small'
                                                        variant='outlined'
                                                        color={tier.tierType === 'FREE' ? 'default' : 'primary'}
                                                    />
                                                </Tooltip>
                                            )}
                                            {cust && !tier && (
                                                <Typography variant='caption' color='text.secondary'>
                                                    no tier
                                                </Typography>
                                            )}
                                        </Stack>
                                    );
                                })}
                            </Stack>
                        </Stack>
                    )}
                </Box>
                <PublisherDetailsSection provider={user.provider} loginName={user.loginName} />
            </AccordionDetails>
        </Accordion>
    );
};

export const PublisherAdmin: FunctionComponent = () => {
    const { publisher: publisherParam } = useParams<{ publisher?: string }>();
    const navigate = useNavigate();
    const queryClient = useQueryClient();

    const [searchText, setSearchText] = useState(publisherParam ?? '');
    const [roleFilter, setRoleFilter] = useState('');
    const [filterOpen, setFilterOpen] = useState(false);

    const debouncedSetSearch = useDebouncedCallback(setSearchText);

    const { data, isFetching, isFetchingNextPage, error, hasNextPage, fetchNextPage } = useInfinitePublishers(
        searchText,
        roleFilter
    );

    const publishers = useMemo(() => data?.pages.flatMap(page => page.content) ?? [], [data]);
    const totalSize = data?.pages[0]?.page.totalElements ?? 0;
    // Top progress bar tracks search/filter loads; pagination has its own spinner.
    const listLoading = isFetching && !isFetchingNextPage;

    const filterButtonRef = useRef<HTMLButtonElement>(null);
    const scrollParentRef = useRef<HTMLElement | null>(null);
    const containerRef = useRef<HTMLDivElement>(null);

    const expandedPublisherKey = useMemo(() => {
        if (!publisherParam) return undefined;
        const matched = publishers.find(p => p.user.loginName === publisherParam);
        return matched ? getPublisherKey(matched) : undefined;
    }, [publisherParam, publishers]);

    const updateContextValue = useMemo(
        () => ({
            handleUpdate: () => {
                queryClient.invalidateQueries({ queryKey: ['admin', 'publisher'] });
                queryClient.invalidateQueries({ queryKey: ['admin', 'publishers'] });
            }
        }),
        [queryClient]
    );

    const getScrollParent = () => {
        scrollParentRef.current ??= findScrollableAncestor(containerRef.current?.parentElement ?? null);
        return scrollParentRef.current;
    };

    const loadMorePublishers = () => {
        if (hasNextPage && !isFetchingNextPage) {
            void fetchNextPage();
        }
    };

    const handleAccordionToggle = (loginName: string, isExpanded: boolean) => {
        const target = isExpanded
            ? `${AdminDashboardRoutes.PUBLISHER_ADMIN}/${encodeURIComponent(loginName)}`
            : AdminDashboardRoutes.PUBLISHER_ADMIN;
        navigate(target, { replace: true });
    };

    return (
        <UpdateContext.Provider value={updateContextValue}>
            <Box ref={containerRef}>
                <SearchListContainer
                    searchContainer={[
                        <StyledInput
                            key='publisher-admin-search'
                            placeholder='Search by login or display name...'
                            value={searchText}
                            onChange={debouncedSetSearch}
                        />
                    ]}
                    listContainer={null}
                    loading={listLoading}
                />
                <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                    <Stack direction='row' alignItems='center' justifyContent='space-between'>
                        <IconButton
                            ref={filterButtonRef}
                            onClick={() => setFilterOpen(true)}
                            size='small'
                            title='Filters'>
                            <Badge color='primary' variant='dot' invisible={!roleFilter}>
                                <FilterListIcon />
                            </Badge>
                        </IconButton>
                        {totalSize > 0 && (
                            <Typography variant='body2' color='text.secondary'>
                                {totalSize} publisher{totalSize === 1 ? '' : 's'} found
                            </Typography>
                        )}
                    </Stack>

                    <FiltersPopover
                        anchorEl={filterOpen ? filterButtonRef.current : null}
                        onClose={() => setFilterOpen(false)}
                        roleFilter={roleFilter}
                        onRoleChange={setRoleFilter}
                    />

                    {error && <Alert severity='error'>{formatError(error as Error | Partial<ErrorResponse>)}</Alert>}

                    {!isFetching && !error && publishers.length === 0 && (
                        <Paper elevation={0} sx={{ p: 3, textAlign: 'center' }}>
                            <Typography color='text.secondary'>No publishers matched the current filters.</Typography>
                        </Paper>
                    )}

                    {publishers.length > 0 && (
                        <InfiniteScroll
                            loadMore={loadMorePublishers}
                            hasMore={Boolean(hasNextPage)}
                            threshold={200}
                            useWindow={false}
                            getScrollParent={getScrollParent}
                            initialLoad={false}
                            loader={
                                <Box key='loader' sx={{ display: 'flex', justifyContent: 'center', py: 2 }}>
                                    <CircularProgress size={24} />
                                </Box>
                            }>
                            <Paper elevation={0} sx={{ overflow: 'hidden' }}>
                                {publishers.map((entry, index) => (
                                    <PublisherListItem
                                        key={getPublisherKey(entry)}
                                        entry={entry}
                                        index={index}
                                        expanded={expandedPublisherKey === getPublisherKey(entry)}
                                        onToggle={handleAccordionToggle}
                                    />
                                ))}
                            </Paper>
                        </InfiniteScroll>
                    )}
                </Box>
            </Box>
        </UpdateContext.Provider>
    );
};
