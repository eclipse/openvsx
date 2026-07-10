/********************************************************************************
 * Copyright (c) 2019 TypeFox and others
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

import { FunctionComponent, useContext, useEffect, useRef, useState } from 'react';
import InfiniteScroll from 'react-infinite-scroller';
import { Box } from '@mui/material';
import { ExtensionCard, ExtensionCardSkeleton } from './extension-card';
import { isError, SearchEntry, SearchResult } from '../extension-registry-types';
import { ExtensionFilter } from '../extension-registry-service';
import { useExtensionResultsCursor } from '../hooks/use-extension-results-cursor';
import { MainContext } from '../context';

export const ExtensionList: FunctionComponent<ExtensionListProps> = props => {
    const abortController = useRef<AbortController>(new AbortController());
    const enableLoadMore = useRef(false);
    const lastRequestedPage = useRef(0);
    const pageOffset = useRef(0);
    const filterSize = useRef(props.filter.size ?? 10);
    const context = useContext(MainContext);
    const [extensions, setExtensions] = useState<SearchEntry[]>([]);
    const [extensionKeys, setExtensionKeys] = useState<Set<string>>(new Set<string>());
    const [appliedFilter, setAppliedFilter] = useState<ExtensionFilter>();
    const [hasMore, setHasMore] = useState<boolean>(false);
    const [loading, setLoading] = useState<boolean>(true);
    const grid = useExtensionResultsCursor(extensions.length);

    useEffect(() => {
        enableLoadMore.current = true;
        return () => {
            abortController.current.abort();
            enableLoadMore.current = false;
        };
    }, []);

    useEffect(() => {
        filterSize.current = props.filter.size ?? filterSize.current;
        // Inputs are already debounced upstream; fetch immediately and drop
        // responses that arrive after the filter changed again.
        let stale = false;
        (async () => {
            try {
                const result = await context.service.search(abortController.current, props.filter);
                if (isError(result)) {
                    throw result;
                }
                if (stale) {
                    return;
                }

                const searchResult = result as SearchResult;
                props.onUpdate(searchResult.totalSize);
                const actualSize = searchResult.extensions.length;
                pageOffset.current = lastRequestedPage.current;
                const extensionKeys = new Set<string>();
                for (const ext of searchResult.extensions) {
                    extensionKeys.add(`${ext.namespace}.${ext.name}`);
                }

                setExtensions(searchResult.extensions);
                setExtensionKeys(extensionKeys);
                setAppliedFilter(props.filter);
                setHasMore(actualSize < searchResult.totalSize && actualSize > 0);
                // Fresh result set: cursor back to the first card, so Enter in
                // the search field opens the first result of the new query.
                grid.reset();
            } catch (err) {
                if (!stale) {
                    context.handleError(err);
                }
            } finally {
                if (!stale) {
                    setLoading(false);
                }
            }
        })();
        return () => {
            stale = true;
        };
    }, [props.filter.category, props.filter.query, props.filter.sortBy, props.filter.sortOrder]);

    const loadMore = async (p: number): Promise<void> => {
        setLoading(true);
        setHasMore(false);
        lastRequestedPage.current = p;
        const filter = copyFilter(appliedFilter as ExtensionFilter);
        if (!isSameFilter(props.filter, filter)) {
            return;
        }
        try {
            filter.offset = (p - pageOffset.current) * filterSize.current;
            const result = await context.service.search(abortController.current, filter);
            if (isError(result)) {
                throw result;
            }

            const newExtensions: SearchEntry[] = [];
            const newExtensionKeys = new Set<string>();
            newExtensions.push(...extensions);
            extensionKeys.forEach(key => newExtensionKeys.add(key));
            const searchResult = result as SearchResult;
            if (enableLoadMore.current && isSameFilter(props.filter, filter)) {
                for (const ext of searchResult.extensions) {
                    const key = `${ext.namespace}.${ext.name}`;
                    if (!extensionKeys.has(key)) {
                        newExtensions.push(ext);
                        newExtensionKeys.add(key);
                    }
                }

                setExtensions(newExtensions);
                setExtensionKeys(newExtensionKeys);
                setHasMore(extensions.length < searchResult.totalSize && searchResult.extensions.length > 0);
            }
        } catch (err) {
            context.handleError(err);
        } finally {
            setLoading(false);
        }
    };

    const isSameFilter = (f1: ExtensionFilter, f2: ExtensionFilter): boolean => {
        return (
            f1.category === f2.category &&
            f1.query === f2.query &&
            f1.sortBy === f2.sortBy &&
            f1.sortOrder === f2.sortOrder
        );
    };

    const copyFilter = (f: ExtensionFilter): ExtensionFilter => {
        return {
            query: f.query,
            category: f.category || '',
            size: f.size,
            offset: f.offset,
            sortBy: f.sortBy,
            sortOrder: f.sortOrder
        };
    };

    const extensionList = extensions.map((ext, idx) => (
        <ExtensionCard
            extension={ext}
            fadeDelayMs={(idx % filterSize.current) * 200}
            key={`${ext.namespace}.${ext.name}`}
            {...grid.itemProps(idx)}
        />
    ));

    // Placeholders for the page being fetched reserve its space up front, so the
    // grid reaches its final height at once and the footer doesn't jump when the
    // real cards swap in (they share the skeleton's footprint).
    const skeletons = loading
        ? Array.from({ length: filterSize.current }, (_, idx) => <ExtensionCardSkeleton key={`skeleton-${idx}`} />)
        : null;

    return (
        <InfiniteScroll loadMore={loadMore} hasMore={hasMore} threshold={200}>
            <Box
                {...grid.containerProps}
                sx={{
                    display: 'grid',
                    gridTemplateColumns: {
                        xs: 'repeat(2, minmax(0, 1fr))',
                        sm: 'repeat(auto-fill, minmax(175px, 1fr))'
                    },
                    gap: '1rem'
                }}>
                {extensionList}
                {skeletons}
            </Box>
        </InfiniteScroll>
    );
};

export interface ExtensionListProps {
    filter: ExtensionFilter;
    onUpdate: (resultNumber: number) => void;
}
