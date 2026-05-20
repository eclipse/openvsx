/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import { FunctionComponent, createContext, useCallback, useContext, useEffect, useRef, useState } from 'react';
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
    Typography,
} from '@mui/material';
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
import { AdminUser as UserRelationships, isError, PublisherInfo } from '../../extension-registry-types';
import { ErrorResponse } from '../../server-request';
import { ExtensionRegistryService } from '../../extension-registry-service';
import { MainContext } from '../../context';
import { PublisherDetails } from './publisher-details';
import { StyledInput } from './namespace-input';
import { SearchListContainer } from './search-list-container';
import { handleError as formatError } from '../../utils';
import { AdminDashboardRoutes } from './admin-dashboard-routes';

// eslint-disable-next-line react-refresh/only-export-components
export const UpdateContext = createContext({ handleUpdate: () => { } });

const DEBOUNCE_MS = 300;
const PAGE_SIZE = 25;

const ROLE_FILTER_OPTIONS = [
    { value: '', label: 'Any role' },
    { value: 'admin', label: 'Admin' },
    { value: 'privileged', label: 'Privileged' },
    { value: 'none', label: 'No role' },
];

type Role = 'admin' | 'privileged' | 'none';

const ROLE_EDITOR_OPTIONS: { value: Role; label: string }[] = [
    { value: 'none', label: 'No role' },
    { value: 'admin', label: 'Admin' },
    { value: 'privileged', label: 'Privileged' },
];

type ReportError = (err: Error | Partial<ErrorResponse>) => void;

const getUserKey = (entry: UserRelationships) => `${entry.user.provider}/${entry.user.loginName}`;

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

function usePublisherDetail(
    entry: UserRelationships | undefined,
    service: ExtensionRegistryService,
    reportError: ReportError,
    onRoleMutate: (provider: string, loginName: string, newRole: Role) => string | undefined,
    onRoleRollback: (provider: string, loginName: string, previousRole: string | undefined) => void,
) {
    const [publisherInfo, setPublisherInfo] = useState<PublisherInfo | undefined>();
    const [publisherLoading, setPublisherLoading] = useState(false);
    const [publisherError, setPublisherError] = useState<string | null>(null);
    const [roleDraft, setRoleDraft] = useState<Role>('none');
    const [savingRole, setSavingRole] = useState(false);

    const entryProvider = entry?.user.provider;
    const entryLoginName = entry?.user.loginName ?? '';
    const entryKey = entry ? `${entryProvider}/${entryLoginName}` : '';

    useEffect(() => {
        if (!entry) {
            setPublisherInfo(undefined);
            setPublisherError(null);
            setRoleDraft('none');
            return;
        }

        setRoleDraft((entry.user.role as 'admin' | 'privileged') ?? 'none');
        const abortController = new AbortController();

        const load = async () => {
            try {
                setPublisherLoading(true);
                setPublisherError(null);
                const info = await service.admin.getPublisherInfo(abortController, entryProvider!, entryLoginName);
                setPublisherInfo(info);
            } catch (err) {
                if (!abortController.signal.aborted) {
                    reportError(err as Error | Partial<ErrorResponse>);
                    setPublisherError(formatError(err as Error | Partial<ErrorResponse>));
                    setPublisherInfo(undefined);
                }
            } finally {
                if (!abortController.signal.aborted) setPublisherLoading(false);
            }
        };

        void load();
        return () => abortController.abort();
    }, [entryKey, service, reportError]);

    const handleRoleSave = useCallback(async () => {
        if (!entry || roleDraft === (entry.user.role ?? 'none')) return;

        const provider = entry.user.provider;
        if (!provider) return;
        const loginName = entry.user.loginName;
        const previousRole = onRoleMutate(provider, loginName, roleDraft);

        try {
            setSavingRole(true);
            const result = await service.admin.updateUserRole(new AbortController(), provider, loginName, roleDraft);
            if (isError(result)) throw result;
        } catch (err) {
            onRoleRollback(provider, loginName, previousRole);
            setRoleDraft((previousRole as 'admin' | 'privileged') ?? 'none');
            reportError(err as Error | Partial<ErrorResponse>);
            setPublisherError(formatError(err as Error | Partial<ErrorResponse>));
        } finally {
            setSavingRole(false);
        }
    }, [entry, roleDraft, service, reportError, onRoleMutate, onRoleRollback]);

    return {
        publisherInfo,
        publisherLoading,
        publisherError,
        roleDraft,
        setRoleDraft,
        saveRole: useCallback(() => {
 void handleRoleSave();
}, [handleRoleSave]),
        savingRole,
        clearError: useCallback(() => setPublisherError(null), []),
    };
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
        transformOrigin={{ vertical: 'top', horizontal: 'left' }}
    >
        <Box sx={{ p: 2, width: 220 }}>
            <Typography variant='subtitle2' sx={{ mb: 1.5 }}>Filters</Typography>
            <FormControl size='small' fullWidth>
                <InputLabel id='role-filter-label'>Role</InputLabel>
                <Select
                    labelId='role-filter-label'
                    value={roleFilter}
                    label='Role'
                    onChange={e => onRoleChange(e.target.value)}
                >
                    {ROLE_FILTER_OPTIONS.map(o => <MenuItem key={o.value} value={o.value}>{o.label}</MenuItem>)}
                </Select>
            </FormControl>
        </Box>
    </Popover>
);

