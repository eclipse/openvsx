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
import { LinearProgress } from "@material-ui/core";

export class DelayedLoadIndicator extends React.Component<DelayedLoadIndicator.Props, DelayedLoadIndicator.State> {

    protected timeout: number;

    constructor(props: DelayedLoadIndicator.Props) {
        super(props);

        this.state = { waiting: false };
    }

    componentDidMount() {
        this.componentDidUpdate({ loading: false });
    }

    componentWillUnmount() {
        clearTimeout(this.timeout);
    }

    componentDidUpdate(prevProps: DelayedLoadIndicator.Props) {
        if (this.props.loading !== prevProps.loading) {
            clearTimeout(this.timeout);
        }
        if (this.props.loading && !prevProps.loading) {
            this.setState({ waiting: true });
            this.timeout = setTimeout(() => {
                this.setState({ waiting: false });
            }, this.props.delay || 200);
        }
    }

    render() {
        if (this.props.loading && !this.state.waiting) {
            return <LinearProgress color={this.props.color || 'secondary'}/>;
        } else {
            return '';
        }
    }

}

export namespace DelayedLoadIndicator {
    export interface Props {
        loading: boolean;
        delay?: number;
        color?: 'primary' | 'secondary';
    }
    export interface State {
        waiting: boolean;
    }
}
