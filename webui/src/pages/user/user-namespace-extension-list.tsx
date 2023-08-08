/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import React, { FunctionComponent, useContext, useEffect, useState } from 'react';
import { Namespace, isError, Extension, ErrorResult } from '../../extension-registry-types';
import { MainContext } from '../../context';
import { UserExtensionList } from './user-extension-list';
import { Typography } from '@mui/material';

export const UserNamespaceExtensionListContainer: FunctionComponent<UserNamespaceExtensionListContainerProps> = props => {
    const [extensions, setExtensions] = useState<Extension[]>();
    const [loading, setLoading] = useState<boolean>(true);
    const context = useContext(MainContext);

    const abortController = new AbortController();
    useEffect(() => {
        updateExtensions();
        return () => abortController.abort();
    }, []);

    useEffect(() => {
        setExtensions(undefined);
        setLoading(true);
        updateExtensions();
    }, [props.namespace.name]);

    const updateExtensions = async (): Promise<void> => {
        const extensionsURLs: string[] = Object.keys(props.namespace.extensions).map((key: string) => props.namespace.extensions[key]);

        const getExtension = async (url: string) => {
            let result: Extension | ErrorResult;
            try {
                result = await context.service.getExtensionDetail(abortController, url);
                if (isError(result)) {
                    throw result;
                }
                return result;
            } catch (error) {
                context.handleError(error);
                return undefined;
            }
        };

        const extensionUnfiltered = await Promise.all(
            extensionsURLs.map((url: string) => getExtension(url))
        );
        const extensions = extensionUnfiltered.filter(e => !!e) as Extension[];

        setExtensions(extensions);
        setLoading(false);
    };

    return <>
        <Typography variant='h5'>Extensions</Typography>
        {
            extensions && extensions.length > 0
                ? <UserExtensionList extensions={extensions} loading={loading} />
                : <Typography  variant='body1'>No extensions published under this namespace yet.</Typography>
        }
    </>;
};

export interface UserNamespaceExtensionListContainerProps {
    namespace: Namespace;
}