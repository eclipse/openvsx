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

import { FunctionComponent, useContext, useEffect, useRef, useState } from 'react';
import { Typography } from '@mui/material';
import { Namespace, isError, Extension, ErrorResult } from '../../extension-registry-types';
import { MainContext } from '../../context';
import { ExtensionCardList } from '../extension/extension-card-list';

/**
 * Retrieves the full detail for a single extension of a namespace. The caller decides which endpoint
 * to use, e.g. the public registry API (active extensions only) or the admin API (also returns
 * inactive/soft-deleted extensions). Receives both the extension `name` and its metadata `url` so the
 * implementation can use whichever it needs.
 */
export type FetchNamespaceExtension = (
    abortController: AbortController,
    extension: { name: string; url: string }
) => Promise<Readonly<Extension | ErrorResult>>;

/**
 * Generic list of the extensions published under a namespace. It renders each extension as a card
 * (inactive extensions are rendered greyed out by the card itself). The extension detail lookup is
 * injected via {@link FetchNamespaceExtension} so the same component can be reused with different
 * endpoints on the user and admin surfaces.
 */
export const NamespaceExtensionList: FunctionComponent<NamespaceExtensionListProps> = props => {
    const [extensions, setExtensions] = useState<Extension[]>();
    const [loading, setLoading] = useState<boolean>(true);
    const context = useContext(MainContext);

    const fetchExtension: FetchNamespaceExtension =
        props.fetchExtension ??
        ((abortController, extension) => context.service.getExtensionDetail(abortController, extension.url));

    const abortController = useRef<AbortController>(new AbortController());
    useEffect(() => {
        updateExtensions();
        return () => abortController.current.abort();
    }, []);

    useEffect(() => {
        setExtensions(undefined);
        setLoading(true);
        updateExtensions();
    }, [props.namespace.name]);

    const updateExtensions = async (): Promise<void> => {
        const entries = Object.keys(props.namespace.extensions).map((name: string) => ({
            name,
            url: props.namespace.extensions[name]
        }));

        const getExtension = async (entry: { name: string; url: string }) => {
            try {
                const result = await fetchExtension(abortController.current, entry);
                if (isError(result)) {
                    throw result;
                }
                return result;
            } catch (error) {
                context.handleError(error);
                return undefined;
            }
        };

        const extensionUnfiltered = await Promise.all(entries.map(getExtension));
        const extensions = extensionUnfiltered.filter(e => e != null) as Extension[];

        setExtensions(extensions);
        setLoading(false);
    };

    return (
        <>
            <Typography variant='h5'>Extensions</Typography>
            {extensions && extensions.length > 0 ? (
                <ExtensionCardList
                    extensions={extensions}
                    loading={loading}
                    canDelete={props.canDelete ?? true}
                    routePrefix={props.routePrefix}
                />
            ) : (
                <Typography variant='body1'>No extensions published under this namespace yet.</Typography>
            )}
        </>
    );
};

export interface NamespaceExtensionListProps {
    namespace: Namespace;
    // Endpoint used to retrieve each extension's detail. Defaults to the public registry API.
    fetchExtension?: FetchNamespaceExtension;
    canDelete?: boolean;
    // Base route each extension card links to. Supplied by the caller.
    routePrefix: string;
}
