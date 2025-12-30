/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import React, { FunctionComponent, useEffect, useState } from 'react';
import { Namespace, Extension } from '../../extension-registry-types';
import { UserExtensionList } from './user-extension-list';
import { Typography } from '@mui/material';
import { apiSlice } from '../../store/api';

export const UserNamespaceExtensionListContainer: FunctionComponent<UserNamespaceExtensionListContainerProps> = props => {
    const [extensions, setExtensions] = useState<Extension[]>();
    const [loading, setLoading] = useState<boolean>(false);
    const [getExtensionDetail] = apiSlice.useLazyGetExtensionDetailQuery();

    useEffect(() => {
        setLoading(true);
        const namespace = props.namespace.name;
        const promises = Object.keys(props.namespace.extensions)
            .map(async (name: string) => {
                const { data } = await getExtensionDetail({ namespace, name });
                return data;
            });

        Promise.all(promises)
            .then((response) => {
                const extensions = response.filter((extension) => extension != null) as Extension[];
                setExtensions(extensions);
                setLoading(false);
            });
    }, [props.namespace.name, props.namespace.extensions]);

    return <>
        <Typography variant='h5'>Extensions</Typography>
        {
            extensions && extensions.length > 0
                ? <UserExtensionList extensions={extensions} loading={loading} canDelete />
                : <Typography variant='body1'>No extensions published under this namespace yet.</Typography>
        }
    </>;
};

export interface UserNamespaceExtensionListContainerProps {
    namespace: Namespace;
}