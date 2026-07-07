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

import { createContext, FunctionComponent, ReactNode, useContext, useMemo, useState } from 'react';

/**
 * The tint region an extension detail page declares while mounted. The page
 * only describes it; the nav bar compares the depth against its own scroll
 * position to decide when to wear the color.
 */
export interface ExtensionTint {
    // Gallery color the nav wears while the region backs it, flipping its
    // content to the contrast color. Null for default-colored bands, which
    // keep the nav on theme colors.
    color: string | null;
    // Document offset where the region ends; scrolled past it the nav returns
    // to theme colors.
    depth: number;
}

const ExtensionTintContext = createContext<{
    tint: ExtensionTint | null;
    setTint: (tint: ExtensionTint | null) => void;
}>({ tint: null, setTint: () => {} });

// eslint-disable-next-line react-refresh/only-export-components
export function useExtensionTint(): ExtensionTint | null {
    return useContext(ExtensionTintContext).tint;
}

// eslint-disable-next-line react-refresh/only-export-components
export function useSetExtensionTint(): (tint: ExtensionTint | null) => void {
    return useContext(ExtensionTintContext).setTint;
}

export const ExtensionTintProvider: FunctionComponent<{ children: ReactNode }> = ({ children }) => {
    const [tint, setTint] = useState<ExtensionTint | null>(null);
    const value = useMemo(() => ({ tint, setTint }), [tint]);
    return <ExtensionTintContext.Provider value={value}>{children}</ExtensionTintContext.Provider>;
};
