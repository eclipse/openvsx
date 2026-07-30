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

/** Release a transition whose navigation never landed, rather than suspend rendering forever. */
const COMMIT_TIMEOUT_MS = 250;

let pendingCommit: (() => void) | undefined;

/**
 * Runs `update` inside a view transition, holding it open until React has
 * committed the resulting navigation. react-router wraps location updates in
 * `React.startTransition`, which `flushSync` cannot force, so the browser would
 * otherwise snapshot the new state while the old route was still mounted and the
 * morph would animate an element to its own position.
 */
export function startSearchViewTransition(update: () => void): ViewTransition | undefined {
    if (typeof document.startViewTransition !== 'function') {
        update();
        return undefined;
    }

    return document.startViewTransition(
        () =>
            new Promise<void>(resolve => {
                let timeout = 0;
                const release = () => {
                    window.clearTimeout(timeout);
                    pendingCommit = undefined;
                    resolve();
                };
                timeout = window.setTimeout(release, COMMIT_TIMEOUT_MS);
                pendingCommit = release;
                update();
            })
    );
}

/**
 * Lets the browser snapshot the new state. Call from a component that outlives the
 * navigation — the field that starts the morph unmounts as part of it.
 */
export function resolveSearchViewTransition(): void {
    pendingCommit?.();
}
