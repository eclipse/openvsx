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
import { Typography } from "@material-ui/core";
import { RouteComponentProps } from "react-router-dom";

export class ExtensionRegistry extends React.Component<RouteComponentProps> {

    render() {
        return <React.Fragment>
            <Typography variant='h3'>ExtensionRegistry</Typography>
        </React.Fragment>;
    }
}
