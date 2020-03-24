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
import { Theme, WithStyles, createStyles, withStyles, Popover } from "@material-ui/core";

const popoverStyles = (theme: Theme) => createStyles({
    popover: {
        pointerEvents: 'none',
    },
    paper: {
        padding: theme.spacing(1),
        maxWidth: 600
    }
});

export class HoverPopoverComponent extends React.Component<HoverPopoverComponent.Props, HoverPopoverComponent.State> {
    constructor(props: Readonly<HoverPopoverComponent.Props>) {
        super(props);
        this.state = {};
    }

    render() {
        return <React.Fragment>
            <div
                className={this.props.className}
                aria-haspopup='true'
                aria-owns={this.state.anchor ? this.props.id : undefined}
                onMouseEnter={event => this.setState({ anchor: event.currentTarget })}
                onMouseLeave={() => this.setState({ anchor: null })} >
                {this.props.children}
            </div>
            <Popover
                id={this.props.id}
                className={this.props.classes.popover}
                classes={{ paper: this.props.classes.paper }}
                open={Boolean(this.state.anchor)}
                anchorEl={this.state.anchor}
                anchorOrigin={{ vertical: 'bottom', horizontal: 'left' }}
                onClose={() => this.setState({ anchor: null })}
                disableRestoreFocus >
                {this.props.popupContent}
            </Popover>
        </React.Fragment>;
    }
}

export namespace HoverPopoverComponent {
    export interface Props extends WithStyles<typeof popoverStyles> {
        id: string;
        popupContent: React.ReactNode;
        className?: string;
    }
    export interface State {
        anchor?: Element | null;
    }
}

export const HoverPopover = withStyles(popoverStyles)(HoverPopoverComponent);
