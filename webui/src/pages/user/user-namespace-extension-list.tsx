/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import React, { ReactNode } from 'react';
import { Namespace, isError, Extension, ErrorResult } from '../../extension-registry-types';
import { MainContext } from '../../context';
import { UserExtensionList } from './user-extension-list';
import { Typography } from '@material-ui/core';

export class UserNamespaceExtensionListContainer extends React.Component<UserNamespaceExtensionListContainerComponent.Props, UserNamespaceExtensionListContainerComponent.State> {

    static contextType = MainContext;
    declare context: MainContext;

    constructor(props: UserNamespaceExtensionListContainerComponent.Props) {
        super(props);

        this.state = {
            loading: true,
            extensions: undefined,
        };
    }

    componentDidMount(): void {
        this.updateExtensions();
    }

    componentDidUpdate(prevProps: UserNamespaceExtensionListContainerComponent.Props): void {
        if (prevProps.namespace.name !== this.props.namespace.name) {
            this.setState({ extensions: undefined, loading: true });
            this.updateExtensions();
        }
    }

    async updateExtensions(): Promise<void> {
        const extensionsURLs: string[] = Object.keys(this.props.namespace.extensions).map((key: string) => this.props.namespace.extensions[key]);

        const getExtension = async (url: string) => {
            let result: Extension | ErrorResult;
            try {
                result = await this.context.service.getExtensionDetail(url);
                if (isError(result)) {
                    throw result;
                }
                return result;
            } catch (error) {
                this.context.handleError(error);
                return undefined;
            }
        };

        const extensionUnfiltered = await Promise.all(
            extensionsURLs.map((url: string) => getExtension(url))
        );
        const extensions = extensionUnfiltered.filter(e => !!e) as Extension[];

        this.setState({ extensions, loading: false });
    }

    render(): ReactNode {
        return <>
            <Typography variant='h5'>Extensions</Typography>
            {
                this.state.extensions && this.state.extensions.length > 0 ?
                <UserExtensionList extensions={this.state.extensions} loading={this.state.loading} />
                :
                <Typography  variant='body1'>No extensions published under this namespace yet.</Typography>
            }
        </>;
    }
}

export namespace UserNamespaceExtensionListContainerComponent {
    export interface Props {
        namespace: Namespace;
    }

    export interface State {
        loading: boolean;
        extensions?: Extension[];
    }
}
