/******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/

import { FunctionComponent } from 'react';
import { SvgIcon, SvgIconProps } from '@mui/material';
import GitHubIcon from '@mui/icons-material/GitHub';
import RocketLaunchIcon from '@mui/icons-material/RocketLaunch';
import { providerKind } from './registration-fields';

// GitLab logo (Simple Icons, CC0)
const GitLabIcon: FunctionComponent<SvgIconProps> = props => (
    <SvgIcon {...props}>
        <path d='M23.6004 9.5927l-.0337-.0862L20.3.9814a.851.851 0 00-.3362-.405.8748.8748 0 00-.9997.0539.8748.8748 0 00-.29.4399l-2.2055 6.748H7.5375l-2.2057-6.748a.8478.8478 0 00-.29-.4412.8748.8748 0 00-.9997-.0537.8558.8558 0 00-.3362.4049L.4332 9.5015l-.0325.0862a6.0578 6.0578 0 002.0088 6.9995l.0113.0087.03.0213 4.976 3.7264 2.462 1.8633 1.4995 1.1321a1.0085 1.0085 0 001.2197 0l1.4995-1.1321 2.462-1.8633 5.006-3.7489.0125-.01a6.0559 6.0559 0 002.0051-6.9908z' />
    </SvgIcon>
);

export interface TrustedPublisherProviderIconProps extends SvgIconProps {
    providerId: string;
}

export const TrustedPublisherProviderIcon: FunctionComponent<TrustedPublisherProviderIconProps> = props => {
    const { providerId, ...iconProps } = props;
    switch (providerKind(providerId)) {
        case 'github':
            return <GitHubIcon {...iconProps} />;
        case 'gitlab':
            return <GitLabIcon {...iconProps} />;
        default:
            return <RocketLaunchIcon {...iconProps} />;
    }
};
