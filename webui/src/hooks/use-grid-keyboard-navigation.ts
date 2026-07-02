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
    const { focusSearch, focusResultsSignal } = useSearchFocus();

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

    const setRovingTabIndex = useCallback((items: HTMLElement[], activeIndex: number): void => {
        items.forEach((item, i) => {
            item.tabIndex = i === activeIndex ? 0 : -1;
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
            setRovingTabIndex(items, active === -1 ? 0 : active);
        };
        sync();
        const observer = new MutationObserver(sync);
        observer.observe(container, { childList: true, subtree: true });
        return () => observer.disconnect();
    }, [getItems, setRovingTabIndex]);

    // Focus the first card when the search field asks to hand off (ArrowDown).
    useSignalEffect(
        focusResultsSignal,
        useCallback(() => {
            const items = getItems();
            if (items.length === 0) {
                return;
            }
            setRovingTabIndex(items, 0);
            items[0].focus();
        }, [getItems, setRovingTabIndex])
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

            switch (event.key) {
                case 'ArrowRight':
                    nextIndex = Math.min(currentIndex + 1, last);
                    break;
                case 'ArrowLeft':
                    nextIndex = Math.max(currentIndex - 1, 0);
                    break;
                case 'ArrowDown':
                    nextIndex = Math.min(currentIndex + cols, last);
                    break;
                case 'ArrowUp':
                    if (currentIndex < cols) {
                        event.preventDefault();
                        focusSearch();
                        return;
                    }
                    nextIndex = currentIndex - cols;
                    break;
                case 'Home':
                    nextIndex = 0;
                    break;
                case 'End':
                    nextIndex = last;
                    break;
            }

            if (nextIndex !== currentIndex) {
                event.preventDefault();
                setRovingTabIndex(items, nextIndex);
                items[nextIndex].focus();
            }
        },
        [getItems, getColumnCount, setRovingTabIndex, focusSearch]
    );

    const onFocus = useCallback(
        (event: FocusEvent<T>): void => {
            const target = event.target as HTMLElement;
            if (!target.matches(ITEM_SELECTOR)) return;
            const items = getItems();
            const index = items.indexOf(target);
            if (index >= 0) setRovingTabIndex(items, index);
        },
        [getItems, setRovingTabIndex]
    );

    return { containerRef, onKeyDown, onFocus };
}
