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

import { FunctionComponent } from 'react';
import { Avatar } from '@mui/material';
import { SxProps, Theme } from '@mui/material/styles';
import { UserData } from '../extension-registry-types';

/**
 * Rounded avatar showing the user's picture, falling back to bold initials on
 * the tinted surface. Size and corner radius come from the caller's `sx`.
 */
export const UserAvatar: FunctionComponent<UserAvatarProps> = ({ user, sx }) => (
    <Avatar
        src={user.avatarUrl}
        alt={user.loginName}
        variant='rounded'
        sx={[{ bgcolor: 'surface3', color: 'text.secondary', fontWeight: 700 }, ...(Array.isArray(sx) ? sx : [sx])]}>
        {user.loginName.slice(0, 2).toUpperCase()}
    </Avatar>
);

export interface UserAvatarProps {
    user: Pick<UserData, 'loginName' | 'avatarUrl'>;
    sx?: SxProps<Theme>;
}
