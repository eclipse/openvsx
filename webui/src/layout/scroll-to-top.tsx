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

import { FunctionComponent, useLayoutEffect } from 'react';
import { useLocation, useNavigationType } from 'react-router-dom';

// BrowserRouter leaves window scroll untouched on navigation, so a page opened
// from deep in a long list would start at that old offset. Reset to the top on
// forward navigations only: POP (back/forward) keeps the browser's native
// scroll restoration, and same-path param updates (e.g. search filters, which
// replace in place) must not jump either — hence keying on pathname alone.
// Links that swap content in place opt out via state `{ preserveScroll: true }`.
export const ScrollToTop: FunctionComponent = () => {
    const { pathname, state } = useLocation();
    const navigationType = useNavigationType();
    const preserveScroll = (state as { preserveScroll?: boolean } | null)?.preserveScroll;

    useLayoutEffect(() => {
        if (navigationType !== 'POP' && !preserveScroll) {
            window.scrollTo({ top: 0, left: 0 });
        }
    }, [pathname]);

    return null;
};
