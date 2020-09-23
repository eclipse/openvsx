/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import * as React from 'react';
import { Box, Typography, Avatar, Select, MenuItem, Button, Theme, createStyles } from '@material-ui/core';
import { withStyles, WithStyles } from '@material-ui/styles';
import { NamespaceMembership, MembershipRole, UserData, Namespace } from '../../extension-registry-types';
import { ExtensionRegistryService } from '../../extension-registry-service';
import { ErrorResponse } from '../../server-request';

const memberStyle = (theme: Theme) => createStyles({
    memberName: {
        fontWeight: 'bold',
        overflow: 'hidden',
        textOverflow: 'ellipsis'
    },
    buttonContainer: {
        flex: 1,
        display: 'flex',
        justifyContent: 'flex-end',
        alignItems: 'center',
        [theme.breakpoints.down('sm')]: {
            flexDirection: 'column'
        }
    },
    deleteBtn: {
        color: theme.palette.error.main,
        height: 36
    },
    owner: {
        fontFamily: '"Roboto", "Helvetica", "Arial", sans-serif',
        fontWeight: 500,
        lineHeight: '1.75',
        letterSpacing: '0.02857em',
        textTransform: 'uppercase',
        border: '1px solid rgba(0, 0, 0, 0.23)',
        borderRadius: 4,
        padding: '5px 15px',
        fontSize: '0.875rem'
    },
    selectOutlined: {
        padding: '8.5px 8px'
    }
});

class UserNamespaceMemberComponent extends React.Component<UserNamespaceMember.Props> {
    render(): React.ReactNode {
        const memberUser = this.props.member.user;
        return <Box key={'member:' + memberUser.loginName} p={2} display='flex' alignItems='center'>
            <Box alignItems='center' overflow='auto' width='33%'>
                <Typography classes={{ root: this.props.classes.memberName }}>{memberUser.loginName}</Typography>
                {memberUser.fullName ? <Typography variant='body2'>{memberUser.fullName}</Typography> : ''}
            </Box>
            {
                memberUser.avatarUrl ?
                    <Box display='flex' alignItems='center'>
                        <Avatar src={memberUser.avatarUrl}></Avatar>
                    </Box>
                    : ''
            }
            <Box className={this.props.classes.buttonContainer}>
                {
                    memberUser.loginName === this.props.user.loginName && memberUser.provider === this.props.user.provider && memberUser.role && memberUser.role !== 'admin' ?
                        '' :
                        <Box m={1}>
                            <Select
                                variant='outlined'
                                classes={{ outlined: this.props.classes.selectOutlined }}
                                value={this.props.member.role}
                                onChange={(event: React.ChangeEvent<{ value: MembershipRole }>) => this.props.onChangeRole(event.target.value)}>
                                <MenuItem value='contributor'>Contributor</MenuItem>
                                <MenuItem value='owner'>Owner</MenuItem>
                            </Select>
                        </Box>
                }
                {
                    memberUser.loginName === this.props.user.loginName && memberUser.provider === this.props.user.provider && memberUser.role !== 'admin' ?
                        <Box className={this.props.classes.owner}>
                            Owner
                        </Box>
                        :
                        <Button
                            variant='outlined'
                            classes={{ root: this.props.classes.deleteBtn }}
                            onClick={() => this.props.onRemoveUser()}>
                            Delete
                        </Button>
                }
            </Box>
        </Box>;
    }
}

export namespace UserNamespaceMember {
    export interface Props extends WithStyles<typeof memberStyle> {
        namespace: Namespace;
        member: NamespaceMembership;
        user: UserData;
        service: ExtensionRegistryService;
        onChangeRole: (role: MembershipRole) => void;
        onRemoveUser: () => void;
        handleError: (err: Error | Partial<ErrorResponse>) => void;
    }
}

export const UserNamespaceMember = withStyles(memberStyle)(UserNamespaceMemberComponent);
