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
import { RouteComponentProps } from "react-router-dom";
import { createURL } from "../../utils";
import { ExtensionRaw } from "../../extension-registry-types";
import { UserSettingsRoutes } from "./user-settings";

export class UserSettingTabs extends React.Component<UserSettingTabs.Props, UserSettingTabs.State> {

    protected resolvedRoute: string[];

    constructor(props: UserSettingTabs.Props) {
        super(props);

        const params = this.props.match.params as UserSettingTabs.Params;

        this.state = { tab: params.tab };
    }

    protected handleChange = (event: React.ChangeEvent, newTab: string) => {
        this.props.history.push(this.createRoute(newTab));
        this.setState({ tab: newTab });
    }

    protected createRoute(tab: string) {
        return createURL([UserSettingsRoutes.MAIN, tab]);
    }

    render() {
        return <React.Fragment>
            <Tabs value={this.state.tab} onChange={this.handleChange} orientation='vertical'>
                <Tab value='profile' label='Profile' />
                <Tab value='tokens' label='Tokens' />
            </Tabs>
        </React.Fragment>;
    }
}

export namespace UserSettingTabs {
    export interface Props extends RouteComponentProps {

    }
    export interface State {
        tab: string
    }

    export interface Params extends ExtensionRaw {
        tab: string;
    }
}
