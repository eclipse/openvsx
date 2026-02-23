/********************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import type { ReactElement } from 'react';
import { FunctionComponent, useRef, useState, useEffect, cloneElement } from 'react';
import { Tooltip, TooltipProps } from '@mui/material';
interface ConditionalTooltipProps extends Omit<TooltipProps, 'title'> {
    title: string;
}

/**
 * A Tooltip component that only shows when the child content is truncated with ellipsis.
 * Uses ref to check if the content overflows its container.
 */
export const ConditionalTooltip: FunctionComponent<ConditionalTooltipProps> = ({
    title,
    children,
    ...tooltipProps
}) => {
    const textRef = useRef<HTMLElement>(null);
    const [isTruncated, setIsTruncated] = useState(false);

    useEffect(() => {
        const checkTruncation = () => {
            if (textRef.current) {
                setIsTruncated(textRef.current.scrollWidth > textRef.current.clientWidth);
            }
        };

        checkTruncation();

        // Recheck on window resize
        window.addEventListener('resize', checkTruncation);
        return () => window.removeEventListener('resize', checkTruncation);
    }, [title, children]);

    return (
        <Tooltip
            title={isTruncated ? title : ''}
            disableHoverListener={!isTruncated}
            disableInteractive
            PopperProps={{
                disablePortal: true,
                modifiers: [
                    {
                        name: 'preventOverflow',
                        enabled: true,
                        options: {
                            boundary: 'clippingParents',
                        },
                    },
                ],
                sx: {
                    pointerEvents: 'none',
                },
            }}
            {...tooltipProps}
        >
            {cloneElement(children as ReactElement, {
                ref: textRef,
            })}
        </Tooltip>
    );
};
