/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import React, { FunctionComponent, useState, useRef, useEffect } from 'react';
import { LinearProgress } from "@mui/material";

export const DelayedLoadIndicator: FunctionComponent<DelayedLoadIndicatorProps> = props => {
    const [waiting, setWaiting] = useState<boolean>(false);
    const timeout = useRef<number>();

    useEffect(() => {
        setDelay();
        return tryClearTimeout;
    }, []);

    useEffect(() => {
        setDelay();
    }, [props.loading]);

    const tryClearTimeout = () => {
        if (timeout.current) {
            clearTimeout(timeout.current);
        }
    };

    const setDelay = () => {
        tryClearTimeout();
        if (props.loading) {
            setWaiting(true);
            timeout.current = setTimeout(() => setWaiting(false), props.delay || 200);
        }
    };

    return props.loading && !waiting
        ? <LinearProgress color={props.color || 'secondary'}/>
        : null;
};

export interface DelayedLoadIndicatorProps {
    loading: boolean;
    delay?: number;
    color?: 'primary' | 'secondary';
}