const RoleEditor: FunctionComponent<{
    roleDraft: Role;
    onChange: (role: Role) => void;
    onSave: () => void;
    saving: boolean;
}> = ({ roleDraft, onChange, onSave, saving }) => (
    <Stack spacing={1} sx={{ minWidth: 200 }}>
        <Typography variant='subtitle2'>Role</Typography>
        <Stack direction='row' spacing={1} alignItems='center'>
            <FormControl size='small' sx={{ minWidth: 140 }}>
                <Select value={roleDraft} onChange={e => onChange(e.target.value as Role)} displayEmpty>
                    {ROLE_EDITOR_OPTIONS.map(o => <MenuItem key={o.value} value={o.value}>{o.label}</MenuItem>)}
                </Select>
            </FormControl>
            <ButtonWithProgress working={saving} onClick={onSave} title='Save role'>
                Save
            </ButtonWithProgress>
        </Stack>
    </Stack>
);

interface UserListItemProps {
    entry: UserRelationships;
    index: number;
    expanded: boolean;
    onToggle: (userKey: string, loginName: string, isExpanded: boolean) => void;
    onLoadingChange: (loading: boolean) => void;
    onRoleMutate: (provider: string, loginName: string, newRole: Role) => string | undefined;
    onRoleRollback: (provider: string, loginName: string, previousRole: string | undefined) => void;
}

