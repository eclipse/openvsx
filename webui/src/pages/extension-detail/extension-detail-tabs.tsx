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
import { ExtensionDetailRoutes } from './extension-detail';

export class ExtensionDetailTabsComponent extends React.Component<ExtensionDetailTabs.Props> {

    protected handleChange = (event: React.ChangeEvent, newTab: string): void => {
        const params = this.props.match.params as ExtensionDetailTabs.Params;
        const previousTab = params.version === 'reviews' ? 'reviews' : 'overview';
        if (newTab !== previousTab) {
            let route: string;
            if (newTab === 'reviews') {
                route = createRoute([ExtensionDetailRoutes.ROOT, params.namespace, params.name, 'reviews']);
            } else if (params.version && params.version !== 'reviews') {
                route = createRoute([ExtensionDetailRoutes.ROOT, params.namespace, params.name, params.version]);
            } else {
                route = createRoute([ExtensionDetailRoutes.ROOT, params.namespace, params.name]);
            }
            this.props.history.push(route);
        }
    };

    render(): React.ReactNode {
        const params = this.props.match.params as ExtensionDetailTabs.Params;
        const tab = params.version === 'reviews' ? 'reviews' : 'overview';
        return <React.Fragment>
            <Tabs value={tab} onChange={this.handleChange}>
                <Tab value='overview' label='Overview' />
                <Tab value='reviews' label='Ratings &amp; Reviews' />
            </Tabs>
        </React.Fragment>;
    }
}

export namespace ExtensionDetailTabs {
    export interface Props extends RouteComponentProps {
    }

    export interface Params {
        readonly namespace: string;
        readonly name: string;
        readonly version?: string;
    }
}

export const ExtensionDetailTabs = withRouter(ExtensionDetailTabsComponent);
