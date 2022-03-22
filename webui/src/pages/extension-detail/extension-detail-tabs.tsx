/********************************************************************************
 * Copyright (c) 2019 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import * as React from 'react';
import { Tabs, Tab } from '@material-ui/core';
import { RouteComponentProps, withRouter } from 'react-router-dom';
import { createRoute } from '../../utils';
import { Extension } from '../../extension-registry-types';
import { ExtensionDetailRoutes } from './extension-detail';

export class ExtensionDetailTabsComponent extends React.Component<ExtensionDetailTabs.Props> {

    protected handleChange = (event: React.ChangeEvent, newTab: string): void => {
        const params = this.props.match.params as ExtensionDetailTabs.Params;
        const previousTab = versionPointsToTab(params) ? params.version : 'overview';
        if (newTab !== previousTab) {
            const extension = this.props.extension;
            const arr = [ExtensionDetailRoutes.ROOT, params.namespace, params.name];
            if (params.target) {
                arr.push(params.target);
            }

            if (newTab === 'reviews' || newTab === 'changes') {
                arr.push(newTab);
            } else if (params.version && !versionPointsToTab(params)) {
                arr.push(params.version);
            } else if (extension && !this.isLatestVersion(extension)) {
                arr.push(extension.version);
            }

            this.props.history.push(createRoute(arr));
        }
    };

    protected isLatestVersion(extension: Extension): boolean {
        return extension.versionAlias.indexOf('latest') >= 0;
    }

    render(): React.ReactNode {
        const params = this.props.match.params as ExtensionDetailTabs.Params;
        const tab = versionPointsToTab(params) ? params.version : 'overview';
        return <React.Fragment>
            <Tabs value={tab} onChange={this.handleChange}>
                <Tab value='overview' label='Overview' />
                <Tab value='changes' label='Changes' />
                <Tab value='reviews' label='Ratings &amp; Reviews' />
            </Tabs>
        </React.Fragment>;
    }
}

export namespace ExtensionDetailTabs {
    export interface Props extends RouteComponentProps {
        extension: Extension;
    }

    export interface Params {
        readonly namespace: string;
        readonly name: string;
        readonly target?: string;
        readonly version?: string;
    }
}

export function versionPointsToTab(params: ExtensionDetailTabs.Params): boolean {
    return params.version === 'reviews' || params.version === 'changes';
}

export const ExtensionDetailTabs = withRouter(ExtensionDetailTabsComponent);
