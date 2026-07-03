/******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/

import { FunctionComponent, useLayoutEffect } from 'react';
import { useLocation, useNavigationType } from 'react-router-dom';

// BrowserRouter leaves window scroll untouched on navigation, so a page opened
// from deep in a long list would start at that old offset. Reset to the top on
// forward navigations only: POP (back/forward) keeps the browser's native
// scroll restoration, and same-path param updates (e.g. search filters, which
// replace in place) must not jump either — hence keying on pathname alone.
export const ScrollToTop: FunctionComponent = () => {
    const { pathname } = useLocation();
    const navigationType = useNavigationType();

    useLayoutEffect(() => {
        if (navigationType !== 'POP') {
            window.scrollTo({ top: 0, left: 0 });
        }
    }, [pathname]);

    return null;
};
