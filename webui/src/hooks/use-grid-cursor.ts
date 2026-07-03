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

import { FocusEvent, KeyboardEvent, RefCallback, RefObject, useCallback, useRef, useState } from 'react';

type GridDirection = 'up' | 'down' | 'left' | 'right';

/** Linear cursor steps in reading order, for driving the cursor from outside the grid. */
export type GridStep = 'next' | 'previous';

const ARROW_KEY_DIRECTIONS: Record<string, GridDirection | undefined> = {
    ArrowUp: 'up',
    ArrowDown: 'down',
    ArrowLeft: 'left',
    ArrowRight: 'right'
};

function getNextIndex(direction: GridDirection, current: number, cols: number, last: number): number {
    switch (direction) {
        case 'right':
            return Math.min(current + 1, last);
        case 'left':
            return Math.max(current - 1, 0);
        case 'down':
            return Math.min(current + cols, last);
        case 'up':
            return current < cols ? current : current - cols;
    }
}

export interface GridCursorOptions {
    /** Show the cursor ring on the active item (sets `data-cursor-visible` on the container). */
    cursorVisible?: boolean;
    /** Called when ArrowUp is pressed on the first row (e.g. to hand focus back to a search field). */
    onExitTop?: () => void;
}

export interface GridContainerProps {
    ref: RefObject<HTMLElement>;
    onKeyDown: (event: KeyboardEvent<HTMLElement>) => void;
    onFocus: (event: FocusEvent<HTMLElement>) => void;
    'data-cursor-visible'?: string;
}

export interface GridItemProps {
    ref: RefCallback<HTMLElement>;
    tabIndex: number;
    'data-active'?: string;
}

export interface GridCursor {
    /** Spread onto the CSS grid container. */
    containerProps: GridContainerProps;
    /** Spread onto the focusable element of the item at `index`. */
    itemProps: (index: number) => GridItemProps;
    /** Step the cursor one item in reading order without moving focus (e.g. while a search field keeps focus). */
    move: (step: GridStep) => void;
    /** Click the item under the cursor. */
    openActive: () => void;
    /** Put the cursor back on the first item (e.g. when the list is replaced). */
    reset: () => void;
}

/**
 * Cursor for a CSS grid of focusable items.
 *
 * The active item is both the visible cursor (`data-active`, styled while the
 * container has `data-cursor-visible`) and the grid's only tab stop (roving
 * tabindex). It moves two ways: arrow keys inside the grid move real focus
 * across both axes, while `move()` steps the cursor one item at a time in
 * reading order with focus staying wherever it is. The column count is read
 * from the container's computed grid tracks, so it adapts to viewport width.
 */
export function useGridCursor(itemCount: number, options: GridCursorOptions = {}): GridCursor {
    const { cursorVisible = false, onExitTop } = options;
    const containerRef = useRef<HTMLElement>(null);
    const itemRefs = useRef(new Map<number, HTMLElement>());
    const refCallbacks = useRef(new Map<number, RefCallback<HTMLElement>>());
    const [activeIndex, setActiveIndex] = useState(0);

    // Clamp instead of resetting state so the cursor survives the list shrinking.
    const active = itemCount === 0 ? 0 : Math.min(activeIndex, itemCount - 1);
    const activeRef = useRef(active);
    activeRef.current = active;

    const getColumnCount = useCallback((): number => {
        const container = containerRef.current;
        if (!container) return 1;
        const tracks = getComputedStyle(container).gridTemplateColumns;
        return tracks === 'none' ? 1 : tracks.split(' ').length;
    }, []);

    const moveTo = useCallback((index: number, opts: { focus: boolean }): void => {
        setActiveIndex(index);
        const item = itemRefs.current.get(index);
        if (!item) return;
        if (opts.focus) {
            item.focus();
        } else {
            item.scrollIntoView({ block: 'nearest' });
        }
    }, []);

    const move = useCallback(
        (step: GridStep): void => {
            if (itemCount === 0) return;
            const delta = step === 'next' ? 1 : -1;
            const next = Math.min(Math.max(activeRef.current + delta, 0), itemCount - 1);
            if (next !== activeRef.current) {
                moveTo(next, { focus: false });
            }
        },
        [itemCount, moveTo]
    );

    const openActive = useCallback((): void => {
        itemRefs.current.get(activeRef.current)?.click();
    }, []);

    const reset = useCallback((): void => setActiveIndex(0), []);

    const onKeyDown = useCallback(
        (event: KeyboardEvent<HTMLElement>): void => {
            if (itemCount === 0) return;
            const current = activeRef.current;
            const last = itemCount - 1;
            const cols = getColumnCount();

            if (event.key === 'ArrowUp' && current < cols && onExitTop) {
                event.preventDefault();
                onExitTop();
                return;
            }
            let next: number;
            if (event.key === 'Home') {
                next = 0;
            } else if (event.key === 'End') {
                next = last;
            } else {
                const direction = ARROW_KEY_DIRECTIONS[event.key];
                if (!direction) return;
                next = getNextIndex(direction, current, cols, last);
            }
            if (next !== current) {
                event.preventDefault();
                moveTo(next, { focus: true });
            }
        },
        [itemCount, getColumnCount, moveTo, onExitTop]
    );

    // Keep the cursor on whichever item receives real focus (Tab, click).
    const onFocus = useCallback((event: FocusEvent<HTMLElement>): void => {
        for (const [index, item] of itemRefs.current) {
            if (item === event.target) {
                setActiveIndex(index);
                return;
            }
        }
    }, []);

    const itemProps = useCallback(
        (index: number): GridItemProps => {
            let ref = refCallbacks.current.get(index);
            if (!ref) {
                // Cached per index so a memoized item only re-renders when its
                // tabIndex / data-active actually change.
                ref = element => {
                    if (element) {
                        itemRefs.current.set(index, element);
                    } else {
                        itemRefs.current.delete(index);
                    }
                };
                refCallbacks.current.set(index, ref);
            }
            return {
                ref,
                tabIndex: index === active ? 0 : -1,
                'data-active': index === active ? '' : undefined
            };
        },
        [active]
    );

    return {
        containerProps: {
            ref: containerRef,
            onKeyDown,
            onFocus,
            'data-cursor-visible': cursorVisible ? '' : undefined
        },
        itemProps,
        move,
        openActive,
        reset
    };
}
