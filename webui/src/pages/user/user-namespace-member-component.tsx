/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import React, { FunctionComponent, useContext } from 'react';
import { Box, Typography, Avatar, Select, MenuItem, Button, SelectChangeEvent } from '@mui/material';
import { NamespaceMembership, MembershipRole, Namespace, UserData } from '../../extension-registry-types';
import { MainContext } from '../../context';

export const UserNamespaceMember: FunctionComponent<UserNamespaceMemberProps> = props => {
    const equalUser = (user1: UserData | undefined, user2: UserData | undefined) => {
        return user1?.loginName === user2?.loginName && user1?.provider === user2?.provider;
    };

    const memberUser = props.member.user;
    const context = useContext(MainContext);
    const contextUser = context.user;
    return <Box key={'member:' + memberUser.loginName} p={2} display='flex' alignItems='center'>
        <Box alignItems='center' overflow='auto' width='33%'>
            <Typography sx={{ fontWeight: 'bold', overflow: 'hidden', textOverflow: 'ellipsis' }}>{memberUser.loginName}</Typography>
            {memberUser.fullName ? <Typography variant='body2'>{memberUser.fullName}</Typography> : ''}
        </Box>
        {
            memberUser.avatarUrl ?
                <Box display='flex' alignItems='center'>
                    <Avatar src={memberUser.avatarUrl}></Avatar>
                </Box>
                : ''
        }
        <Box
            sx={{
                flex: 1,
                display: 'flex',
                justifyContent: 'flex-end',
                alignItems: 'center',
                flexDirection: { xs: 'column', sm: 'column', md: 'row', lg: 'row', xl: 'row' }
            }}
        >
            {
                props.fixSelf && equalUser(memberUser, contextUser) ?
                    <Box
                        sx={{
                            fontFamily: '"Roboto", "Helvetica", "Arial", sans-serif',
                            fontWeight: 500,
                            lineHeight: '1.75',
                            letterSpacing: '0.02857em',
                            textTransform: 'uppercase',
                            border: '1px solid rgba(0, 0, 0, 0.23)',
                            borderRadius: 4,
                            padding: '5px 15px',
                            fontSize: '0.875rem'
                        }}
                    >
                        Owner
                    </Box>
                    :
                    <>
                        <Box m={1}>
                            <Select
                                variant='outlined'
                                value={props.member.role}
                                onChange={(event: SelectChangeEvent<MembershipRole>) => props.onChangeRole(event.target.value as MembershipRole)}>
                                <MenuItem value='contributor'>Contributor</MenuItem>
                                <MenuItem value='owner'>Owner</MenuItem>
                            </Select>
                        </Box>
                        <Button
                            variant='outlined'
                            sx={{ color: 'error.main', height: 36 }}
                            onClick={() => props.onRemoveUser()}>
                            Delete
                        </Button>
                    </>
            }
        </Box>
    </Box>;
};

export interface UserNamespaceMemberProps {
    namespace: Namespace;
    member: NamespaceMembership;
    fixSelf: boolean;
    onChangeRole: (role: MembershipRole) => void;
    onRemoveUser: () => void;
}