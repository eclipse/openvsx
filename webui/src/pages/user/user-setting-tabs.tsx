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
import { Tabs, Tab, useTheme, useMediaQuery } from "@material-ui/core";
import { RouteComponentProps } from "react-router-dom";
import { createRoute } from "../../utils";
import { ExtensionRaw } from "../../extension-registry-types";
import { UserSettingsRoutes } from "./user-settings";

export const UserSettingTabs = (props: UserSettingTabs.Props) => {

    const theme = useTheme();
    const isATablet = useMediaQuery(theme.breakpoints.down('md'));
    const isAMobile = useMediaQuery(theme.breakpoints.down('sm'));
    const params = props.match.params as UserSettingTabs.Params;

    const handleChange = (event: React.ChangeEvent, newTab: string) => {
        props.history.push(generateRoute(newTab));
    };

    const generateRoute = (tab: string) => {
        return createRoute([UserSettingsRoutes.ROOT, tab]);
    };

    return (
        <React.Fragment>
            <Tabs
                value={params.tab}
                onChange={handleChange}
                orientation={isATablet ? 'horizontal' : 'vertical'}
                centered={isAMobile ? true : false}
            >
                <Tab value='profile' label='Profile' />
                <Tab value='tokens' label='Access Tokens' />
                <Tab value='namespaces' label='Namespaces' />
            </Tabs>
        </React.Fragment>
    );
};

export namespace UserSettingTabs {
    export interface Props extends RouteComponentProps {
    }

    export interface Params extends ExtensionRaw {
        tab: string;
    }
}
