/********************************************************************************
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
 ********************************************************************************/

import { afterEach, describe, expect, it, vi } from 'vitest';
import { act, screen } from '@testing-library/react';
import { useLocation, useNavigate } from 'react-router';
import { renderHookWithProviders, renderWithProviders } from '../support/test-providers';
import { useSearchQuery } from '../../../src/context/search/search-context';
import { useSearch, SEARCH_DEBOUNCE_MS } from '../../../src/hooks/use-search';

// Exercise the consumer-facing surface: `useSearch` (the `search` action + the
// applied `filter`) plus the field's draft read (`useSearchQuery`). A raw
// navigate/location stands in for history navigation and reads the route.
function setup(route = '/search') {
    return renderHookWithProviders(
        () => ({ ...useSearch(), draft: useSearchQuery().query, navigate: useNavigate(), location: useLocation() }),
        { route }
    );
}

afterEach(() => vi.useRealTimers());

describe('useSearch — draft ↔ URL', () => {
    it('updates the draft immediately but navigates only after the debounce', () => {
        vi.useFakeTimers();
        const { result } = setup('/search');

        act(() => result.current.search({ query: 'react' }, { debounce: true }));
        expect(result.current.draft).toBe('react'); // shown at once
        expect(result.current.filter.query).toBe(''); // not navigated yet

        act(() => vi.advanceTimersByTime(SEARCH_DEBOUNCE_MS));
        expect(result.current.filter.query).toBe('react');
    });

    it('applies immediately, updating both the draft and the URL', () => {
        const { result } = setup('/');

        act(() => result.current.search({ query: 'vue' }));
        expect(result.current.draft).toBe('vue');
        expect(result.current.filter.query).toBe('vue');
        expect(result.current.location.pathname).toBe('/search');
    });

    // The backspace-dance guard: only history navigation (POP) syncs the field
    // from the URL. Our own navigations are PUSH/REPLACE, so a late-committing
    // echo of one can never roll the field back over what the user just typed.
    it('follows the URL only on POP (back/forward), never on our own navigations', () => {
        const { result } = setup('/search?q=cat');
        expect(result.current.draft).toBe('cat'); // initial load is a POP → seeds the field

        // A PUSH to a different query stands in for our own writes: leave the field alone.
        act(() => result.current.navigate('/search?q=dog'));
        expect(result.current.filter.query).toBe('dog');
        expect(result.current.draft).toBe('cat');

        // Back then forward are POP: the field follows the URL again (here → 'dog').
        act(() => result.current.navigate(-1));
        expect(result.current.draft).toBe('cat');
        act(() => result.current.navigate(1));
        expect(result.current.draft).toBe('dog');
    });

    it('shares one draft across every field (no per-field local copy)', () => {
        const api: { search: ReturnType<typeof useSearch>['search'] | null } = { search: null };
        function Probe({ id, capture }: { id: string; capture?: boolean }) {
            const { query } = useSearchQuery();
            const { search } = useSearch();
            if (capture) api.search = search;
            return <span data-testid={id}>{query}</span>;
        }
        renderWithProviders(
            <>
                <Probe id='a' capture />
                <Probe id='b' />
            </>,
            { route: '/search' }
        );

        // A change through one field is visible in the other in the same commit —
        // what lets keystrokes survive the hero → nav hand-off.
        act(() => api.search!({ query: 'svelte' }, { debounce: true }));
        expect(screen.getByTestId('a').textContent).toBe('svelte');
        expect(screen.getByTestId('b').textContent).toBe('svelte');
    });

    describe('empty query', () => {
        it('re-searches (clears the filter) on the search page', () => {
            vi.useFakeTimers();
            const { result } = setup('/search?q=cat');

            act(() => result.current.search({ query: '' }, { debounce: true }));
            act(() => vi.advanceTimersByTime(SEARCH_DEBOUNCE_MS));
            expect(result.current.filter.query).toBe(''); // navigated to "show all"
        });

        it('does not navigate away from another page', () => {
            vi.useFakeTimers();
            const { result } = setup('/');

            act(() => result.current.search({ query: '' }, { debounce: true }));
            act(() => vi.advanceTimersByTime(SEARCH_DEBOUNCE_MS));
            expect(result.current.location.pathname).toBe('/'); // stayed put
        });
    });
});