const UserListItem: FunctionComponent<UserListItemProps> = ({
    entry, index, expanded, onToggle, onLoadingChange, onRoleMutate, onRoleRollback,
}) => {
    const { user } = entry;
    const userKey = getUserKey(entry);
    const { service, handleError: reportError, user: currentUser } = useContext(MainContext);
    const [pendingExpand, setPendingExpand] = useState(false);
    const isCurrentUser = currentUser?.loginName === user.loginName
        && currentUser?.provider === user.provider;

    const {
        publisherInfo, publisherLoading, publisherError,
        roleDraft, setRoleDraft, saveRole, savingRole,
        clearError,
    } = usePublisherDetail(
        expanded || pendingExpand ? entry : undefined,
        service, reportError, onRoleMutate, onRoleRollback,
    );

    useEffect(() => {
 onLoadingChange(pendingExpand);
}, [pendingExpand, onLoadingChange]);

    useEffect(() => {
        if (pendingExpand && !publisherLoading && (publisherInfo || publisherError)) {
            setPendingExpand(false);
            onToggle(userKey, user.loginName, true);
        }
    }, [pendingExpand, publisherLoading, publisherInfo, publisherError, onToggle, userKey, user.loginName]);

    const handleChange = useCallback((_: unknown, isExpanded: boolean) => {
        if (isExpanded) {
            if (publisherInfo) {
                onToggle(userKey, user.loginName, true);
            } else {
                setPendingExpand(true);
            }
        } else {
            setPendingExpand(false);
            onToggle(userKey, user.loginName, false);
        }
    }, [publisherInfo, onToggle, userKey, user.loginName]);

    return (
        <Accordion
            expanded={expanded}
            onChange={handleChange}
            disableGutters
            elevation={0}
            slotProps={{ transition: { unmountOnExit: true } }}
            sx={{ '&:before': { display: 'none' }, borderTop: index === 0 ? 'none' : 1, borderColor: 'divider' }}
        >
            <AccordionSummary expandIcon={<ExpandMoreIcon />} sx={{ px: 0 }}>
                <Stack direction='row' spacing={2} alignItems='center' sx={{ minWidth: 0, width: '100%' }}>
                    <Avatar variant='rounded' src={user.avatarUrl} sx={{ width: 36, height: 36 }} />
                    <Box sx={{ minWidth: 0, flex: 1 }}>
                        <Stack direction='row' spacing={0.5} alignItems='center'>
                            <Typography variant='subtitle2' noWrap>{user.loginName}</Typography>
                            {isCurrentUser && (
                                <Chip label='you' size='small' color='info' variant='outlined'
                                    sx={{ height: 18, '& .MuiChip-label': { px: 0.5, fontSize: '0.65rem' } }} />
                            )}
                        </Stack>
                        <Typography variant='caption' color='text.secondary' noWrap>
                            {user.fullName || '—'}
                        </Typography>
                    </Box>
                    <Stack direction='row' spacing={0.5} sx={{ flexShrink: 0 }}>
                        <Chip icon={providerIcon(user.provider)} label={user.provider ?? '—'} size='small' variant='outlined' />
                        <Chip icon={roleIcon(user.role)} label={user.role || 'none'} size='small'
                            color={user.role ? 'primary' : 'default'} variant={user.role ? 'filled' : 'outlined'} />
                    </Stack>
                </Stack>
            </AccordionSummary>
            <AccordionDetails sx={{ pt: 0, px: 0 }}>
                {publisherError && <Alert severity='error' sx={{ mb: 2 }} onClose={clearError}>{publisherError}</Alert>}
                {publisherInfo && (
                    <>
                        <Box sx={{ display: 'flex', gap: 3, flexWrap: 'wrap', alignItems: 'flex-start' }}>
                            <RoleEditor roleDraft={roleDraft} onChange={setRoleDraft} onSave={saveRole} saving={savingRole} />
                            {entry.namespaces.length > 0 && (
                                <Stack spacing={1} sx={{ minWidth: 150 }}>
                                    <Stack direction='row' spacing={0.5} alignItems='center'>
                                        <FolderSharedIcon fontSize='small' color='action' />
                                        <Typography variant='subtitle2'>Namespaces</Typography>
                                    </Stack>
                                    <Stack direction='row' spacing={0.5} sx={{ flexWrap: 'wrap', gap: 0.5 }}>
                                        {entry.namespaces.map(ns => (
                                            <Chip key={ns.name} label={ns.name} size='small'
                                                component={RouterLink}
                                                to={`${AdminDashboardRoutes.NAMESPACE_ADMIN}/${encodeURIComponent(ns.name)}`}
                                                clickable />
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
                                                    <Chip label={cust.name} size='small'
                                                        component={RouterLink}
                                                        to={`${AdminDashboardRoutes.CUSTOMERS}/${encodeURIComponent(cust.name)}`}
                                                        clickable />
                                                    {tier && (
                                                        <Tooltip title={`${tier.capacity} req / ${tier.duration}s (${tier.refillStrategy.toLowerCase()})`}>
                                                            <Chip label={tier.name} size='small' variant='outlined'
                                                                color={tier.tierType === 'FREE' ? 'default' : 'primary'} />
                                                        </Tooltip>
                                                    )}
                                                    {cust && !tier && (
                                                        <Typography variant='caption' color='text.secondary'>no tier</Typography>
                                                    )}
                                                </Stack>
                                            );
                                        })}
                                    </Stack>
                                </Stack>
                            )}
                        </Box>
                        <Divider sx={{ my: 2 }} />
                        <PublisherDetails publisherInfo={publisherInfo} />
                    </>
                )}
            </AccordionDetails>
        </Accordion>
    );
};

