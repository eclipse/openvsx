/******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/

import { FocusEvent, KeyboardEvent, RefObject, useCallback, useEffect, useRef } from 'react';
import { useSearchFocus } from '../context/search/search-focus-context';
import { useSignalEffect } from './use-signal-effect';

const ITEM_SELECTOR = 'a[data-ext-card]';
const NAV_KEYS = ['ArrowUp', 'ArrowDown', 'ArrowLeft', 'ArrowRight', 'Home', 'End'];

type Direction = 'up' | 'down' | 'left' | 'right';

const KEY_DIRECTIONS: Record<string, Direction> = {
    ArrowUp: 'up',
    ArrowDown: 'down',
    ArrowLeft: 'left',
    ArrowRight: 'right'
};

function getNextIndex(direction: Direction, current: number, cols: number, last: number): number {
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

interface GridKeyboardNavigation<T extends HTMLElement> {
    containerRef: RefObject<T>;
    onKeyDown: (event: KeyboardEvent<T>) => void;
    onFocus: (event: FocusEvent<T>) => void;
}

/**
 * Two-axis keyboard navigation for the extension results grid.
 *
 * Items (card links marked with `data-ext-card`) participate in a roving
 * tabindex: only one is tab-focusable at a time. Arrow keys move focus across
 * both axes — the column count is derived from the live layout so it adapts to
 * viewport width and infinite-scroll appends. Pressing ArrowUp on the first row
 * returns focus to the search field.
 */
export function useGridKeyboardNavigation<T extends HTMLElement>(): GridKeyboardNavigation<T> {
    const containerRef = useRef<T>(null);
    const { focusSearch, resultsNavSignal, resultsNavAction, searchFocused } = useSearchFocus();

    // The cursor ring is only shown while the search field has focus (the card
    // styles key off this container attribute).
    useEffect(() => {
        containerRef.current?.toggleAttribute('data-search-focus', searchFocused);
    }, [searchFocused]);

    const getItems = useCallback((): HTMLElement[] => {
        const container = containerRef.current;
        if (!container) return [];
        return Array.from(container.querySelectorAll<HTMLElement>(ITEM_SELECTOR));
    }, []);

    // Column count = number of items sharing the topmost row's offsetTop.
    const getColumnCount = useCallback((items: HTMLElement[]): number => {
        if (items.length === 0) return 1;
        const firstTop = items[0].offsetTop;
        let cols = 0;
        for (const item of items) {
            if (item.offsetTop !== firstTop) break;
            cols++;
        }
        return Math.max(cols, 1);
    }, []);

    // The active item doubles as the visible cursor: it is the only tab stop and
    // carries `data-active`, which the card styles as a focus ring. The search
    // field moves it without taking focus away from the input.
    const setActiveItem = useCallback((items: HTMLElement[], activeIndex: number): void => {
        items.forEach((item, i) => {
            item.tabIndex = i === activeIndex ? 0 : -1;
            item.toggleAttribute('data-active', i === activeIndex);
        });
    }, []);

    // Keep exactly one item tab-focusable as the list mounts and grows
    // (infinite scroll appends new links that default to tabIndex 0).
    useEffect(() => {
        const container = containerRef.current;
        if (!container) return;
        const sync = () => {
            const items = getItems();
            if (items.length === 0) return;
            const active = items.findIndex(item => item.tabIndex === 0);
            setActiveItem(items, active === -1 ? 0 : active);
        };
        sync();
        const observer = new MutationObserver(sync);
        observer.observe(container, { childList: true, subtree: true });
        return () => observer.disconnect();
    }, [getItems, setActiveItem]);

    // Cursor commands from the search field: move the active item across both
    // grid axes, or open it (Enter).
    useSignalEffect(
        resultsNavSignal,
        useCallback(() => {
            const items = getItems();
            if (items.length === 0 || !resultsNavAction) {
                return;
            }
            const current = Math.max(
                items.findIndex(item => item.tabIndex === 0),
                0
            );
            if (resultsNavAction === 'open') {
                items[current].click();
                return;
            }
            const next = getNextIndex(resultsNavAction, current, getColumnCount(items), items.length - 1);
            if (next !== current) {
                setActiveItem(items, next);
                items[next].scrollIntoView({ block: 'nearest' });
            }
        }, [getItems, getColumnCount, setActiveItem, resultsNavAction])
    );

    const onKeyDown = useCallback(
        (event: KeyboardEvent<T>): void => {
            if (!NAV_KEYS.includes(event.key)) return;

            const items = getItems();
            if (items.length === 0) return;

            const currentIndex = items.indexOf(document.activeElement as HTMLElement);
            if (currentIndex === -1) return;

            const cols = getColumnCount(items);
            const last = items.length - 1;
            let nextIndex = currentIndex;

            if (event.key === 'ArrowUp' && currentIndex < cols) {
                event.preventDefault();
                focusSearch();
                return;
            }
            if (event.key === 'Home') {
                nextIndex = 0;
            } else if (event.key === 'End') {
                nextIndex = last;
            } else {
                nextIndex = getNextIndex(KEY_DIRECTIONS[event.key], currentIndex, cols, last);
            }

            if (nextIndex !== currentIndex) {
                event.preventDefault();
                setActiveItem(items, nextIndex);
                items[nextIndex].focus();
            }
        },
        [getItems, getColumnCount, setActiveItem, focusSearch]
    );

    const onFocus = useCallback(
        (event: FocusEvent<T>): void => {
            const target = event.target as HTMLElement;
            if (!target.matches(ITEM_SELECTOR)) return;
            const items = getItems();
            const index = items.indexOf(target);
            if (index >= 0) setActiveItem(items, index);
        },
        [getItems, setActiveItem]
    );

    return { containerRef, onKeyDown, onFocus };
}
