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
import { toRelativeTime, toLocalTime } from '../utils';

export class Timestamp extends React.Component<Timestamp.Props> {
    render(): React.ReactNode {
        const timestamp = this.props.value;
        return <span title={toLocalTime(timestamp)} className={this.props.className}>
            {toRelativeTime(timestamp)}
        </span>;
    }
}

export namespace Timestamp {
    export interface Props {
        value: string;
        className?: string;
    }
}
