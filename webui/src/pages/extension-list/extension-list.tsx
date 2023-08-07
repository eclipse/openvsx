/********************************************************************************
 * Copyright (c) 2019 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import React, { FunctionComponent, useContext, useEffect, useRef, useState } from 'react';
import * as InfiniteScroll from 'react-infinite-scroller';
import { Box, Grid, CircularProgress, Container } from '@mui/material';
import { ExtensionListItem } from './extension-list-item';
import { isError, SearchEntry, SearchResult } from '../../extension-registry-types';
import { ExtensionFilter } from '../../extension-registry-service';
import { debounce } from '../../utils';
import { DelayedLoadIndicator } from '../../components/delayed-load-indicator';
import { MainContext } from '../../context';

export const ExtensionList: FunctionComponent<ExtensionListProps> = props => {
    const abortController = new AbortController();
    const cancellationToken: { timeout?: number } = {};
    const enableLoadMore = useRef(false);
    const lastRequestedPage = useRef(0);
    const pageOffset = useRef(0);
    const filterSize = useRef(props.filter.size || 10);
    const context = useContext(MainContext);
    const [extensions, setExtensions] = useState<SearchEntry[]>([]);
    const [extensionKeys, setExtensionKeys] = useState<Set<string>>(new Set<string>());
    const [appliedFilter, setAppliedFilter] = useState<ExtensionFilter>();
    const [hasMore, setHasMore] = useState<boolean>(false);
    const [loading, setLoading] = useState<boolean>(true);

    useEffect(() => {
        enableLoadMore.current = true;
        return () => {
            abortController.abort();
            clearTimeout(cancellationToken.timeout);
            enableLoadMore.current = false;
        };
    }, []);

    useEffect(() => {
        filterSize.current = props.filter.size || filterSize.current;
        debounce(
            async () => {
                try {
                    const result = await context.service.search(abortController, props.filter);
                    if (isError(result)) {
                        throw result;
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
                } catch (err) {
                    context.handleError(err);
                } finally {
                    setLoading(false);
                }
            },
            cancellationToken,
            props.debounceTime
        );
    }, [props.filter.category, props.filter.query, props.filter.sortBy, props.filter.sortOrder, props.debounceTime]);

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
            const result = await context.service.search(abortController, filter);
            if (isError(result)) {
                throw result;
            }

            const newExtensions: SearchEntry[] = [];
            const newExtensionKeys = new Set<string>();
            newExtensions.push(...extensions);
            extensionKeys.forEach((key) => newExtensionKeys.add(key));
            const searchResult = result as SearchResult;
            if (enableLoadMore.current && isSameFilter(props.filter, filter)) {
                // Check for duplicate keys to avoid problems due to asynchronous user edit / loadMore call
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
        return f1.category === f2.category && f1.query === f2.query && f1.sortBy === f2.sortBy && f1.sortOrder === f2.sortOrder;
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
        <ExtensionListItem
            idx={idx}
            extension={ext}
            filterSize={filterSize.current}
            key={`${ext.namespace}.${ext.name}`} />
    ));

    const loader = <Box component='div' key='extension-list-loader' sx={{ display: 'flex', justifyContent: 'center', m: 3 }}>
        <CircularProgress size='3rem' color='secondary' />
    </Box>;

    return <>
        <DelayedLoadIndicator loading={loading}/>
        <InfiniteScroll
            loadMore={loadMore}
            hasMore={hasMore}
            loader={loader}
            threshold={200} >
            <Container maxWidth='xl'>
                <Grid container spacing={2} sx={{ justifyContent: 'center' }}>
                    {extensionList}
                </Grid>
            </Container>
        </InfiniteScroll>
    </>;
};

export interface ExtensionListProps {
    filter: ExtensionFilter;
    debounceTime: number;
    onUpdate: (resultNumber: number) => void;
}