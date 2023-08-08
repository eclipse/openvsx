/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
import React, { FunctionComponent, ReactElement, useState } from 'react';
import copy from 'clipboard-copy';
import { Tooltip, TooltipProps } from '@mui/material';

export const CopyToClipboard: FunctionComponent<CopyToClipboardProps> = props => {
    const [showTooltip, setTooltip] = useState<boolean>(false);

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
            title={'Copied to clipboard!'}
            leaveDelay={1500}
            onClose={handleOnTooltipClose}
            disableHoverListener
            {...props.tooltipProps || {}}
        >
            {
                props.children({ copy: onCopy }) as ReactElement<any>
            }
        </Tooltip>
    );
};

interface ChildProps {
    copy: (content: any) => void;
}

export interface CopyToClipboardProps {
    tooltipProps?: Partial<TooltipProps>;
    children: (props: ChildProps) => ReactElement<any>;
}