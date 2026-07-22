/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import { FunctionComponent, useContext } from 'react';
import { Box, Typography, Select, MenuItem, SelectChangeEvent } from '@mui/material';
import { styled } from '@mui/material/styles';
import { NamespaceMembership, MembershipRole, UserData } from '../../extension-registry-types';
import { MainContext } from '../../context';
import { MONO_FONT } from '../../default/theme';
import { DeleteIconButton } from '../delete-icon-button';
import { TagChip } from '../page-primitives';
import { UserAvatar } from '../user-avatar';
import { NamespaceDetailConfigContext } from './namespace-detail-config';

// Same row anatomy as the trusted publisher list.
// Single line at every width: the name column truncates and the controls shrink.
const MemberRow = styled(Box)(({ theme }) => ({
    display: 'flex',
    alignItems: 'center',
    gap: theme.spacing(2),
    padding: theme.spacing(2),
    [theme.breakpoints.down('sm')]: {
        gap: theme.spacing(1),
        padding: theme.spacing(1.5, 1.25)
    }
}));

export const NamespaceMemberRow: FunctionComponent<NamespaceMemberRowProps> = props => {
    const equalUser = (user1: UserData | undefined, user2: UserData | undefined) => {
        return user1?.loginName === user2?.loginName && user1?.provider === user2?.provider;
    };

    const memberUser = props.member.user;
    const context = useContext(MainContext);
    const { fixSelf } = useContext(NamespaceDetailConfigContext);
    const contextUser = context.user;
    return (
        <MemberRow>
            <UserAvatar
                user={memberUser}
                sx={{
                    width: '2.5rem',
                    height: '2.5rem',
                    borderRadius: '0.625rem',
                    fontSize: '0.875rem',
                    flexShrink: 0
                }}
            />
            <Box sx={{ flex: 1, minWidth: 0 }}>
                <Typography noWrap sx={{ fontSize: '0.875rem', fontWeight: 700, lineHeight: 1.2 }}>
                    {memberUser.fullName ?? memberUser.loginName}
                </Typography>
                <Typography
                    noWrap
                    sx={{
                        fontSize: '0.78125rem',
                        color: 'text.disabled',
                        fontFamily: MONO_FONT,
                        mt: '0.125rem'
                    }}>
                    {memberUser.loginName}
                </Typography>
            </Box>
            {fixSelf && equalUser(memberUser, contextUser) ? (
                <TagChip accent>Owner</TagChip>
            ) : (
                <Box
                    sx={{
                        display: 'flex',
                        alignItems: 'center',
                        gap: { xs: '0.5rem', sm: '0.8125rem' },
                        ml: 'auto',
                        flexShrink: 0
                    }}>
                    <Select
                        variant='outlined'
                        size='small'
                        sx={{
                            fontSize: { xs: '0.75rem', sm: '0.8125rem' },
                            fontWeight: 600,
                            '& .MuiSelect-select': {
                                py: { xs: '0.3125rem', sm: '0.53125rem' },
                                pl: { xs: '0.625rem', sm: '0.875rem' }
                            }
                        }}
                        value={props.member.role}
                        onChange={(event: SelectChangeEvent<MembershipRole>) =>
                            props.onChangeRole(event.target.value as MembershipRole)
                        }>
                        <MenuItem value='contributor'>Contributor</MenuItem>
                        <MenuItem value='owner'>Owner</MenuItem>
                    </Select>
                    <DeleteIconButton
                        title='Remove member'
                        aria-label={`Remove ${memberUser.loginName}`}
                        onClick={() => props.onRemoveUser()}
                    />
                </Box>
            )}
        </MemberRow>
    );
};

export interface NamespaceMemberRowProps {
    member: NamespaceMembership;
    onChangeRole: (role: MembershipRole) => void;
    onRemoveUser: () => void;
}