export const PublisherAdmin: FunctionComponent = () => {
    const { publisher: publisherParam } = useParams<{ publisher?: string }>();
    const { service, handleError: reportError } = useContext(MainContext);
    const navigate = useNavigate();

    const initialSearch = publisherParam ?? '';
    const [users, setUsers] = useState<UserRelationships[]>([]);
    const [totalSize, setTotalSize] = useState(0);
    const [usersLoading, setUsersLoading] = useState(true);
    const [usersError, setUsersError] = useState<string | null>(null);
    const [searchText, setSearchText] = useState(initialSearch);
    const [debouncedSearch, setDebouncedSearch] = useState(initialSearch);
    const [roleFilter, setRoleFilter] = useState('');
    const [hasMore, setHasMore] = useState(false);
    const [fetchCounter, setFetchCounter] = useState(0);
    const [expandedUserKey, setExpandedUserKey] = useState<string | undefined>();
    const [filterOpen, setFilterOpen] = useState(false);
    const [detailLoading, setDetailLoading] = useState(false);

    const debounceTimer = useRef<number>();
    const filterButtonRef = useRef<HTMLButtonElement>(null);
    const initialParamHandled = useRef(false);
    const pageRef = useRef(0);
    const enableLoadMore = useRef(false);
    const scrollParentRef = useRef<HTMLElement | null>(null);
    const containerRef = useRef<HTMLDivElement>(null);

    const getScrollParent = useCallback(() => {
        scrollParentRef.current ??= findScrollableAncestor(containerRef.current?.parentElement ?? null);
        return scrollParentRef.current;
    }, []);

    const onSearchChange = useCallback((value: string) => {
        setSearchText(value);
        clearTimeout(debounceTimer.current);
        debounceTimer.current = globalThis.setTimeout(() => setDebouncedSearch(value), DEBOUNCE_MS);
    }, []);

    const onSearchSubmit = useCallback((value: string) => {
        setSearchText(value);
        clearTimeout(debounceTimer.current);
        setDebouncedSearch(value);
    }, []);

    const handleAccordionToggle = useCallback((userKey: string, loginName: string, isExpanded: boolean) => {
        if (isExpanded) {
            setExpandedUserKey(userKey);
            navigate(`${AdminDashboardRoutes.PUBLISHER_ADMIN}/${encodeURIComponent(loginName)}`, { replace: true });
        } else {
            setExpandedUserKey(undefined);
            navigate(AdminDashboardRoutes.PUBLISHER_ADMIN, { replace: true });
        }
    }, [navigate]);

    const handleDetailLoadingChange = useCallback((loading: boolean) => setDetailLoading(loading), []);

    const refresh = useCallback(() => setFetchCounter(c => c + 1), []);
    const updateContextValue = useCallback(() => ({ handleUpdate: refresh }), [refresh]);

    const handleRoleMutate = useCallback((provider: string, loginName: string, newRole: Role): string | undefined => {
        let previousRole: string | undefined;
        setUsers(prev => prev.map(u => {
            if (u.user.provider === provider && u.user.loginName === loginName) {
                previousRole = u.user.role;
                return { ...u, user: { ...u.user, role: newRole === 'none' ? undefined : newRole } };
            }
            return u;
        }));
        return previousRole;
    }, []);

    const handleRoleRollback = useCallback((provider: string, loginName: string, previousRole: string | undefined) => {
        setUsers(prev => prev.map(u =>
            u.user.provider === provider && u.user.loginName === loginName
                ? { ...u, user: { ...u.user, role: previousRole } }
                : u
        ));
    }, []);

    useEffect(() => () => clearTimeout(debounceTimer.current), []);

    useEffect(() => {
        const abortController = new AbortController();
        enableLoadMore.current = false;

        const fetchUsers = async () => {
            try {
                setUsersLoading(true);
                setUsersError(null);
                setHasMore(false);
                pageRef.current = 0;
                const data = await service.admin.getUsers(abortController, {
                    search: debouncedSearch || undefined,
                    role: roleFilter || undefined,
                    size: PAGE_SIZE,
                    page: 0,
                });
                const content = data.content ?? [];
                const total = data.page?.totalElements ?? 0;
                setUsers(content);
                setTotalSize(total);
                setHasMore(content.length < total);
                enableLoadMore.current = true;
            } catch (err) {
                if (!abortController.signal.aborted) {
                    reportError(err as Error | Partial<ErrorResponse>);
                    setUsersError(formatError(err as Error | Partial<ErrorResponse>));
                }
            } finally {
                if (!abortController.signal.aborted) setUsersLoading(false);
            }
        };

        void fetchUsers();
        return () => abortController.abort();
    }, [debouncedSearch, roleFilter, fetchCounter, service, reportError]);

    const loadMore = useCallback(async () => {
        if (!enableLoadMore.current) return;
        enableLoadMore.current = false;
        const nextPage = ++pageRef.current;

        try {
            const data = await service.admin.getUsers(new AbortController(), {
                search: debouncedSearch || undefined,
                role: roleFilter || undefined,
                size: PAGE_SIZE,
                page: nextPage,
            });
            const content = data.content ?? [];
            const total = data.page?.totalElements ?? 0;
            setUsers(prev => {
                const updated = [...prev, ...content];
                setHasMore(updated.length < total && content.length > 0);
                return updated;
            });
            setTotalSize(total);
        } catch (err) {
            reportError(err as Error | Partial<ErrorResponse>);
            setUsersError(formatError(err as Error | Partial<ErrorResponse>));
        } finally {
            enableLoadMore.current = true;
        }
    }, [debouncedSearch, roleFilter, service, reportError]);

    // Collapse when expanded user leaves the result set
    useEffect(() => {
        if (expandedUserKey && !users.some(e => getUserKey(e) === expandedUserKey)) {
            setExpandedUserKey(undefined);
            navigate(AdminDashboardRoutes.PUBLISHER_ADMIN, { replace: true });
        }
    }, [expandedUserKey, users, navigate]);

    // Auto-expand user from URL param on initial load
    useEffect(() => {
        if (!publisherParam || initialParamHandled.current) return;
        const matched = users.find(e => e.user.loginName === publisherParam);
        if (matched) {
            setExpandedUserKey(getUserKey(matched));
            initialParamHandled.current = true;
        }
    }, [publisherParam, users]);

    return (
        <UpdateContext.Provider value={updateContextValue()}>
            <Box ref={containerRef}>
                <SearchListContainer
                    searchContainer={[
                        <StyledInput
                            key='publisher-admin-search'
                            placeholder='Search by login or display name...'
                            value={searchText}
                            onSubmit={onSearchSubmit}
                            onChange={onSearchChange}
                        />
                    ]}
                    listContainer={null}
                    loading={usersLoading || detailLoading}
                />
                <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                    <Stack direction='row' alignItems='center' justifyContent='space-between'>
                        <IconButton ref={filterButtonRef} onClick={() => setFilterOpen(true)} size='small' title='Filters'>
                            <Badge color='primary' variant='dot' invisible={!roleFilter}>
                                <FilterListIcon />
                            </Badge>
                        </IconButton>
                        {totalSize > 0 && (
                            <Typography variant='body2' color='text.secondary'>
                                {totalSize} user{totalSize === 1 ? '' : 's'} found
                            </Typography>
                        )}
                    </Stack>

                    <FiltersPopover
                        anchorEl={filterOpen ? filterButtonRef.current : null}
                        onClose={() => setFilterOpen(false)}
                        roleFilter={roleFilter}
                        onRoleChange={setRoleFilter}
                    />

                    {usersError && <Alert severity='error' onClose={() => setUsersError(null)}>{usersError}</Alert>}

                    {!usersLoading && !usersError && users.length === 0 && (
                        <Paper elevation={0} sx={{ p: 3, textAlign: 'center' }}>
                            <Typography color='text.secondary'>No users matched the current filters.</Typography>
                        </Paper>
                    )}

                    {!usersLoading && users.length > 0 && (
                        <InfiniteScroll
                            loadMore={loadMore}
                            hasMore={hasMore}
                            threshold={200}
                            useWindow={false}
                            getScrollParent={getScrollParent}
                            initialLoad={false}
                            loader={
                                <Box key='loader' sx={{ display: 'flex', justifyContent: 'center', py: 2 }}>
                                    <CircularProgress size={24} />
                                </Box>
                            }
                        >
                            <Paper elevation={0} sx={{ overflow: 'hidden' }}>
                                {users.map((entry, index) => (
                                    <UserListItem
                                        key={getUserKey(entry)}
                                        entry={entry}
                                        index={index}
                                        expanded={expandedUserKey === getUserKey(entry)}
                                        onToggle={handleAccordionToggle}
                                        onLoadingChange={handleDetailLoadingChange}
                                        onRoleMutate={handleRoleMutate}
                                        onRoleRollback={handleRoleRollback}
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
