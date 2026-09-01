/********************************************************************************
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
 ********************************************************************************/

import { FunctionComponent, useContext } from 'react';
import { Link } from '@mui/material';
import { styled } from '@mui/material/styles';
import { MainContext } from '../../context';
import VerifiedIcon from '@mui/icons-material/Verified';

const IconLink = styled(Link)(({ theme }) => ({
    display: 'flex',
    alignItems: 'center',
    marginLeft: theme.spacing(0.5)
}));

const IconBadge = styled('span')(({ theme }) => ({
    display: 'flex',
    alignItems: 'center',
    marginLeft: theme.spacing(0.5)
}));

/**
 * Marks a version that was published from a trusted publishing workflow instead of with a
 * personal access token. Nothing is rendered for the ordinary case, so the icon's presence
 * carries the whole signal. The same rocket stands for trusted publishing in the user settings.
 */
export const TrustedPublishingIcon: FunctionComponent<{
    publishedWithTrustedPublishing?: boolean;
    color: string;
}> = ({ color }) => {
    const { pageSettings } = useContext(MainContext);

    const title = 'Published via trusted publishing';
    const url = pageSettings.urls.trustedPublishing;
    const icon = <VerifiedIcon fontSize='small' />;

    // a plain badge when this instance configures no documentation URL to link to
    return url ? (
        <IconLink href={url} target='_blank' rel='noopener' title={title} aria-label={title} sx={{ color }}>
            {icon}
        </IconLink>
    ) : (
        <IconBadge role='img' title={title} aria-label={title} sx={{ color }}>
            {icon}
        </IconBadge>
    );
};
