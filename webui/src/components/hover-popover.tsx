/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import React, { FunctionComponent, PropsWithChildren, ReactNode, useState } from 'react';
import { Popover } from "@mui/material";

export const HoverPopover: FunctionComponent<PropsWithChildren<HoverPopoverProps>> = props => {
    const [anchor, setAnchor] = useState<Element | null>(null);

    return <>
        <div
            className={props.className}
            aria-haspopup='true'
            aria-owns={anchor ? props.id : undefined}
            onMouseEnter={event => setAnchor(event.currentTarget)}
            onMouseLeave={() => setAnchor(null)} >
            {props.children}
        </div>
        <Popover
            id={props.id}
            open={Boolean(anchor)}
            anchorEl={anchor}
            anchorOrigin={{ vertical: 'bottom', horizontal: 'left' }}
            onClose={() => setAnchor(null)}
            disableRestoreFocus
            sx={{
                pointerEvents: 'none',
                '& .MuiPopover-paper': {
                    p: 1,
                    maxWidth: '80%'
                }
            }}
        >
            {props.popupContent}
        </Popover>
    </>;
};

export interface HoverPopoverProps {
    id: string;
    popupContent: ReactNode;
    className?: string;
}