/********************************************************************************
 * Copyright (c) 2019 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import * as React from "react";
import { Tabs, Tab } from "@material-ui/core";
import { ExtensionDetailRoutes } from "./extension-detail";
import { RouteComponentProps } from "react-router-dom";
import { createURL } from "../../utils";
import { ExtensionRaw } from "../../extension-registry-types";

export class ExtensionDetailTabs extends React.Component<ExtensionDetailTabs.Props, ExtensionDetailTabs.State> {

    protected extensionResolvedRoute: string[];

    constructor(props: ExtensionDetailTabs.Props) {
        super(props);

        const params = this.props.match.params as ExtensionDetailTabs.Params;
        this.extensionResolvedRoute = [params.publisher, params.name, params.version || ''];

        this.state = { tab: params.tab };
    }

    protected handleChange = (event: React.ChangeEvent, newTab: string) => {
        this.props.history.push(this.createRoute(newTab));
        this.setState({ tab: newTab });
    }

    protected createRoute(tab: string) {
        return createURL([ExtensionDetailRoutes.ROOT, tab, ...this.extensionResolvedRoute]);
    }

    render() {
        return <React.Fragment>
            <Tabs value={this.state.tab} onChange={this.handleChange}>
                <Tab value={ExtensionDetailRoutes.OVERVIEW} label='Overview' />
                <Tab value={ExtensionDetailRoutes.RATING} label='Rating &amp; Review' />
            </Tabs>
        </React.Fragment>;
    }
}

export namespace ExtensionDetailTabs {
    export interface Props extends RouteComponentProps {

    }
    export interface State {
        tab: string
    }

    export interface Params extends ExtensionRaw {
        tab: string;
    }
}
