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
import { Box, Typography, Avatar, Select, MenuItem, Button, Theme, createStyles } from "@material-ui/core";
import { withStyles, WithStyles } from "@material-ui/styles";
import { NamespaceMembership, MembershipRole, UserData, isError, Namespace } from "../../extension-registry-types";
import { ExtensionRegistryService } from "../../extension-registry-service";
import { ErrorResponse } from "../../server-request";

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

class UserNamespaceMemberComponent extends React.Component<UserNamespaceMember.Props, UserNamespaceMember.State> {

    constructor(props: UserNamespaceMember.Props) {
        super(props);

        this.state = {
            role: this.props.member.role
        };
    }

    protected changeRole = (event: React.ChangeEvent<{ value: unknown }>) => this.doChangeRole(event.target.value as MembershipRole);
    protected async doChangeRole(role: MembershipRole): Promise<void> {
        try {
            const endpoint = this.props.namespace.roleUrl;
            const membership = this.props.member;
            const result = await this.props.service.setNamespaceMember(endpoint, membership.user, role);
            if (isError(result)) {
                throw (result);
            }
            this.setState({ role });
        } catch (err) {
            this.props.handleError(err);
        }
    }

    render(): React.ReactNode {
        return <Box key={'member:' + this.props.member.user.loginName} p={2} display='flex' alignItems='center'>
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
            <Box className={this.props.classes.buttonContainer}>
                {
                    this.props.member.user.loginName === this.props.user.loginName && this.props.member.user.provider === this.props.user.provider ?
                        '' :
                        <Box m={1}>
                            <Select variant='outlined' classes={{outlined: this.props.classes.selectOutlined}} value={this.state.role} onChange={this.changeRole}>
                                <MenuItem value='contributor'>Contributor</MenuItem>
                                <MenuItem value='owner'>Owner</MenuItem>
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

export namespace UserNamespaceMember {
    export interface Props extends WithStyles<typeof memberStyle> {
        namespace: Namespace;
        member: NamespaceMembership;
        user: UserData;
        service: ExtensionRegistryService;
        onRemoveUser: (member: NamespaceMembership) => void;
        handleError: (err: Error | Partial<ErrorResponse>) => void;
    }
    export interface State {
        role: MembershipRole
    }
}

export const UserNamespaceMember = withStyles(memberStyle)(UserNamespaceMemberComponent);
