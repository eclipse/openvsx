/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import * as React from "react";

export class Optional extends React.Component<Optional.Props> {
    render() {
        if (this.props.enabled) {
            return this.props.children;
        } else {
            return '';
        }
    }
}

export namespace Optional {
    export interface Props {
        enabled: boolean;
    }
}
