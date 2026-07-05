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

import {
    createContext,
    FunctionComponent,
    ReactNode,
    RefObject,
    useCallback,
    useContext,
    useLayoutEffect,
    useMemo,
    useRef,
    useState
} from 'react';

/**
 * Tracks whether a page-level search bar (e.g. the home hero) is active — the
 * nav bar derives its own field's visibility from it.
 */

export interface PageSearchBarValue {
    // A page-level search bar is registered — the nav bar hides its own field.
    hasPageSearchBar: boolean;
    // Synchronous read — `hasPageSearchBar` lags one render when a bar (un)registers
    // in the same commit that emits a focus signal.
    isPageSearchBarActive: () => boolean;
    registerPageSearchBar: () => () => void;
}

const PageSearchBarContext = createContext<PageSearchBarValue>({
    hasPageSearchBar: false,
    isPageSearchBarActive: () => false,
    registerPageSearchBar: () => () => {}
});

// eslint-disable-next-line react-refresh/only-export-components
export function usePageSearchBar(): PageSearchBarValue {
    return useContext(PageSearchBarContext);
}

/**
 * Marks the calling component as the page's search bar, so the nav bar hides its
 * own field and hands focus requests over. Registered while mounted, or — when
 * `visibilityRef` is given — only while that element is in the viewport. Returns
 * whether the bar is currently registered; callers gate focus handling and their
 * view-transition name on it.
 */
// eslint-disable-next-line react-refresh/only-export-components
export function useRegisterPageSearchBar(visibilityRef?: RefObject<Element>): boolean {
    const { registerPageSearchBar } = usePageSearchBar();
    const [registered, setRegistered] = useState(true);
    // Layout effect so the count is updated before other layout effects in the same commit.
    useLayoutEffect(() => {
        const el = visibilityRef?.current;
        if (!el) {
            return registerPageSearchBar();
        }
        let unregister: (() => void) | undefined;
        const update = (visible: boolean) => {
            if (visible && !unregister) {
                unregister = registerPageSearchBar();
            } else if (!visible && unregister) {
                unregister();
                unregister = undefined;
            }
            setRegistered(visible);
        };
        // Seed synchronously — the observer's first callback is async and the nav field would flash.
        const rect = el.getBoundingClientRect();
        update(rect.bottom > 0 && rect.top < window.innerHeight && rect.right > 0 && rect.left < window.innerWidth);
        const observer = new IntersectionObserver(entries => update(entries[entries.length - 1].isIntersecting));
        observer.observe(el);
        return () => {
            observer.disconnect();
            unregister?.();
        };
    }, [registerPageSearchBar, visibilityRef]);
    return registered;
}

export const PageSearchBarProvider: FunctionComponent<{ children: ReactNode }> = ({ children }) => {
    // Ref for synchronous reads during a commit; the state mirror drives re-renders.
    const pageSearchBarCount = useRef(0);
    const [hasPageSearchBar, setHasPageSearchBar] = useState(false);
    const registerPageSearchBar = useCallback(() => {
        pageSearchBarCount.current++;
        setHasPageSearchBar(true);
        return () => {
            pageSearchBarCount.current--;
            setHasPageSearchBar(pageSearchBarCount.current > 0);
        };
    }, []);
    const isPageSearchBarActive = useCallback(() => pageSearchBarCount.current > 0, []);
    const value = useMemo(
        () => ({ hasPageSearchBar, isPageSearchBarActive, registerPageSearchBar }),
        [hasPageSearchBar, isPageSearchBarActive, registerPageSearchBar]
    );
    return <PageSearchBarContext.Provider value={value}>{children}</PageSearchBarContext.Provider>;
};
