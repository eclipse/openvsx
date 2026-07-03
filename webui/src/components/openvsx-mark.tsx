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

import { CSSProperties, FunctionComponent } from 'react';

export const OpenVsxMark: FunctionComponent<{ style?: CSSProperties }> = ({ style }) => (
    <svg viewBox='0 0 110 131' style={{ height: '1.875rem', ...style }}>
        <path d='M30 44.2L52.6 5H7.3zM4.6 88.5h45.3L27.2 49.4zm51 0l22.6 39.2 22.6-39.2z' fill='#c160ef' />
        <path d='M52.6 5L30 44.2h45.2zM27.2 49.4l22.7 39.1 22.6-39.1zm51 0L55.6 88.5h45.2z' fill='#a60ee5' />
    </svg>
);
