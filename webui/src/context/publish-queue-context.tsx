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

import {
    createContext,
    FunctionComponent,
    ReactNode,
    useCallback,
    useContext,
    useEffect,
    useRef,
    useState
} from 'react';
import { MainContext } from '../context';
import { ErrorResult, Extension, isError } from '../extension-registry-types';

/** How often a freshly published package is re-read, and for how long. */
const POLL_INTERVAL_MS = 5000;
const REVIEW_POLL_TIMEOUT_MS = 5 * 60 * 1000;
// Publishing hands the package to an async pipeline, so the extracted files (the icon among
// them) appear after the response. Worth a short wait, but plenty of extensions have no icon.
const ASSET_POLL_TIMEOUT_MS = 60 * 1000;

export type PublishStatus = 'uploading' | 'reviewing' | 'published' | 'rejected' | 'failed';

export interface PublishItem {
    id: number;
    fileName: string;
    size: number;
    status: PublishStatus;
    /** Set once the registry has accepted the package. */
    extension?: Extension;
    /** Set when the upload or the review failed. */
    error?: string;
    /** The queue is still re-reading the extension because its icon has not been stored yet. */
    awaitingIcon?: boolean;
}

export interface PublishQueue {
    items: PublishItem[];
    /** Uploads every package straight away — there is no confirmation step. */
    publish: (files: File[]) => void;
    dismiss: (id: number) => void;
    /** Drops everything that has finished, leaving work in flight alone. */
    clearFinished: () => void;
}

const PublishQueueContext = createContext<PublishQueue>({
    items: [],
    publish: () => {},
    dismiss: () => {},
    clearFinished: () => {}
});

// eslint-disable-next-line react-refresh/only-export-components
export const usePublishQueue = (): PublishQueue => useContext(PublishQueueContext);

/** Only `.vsix` packages are publishable; a drop may carry anything. */
// eslint-disable-next-line react-refresh/only-export-components
export const isVsixFile = (file: File): boolean => file.name.toLowerCase().endsWith('.vsix');

// eslint-disable-next-line react-refresh/only-export-components
export const isFinished = (item: PublishItem): boolean => item.status !== 'uploading' && item.status !== 'reviewing';

/** `isError` narrows to `ErrorResult`, which does not subtract from a `Readonly<…>` union. */
const isPublished = (result: Readonly<Extension | ErrorResult>): result is Readonly<Extension> => !isError(result);

const statusOf = (extension: Readonly<Extension>): PublishStatus => {
    switch (extension.reviewStatus) {
        case 'under_review':
            return 'reviewing';
        case 'rejected':
            return 'rejected';
        default:
            return 'published';
    }
};

const errorMessage = (err: unknown): string => {
    if (isError(err)) {
        return err.error;
    }
    return err instanceof Error ? err.message : 'Publishing failed.';
};

/**
 * The name of the namespace the registry does not know yet, or undefined when the
 * failure was something else. The server answers `Unknown publisher: <name>\n…`.
 */
const unknownNamespace = (err: unknown): string | undefined => {
    const prefix = 'Unknown publisher: ';
    if (!isError(err) || !err.error.startsWith(prefix)) {
        return undefined;
    }
    // A missing newline would make substring() swap its bounds and yield the prefix itself.
    const end = err.error.indexOf('\n', prefix.length);
    const name = (end === -1 ? err.error.substring(prefix.length) : err.error.substring(prefix.length, end)).trim();
    return name && name !== 'undefined' ? name : undefined;
};

/**
 * App-wide queue of extension uploads. Packages dropped anywhere in the app land
 * here and start uploading immediately; a package the registry puts under review
 * is polled until the verdict arrives, so the queue reflects the real outcome
 * rather than just "uploaded".
 */
