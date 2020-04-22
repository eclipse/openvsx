/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import * as React from "react";
import { NamespaceMembership, MembershipRole, UserData, isError } from "../../extension-registry-types";
import { Box, Typography, Avatar, Select, MenuItem, Button, Theme, createStyles } from "@material-ui/core";
import { withStyles, WithStyles } from "@material-ui/styles";
import { ExtensionRegistryService } from "../../extension-registry-service";

const memberStyle = (theme: Theme) => createStyles({
    memberName: {
        fontWeight: 'bold',
        overflow: 'hidden',
        textOverflow: 'ellipsis'
    },
    deleteBtnContainer: {
        flex: 1,
        display: 'flex',
        justifyContent: 'flex-end',
        alignItems: 'center'
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
    selectContainer: {
        marginRight: theme.spacing(1),
        marginLeft: theme.spacing(1)
    },
    selectOutlined: {
        padding: '8.5px 8px'
    }
});

export namespace UserNamespaceMember {
    export interface Props extends WithStyles<typeof memberStyle> {
        member: NamespaceMembership;
        user: UserData;
        service: ExtensionRegistryService;
        onRemoveUser: (member: NamespaceMembership) => void
    }
    export interface State {
        role: MembershipRole
    }
}

class UserNamespaceMemberComponent extends React.Component<UserNamespaceMember.Props, UserNamespaceMember.State> {

    constructor(props: UserNamespaceMember.Props) {
        super(props);

        this.state = {
            role: this.props.member.role
        };
    }

    protected changeRole = (event: React.ChangeEvent<{ value: unknown }>) => this.doChangeRole(event.target.value as MembershipRole);
    protected async doChangeRole(role: MembershipRole) {
        const result = await this.props.service.changeNamespaceMemberRole(this.props.member, role);
        if (isError(result)) {
            throw (result);
        }
        this.setState({ role });
    }

    render(): React.ReactNode {
        return <Box key={'member:' + this.props.member.user.loginName} p={2} display='flex'>
            <Box alignItems='center' overflow='auto' width='33%'>
                <Typography classes={{ root: this.props.classes.memberName }}>{this.props.member.user.loginName}</Typography>
                {this.props.member.user.fullName ? <Typography variant='body2'>{this.props.member.user.fullName}</Typography> : ''}
            </Box>
            {
                this.props.member.user.avatarUrl ?
                    <Box display='flex' alignItems='center'>
                        <Avatar src={this.props.member.user.avatarUrl}></Avatar>
                    </Box>
                    : ''
            }
            <Box className={this.props.classes.deleteBtnContainer}>
                {
                    this.props.member.user.loginName === this.props.user.loginName && this.props.member.user.provider === this.props.user.provider ?
                        '' :
                        <Box className={this.props.classes.selectContainer}>
                            <Select variant='outlined' classes={{outlined: this.props.classes.selectOutlined}} value={this.state.role} onChange={this.changeRole}>
                                <MenuItem value={MembershipRole.CONTRIBUTOR}>Contributor</MenuItem>
                                <MenuItem value={MembershipRole.OWNER}>Owner</MenuItem>
                            </Select>
                        </Box>
                }
                {
                    this.props.member.user.loginName === this.props.user.loginName && this.props.member.user.provider === this.props.user.provider ?
                        <Box className={this.props.classes.owner}>
                            Owner
                        </Box>
                        :
                        <Button
                            variant='outlined'
                            onClick={() => this.props.onRemoveUser(this.props.member)}
                            classes={{ root: this.props.classes.deleteBtn }}>
                            Delete
                        </Button>
                }
            </Box>
        </Box>;
    }
}

export const UserNamespaceMember = withStyles(memberStyle)(UserNamespaceMemberComponent);