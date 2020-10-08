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
import { ExtensionRegistryService } from '../../extension-registry-service';
import { ErrorResponse } from '../../server-request';
import { PageSettings } from '../../page-settings';
import { UserExtensionList } from './user-extension-list';

export class UserNamespaceExtensionListContainer extends React.Component<UserNamespaceExtensionListContainerComponent.Props, UserNamespaceExtensionListContainerComponent.State> {

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
                result = await this.props.service.getExtensionDetail(url);
                if (isError(result)) {
                    throw result;
                }
                return result;
            } catch (error) {
                this.props.setError(error);
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
        return (<>
            <UserExtensionList extensions={this.state.extensions} loading={this.state.loading} />
        </>
        );
    }
}

export namespace UserNamespaceExtensionListContainerComponent {
    export interface Props {
        namespace: Namespace;
        service: ExtensionRegistryService;
        setError: (err: Error | Partial<ErrorResponse>) => void;
        pageSettings: PageSettings;
    }

    export interface State {
        loading: boolean;
        extensions?: Extension[];
    }
}
