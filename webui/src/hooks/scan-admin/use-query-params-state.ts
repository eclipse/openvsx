/******************************************************************************
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
 *****************************************************************************/

import { useState } from 'react';

const getQuery = () => {
    if (typeof window !== 'undefined') {
        return new URLSearchParams(window.location.search);
    }
    return new URLSearchParams();
};

const getQueryStringVal = (key: string): string | null => {
    return getQuery().get(key);
};

const useQueryParam = (
    key: string,
    defaultVal: string
): [string, (val: string) => void] => {
    const [query, setQuery] = useState(getQueryStringVal(key) || defaultVal);

    const updateUrl = (newVal: string) => {
        setQuery(newVal);

        const query = getQuery();

        if (newVal.trim() !== '') {
            query.set(key, newVal);
        } else {
            query.delete(key);
        }

        // This check is necessary if using the hook with Gatsby
        if (typeof window !== 'undefined') {
            const { protocol, pathname, host } = window.location;
            const newUrl = `${protocol}//${host}${pathname}?${query.toString()}`;
            window.history.pushState({}, '', newUrl);
        }
    };

    return [query, updateUrl];
};

export default useQueryParam;