export const PublishQueueProvider: FunctionComponent<{ children: ReactNode }> = ({ children }) => {
    const { service, user, handleError } = useContext(MainContext);
    const [items, setItems] = useState<PublishItem[]>([]);
    const nextId = useRef(0);
    const abortController = useRef(new AbortController());

    useEffect(() => {
        const controller = abortController.current;
        return () => controller.abort();
    }, []);

    const update = useCallback((id: number, patch: Partial<PublishItem>) => {
        setItems(current => current.map(item => (item.id === id ? { ...item, ...patch } : item)));
    }, []);

    /**
     * Reads a just-published extension back. It has to be the list endpoint: only that one reports
     * `reviewStatus` (the single-extension endpoint leaves it unset, so a package awaiting a scan
     * would look merely inactive), and it carries the extracted files too.
     */
    const readPublished = useCallback(
        async (namespace: string, name: string): Promise<Readonly<Extension> | undefined> => {
            const result = await service.getExtensions(abortController.current);
            if (!Array.isArray(result)) {
                return undefined;
            }
            return result.find((entry: Readonly<Extension>) => entry.namespace === namespace && entry.name === name);
        },
        [service]
    );

    /**
     * The verdict of the review and the files extracted from the package both land after publish
     * returns, so keep re-reading the extension until they do. Every read refreshes the card, so
     * the icon appears as soon as the pipeline has stored it.
     */
    const pollUntilSettled = useCallback(
        async (id: number, initial: Readonly<Extension>) => {
            const started = Date.now();
            const pending = (extension: Readonly<Extension>): boolean => {
                const elapsed = Date.now() - started;
                if (extension.reviewStatus === 'under_review') {
                    return elapsed < REVIEW_POLL_TIMEOUT_MS;
                }
                return !extension.files?.icon && elapsed < ASSET_POLL_TIMEOUT_MS;
            };

            let current = initial;
            while (pending(current) && !abortController.current.signal.aborted) {
                await new Promise(resolve => setTimeout(resolve, POLL_INTERVAL_MS));
                if (abortController.current.signal.aborted) {
                    return;
                }
                try {
                    const fresh = await readPublished(initial.namespace, initial.name);
                    if (!fresh) {
                        continue;
                    }
                    current = fresh;
                } catch {
                    // A failed read says nothing about the package; wait for the next one.
                    continue;
                }
                update(id, {
                    extension: current,
                    status: statusOf(current),
                    error: current.reviewStatus === 'rejected' ? current.reviewMessage : undefined,
                    awaitingIcon: !current.files?.icon
                });
            }
            // Whatever the loop ended on, nothing more is coming: stop promising an icon.
            update(id, { awaitingIcon: false });
        },
        [readPublished, update]
    );

    /** The publish response carries none of that, so fall back to it only if the read fails. */
    const hydrate = useCallback(
        async (extension: Readonly<Extension>): Promise<Readonly<Extension>> => {
            try {
                return (await readPublished(extension.namespace, extension.name)) ?? extension;
            } catch {
                return extension;
            }
        },
        [readPublished]
    );

    // A rejected publish arrives as a thrown error (`sendRequest` throws the parsed body) but the
    // signature also allows an ErrorResult return; normalise both to a throw so callers catch one shape.
    const publishOnce = useCallback(
        async (file: File): Promise<Readonly<Extension>> => {
            const result = await service.publishExtension(abortController.current, file);
            if (!isPublished(result)) {
                throw result;
            }
            return result;
        },
        [service]
    );

    const upload = useCallback(
        async (id: number, file: File) => {
            try {
                let result: Readonly<Extension>;
                try {
                    result = await publishOnce(file);
                } catch (err) {
                    // A first-time publisher has no namespace yet; create it and publish again.
                    const namespace = unknownNamespace(err);
                    if (!namespace) {
                        throw err;
                    }
                    const created = await service.createNamespace(abortController.current, namespace);
                    if (isError(created)) {
                        throw created;
                    }
                    result = await publishOnce(file);
                }
                const extension = await hydrate(result);
                update(id, {
                    extension,
                    status: statusOf(extension),
                    error: extension.reviewStatus === 'rejected' ? extension.reviewMessage : undefined,
                    awaitingIcon: !extension.files?.icon
                });
                await pollUntilSettled(id, extension);
            } catch (err) {
                if (!abortController.current.signal.aborted) {
                    update(id, { status: 'failed', error: errorMessage(err) });
                    // The card keeps the record, but a rejected publish is worth interrupting for:
                    // this is the same dialog every other failed request in the app raises.
                    handleError(err as Error);
                }
            }
        },
        [update, pollUntilSettled, publishOnce, hydrate, handleError]
    );

    const publish = useCallback(
        (files: File[]) => {
            if (!user) {
                return;
            }
            const queued = files.filter(isVsixFile).map(file => ({
                id: nextId.current++,
                fileName: file.name,
                size: file.size,
                status: 'uploading' as const,
                file
            }));
            if (queued.length === 0) {
                return;
            }
            setItems(current => [...queued.map(({ file: _file, ...item }) => item), ...current]);
            queued.forEach(({ id, file }) => upload(id, file));
        },
        [user, upload]
    );

    const dismiss = useCallback((id: number) => setItems(current => current.filter(item => item.id !== id)), []);
    const clearFinished = useCallback(() => setItems(current => current.filter(item => !isFinished(item))), []);

    return (
        <PublishQueueContext.Provider value={{ items, publish, dismiss, clearFinished }}>
            {children}
        </PublishQueueContext.Provider>
    );
};
