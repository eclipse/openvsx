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

import { FunctionComponent, useContext, useEffect, useMemo, useRef } from 'react';
import InfiniteScroll from 'react-infinite-scroller';
import { Box } from '@mui/material';
import { ExtensionCard } from './extension-card';
import { ExtensionFilter } from '../extension-registry-service';
import { useExtensionResultsCursor } from '../hooks/use-extension-results-cursor';
import { useInfiniteSearch } from '../hooks/use-infinite-search';
import { MainContext } from '../context';

export const ExtensionList: FunctionComponent<ExtensionListProps> = ({ filter, onUpdate }) => {
    const { handleError } = useContext(MainContext);
    const { data, error, isLoading, isFetchingNextPage, hasNextPage, fetchNextPage } = useInfiniteSearch(filter);

    const extensions = useMemo(() => data?.pages.flatMap(page => page.extensions) ?? [], [data]);
    const totalSize = data?.pages[0]?.totalSize ?? 0;
    const grid = useExtensionResultsCursor(extensions.length);

    // Report the result count to the parent (shown in the search header).
    useEffect(() => {
        onUpdate(totalSize);
    }, [totalSize, onUpdate]);

    // Surface fetch failures through the app's error handler.
    useEffect(() => {
        if (error) {
            handleError(error);
        }
    }, [error, handleError]);

    // Fresh filter: put the cursor back on the first card so Enter opens it.
    useEffect(() => {
        grid.reset();
    }, [filter.query, filter.category, filter.sortBy, filter.sortOrder, grid.reset]);

    const pageSize = filter.size;
    const loading = isLoading || isFetchingNextPage;

    // Cards already in the query cache when this list mounts (e.g. returning via
    // browser back) render in place without replaying the entrance fade, so the
    // page looks as if it was never unmounted. Cards that load afterwards still fade.
    const restoredCount = useRef(loading ? 0 : extensions.length).current;

    // Index-keyed slots (the list only appends): loading slots render as
    // skeletons that become cards in place, so the fade isn't restarted.
    const slotCount = extensions.length + (loading ? pageSize : 0);
    const cards = Array.from({ length: slotCount }, (_, idx) => {
        const extension = extensions[idx];
        return (
            <ExtensionCard
                key={idx}
                extension={extension}
                appear={idx >= restoredCount}
                fadeDelayMs={Math.min(idx % pageSize, 5) * 200}
                {...(extension ? grid.itemProps(idx) : {})}
            />
        );
    });

    return (
        <InfiniteScroll
            loadMore={() => {
                if (hasNextPage && !isFetchingNextPage) {
                    void fetchNextPage();
                }
            }}
            hasMore={hasNextPage && !isFetchingNextPage}
            threshold={200}>
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
                {cards}
            </Box>
        </InfiniteScroll>
    );
};

export interface ExtensionListProps {
    filter: ExtensionFilter;
    onUpdate: (resultNumber: number) => void;
}
