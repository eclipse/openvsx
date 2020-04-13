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
import copy = require('clipboard-copy');
import { Tooltip, TooltipProps } from '@material-ui/core';

interface ChildProps {
    copy: (content: any) => void;
}

export function CopyToClipboard(props: CopyToClipboard.Props) {
    const [showTooltip, setTooltip] = React.useState<boolean>(false);

    const handleOnTooltipClose = () => {
        setTooltip(false);
    };

    const onCopy = (content:  any) => {
        copy(content);
        setTooltip(true);
    };

    return (
        <Tooltip
            open={showTooltip}
            title={"Copied to clipboard!"}
            leaveDelay={1500}
            onClose={handleOnTooltipClose}
            disableHoverListener
            {...props.TooltipProps || {}}
        >
            {
                props.children({ copy: onCopy }) as React.ReactElement<any>
            }
        </Tooltip>
    );
}

export namespace CopyToClipboard {
    export interface Props {
        TooltipProps?: Partial<TooltipProps>;
        children: (props: ChildProps) => React.ReactElement<any>;
    }

    export interface State {
        showTooltip: boolean
    }
}
