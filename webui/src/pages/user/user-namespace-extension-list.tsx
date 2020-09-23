/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import * as React from 'react';
import { Theme, createStyles, WithStyles, withStyles, Typography } from '@material-ui/core';
import { Namespace, isError, Extension, ErrorResult } from '../../extension-registry-types';
import { UserNamespaceExtensionListItem } from './user-namespace-extension-list-item';
import { ExtensionRegistryService } from '../../extension-registry-service';
import { ErrorResponse } from '../../server-request';
import { DelayedLoadIndicator } from '../../components/delayed-load-indicator';
import { PageSettings } from '../../page-settings';

const extensionListStyles = (theme: Theme) => createStyles({
    extensions: {
        display: 'grid',
        gridTemplateColumns: `repeat(auto-fit, minmax(200px, 1fr))`,
        gap: `.5rem`,
        marginTop: '1rem',
    }
});


class UserNamespaceExtensionListComponent extends React.Component<UserNamespaceExtensionListComponent.Props, UserNamespaceExtensionListComponent.State> {

    constructor(props: UserNamespaceExtensionListComponent.Props) {
        super(props);

        this.state = {
            loading: true,
            extensions: undefined,
        };
    }

    componentDidMount() {
        this.updateExtensions();
    }

    componentDidUpdate(prevProps: UserNamespaceExtensionListComponent.Props) {
        if (prevProps.namespace.name !== this.props.namespace.name) {
            this.setState({ extensions: undefined, loading: true });
            this.updateExtensions();
        }
    }

    async updateExtensions() {
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

        const extensions = await Promise.all(
            extensionsURLs.map((url: string) => getExtension(url))
        );

        this.setState({ extensions, loading: false });
    }

    render() {
        const { classes } = this.props;
        return (<>
            <Typography variant='h5'>Extensions</Typography>
            <div className={classes.extensions}>
                <DelayedLoadIndicator loading={this.state.loading} />
                {
                    this.state.extensions ? this.state.extensions.map((extension: Extension, i: number) => <UserNamespaceExtensionListItem
                        pageSettings={this.props.pageSettings}
                        key={`${i}${extension.displayName}`}
                        extension={extension}
                    />) : null
                }
            </div>
        </>
        );
    }
}

export namespace UserNamespaceExtensionListComponent {
    export interface Props extends WithStyles<typeof extensionListStyles> {
        namespace: Namespace;
        service: ExtensionRegistryService;
        setError: (err: Error | Partial<ErrorResponse>) => void;
        pageSettings: PageSettings;
    }

    export interface State {
        loading: boolean;
        extensions?: (Extension | undefined)[];
    }
}

export const UserNamespaceExtensionList = withStyles(extensionListStyles)(UserNamespaceExtensionListComponent);
