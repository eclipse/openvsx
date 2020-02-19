/********************************************************************************
 * Copyright (c) 2019 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import * as React from "react";
import { Button, Theme, createStyles, WithStyles, withStyles, Dialog, DialogTitle, DialogContent, DialogContentText, Box, TextField, DialogActions, Typography } from "@material-ui/core";
import { UserData, PersonalAccessToken, isError } from "../../extension-registry-types";
import { ExtensionRegistryService } from "../../extension-registry-service";
import { handleError } from "../../utils";

const tokensDialogStyle = (theme: Theme) => createStyles({
    boldText: {
        fontWeight: 'bold'
    }
});

class GenerateTokenDialogComponent extends React.Component<GenerateTokenDialogComponent.Props, GenerateTokenDialogComponent.State> {

    constructor(props: GenerateTokenDialogComponent.Props) {
        super(props);

        this.state = {
            open: false,
            tokenComment: ''
        };
    }

    protected handleOpenDialog = () => {
        this.setState({ open: true });
    }

    protected handleCancel = () => {
        this.setState({
            open: false,
            tokenComment: '',
            token: undefined
        });
    }

    protected handleCommentChange = (event: React.ChangeEvent<HTMLInputElement>) => {
        this.setState({ tokenComment: event.target.value });
    }

    protected handleGenerate = async () => {
        try {
            const token = await this.props.service.createAccessToken(this.props.user, this.state.tokenComment);
            if (isError(token)) {
                handleError(token);
            } else {
                this.setState({ token });
                this.props.handleTokenGenerated();
            }
        } catch (err) {
            handleError(err);
        }
    }

    render() {
        return <React.Fragment>
            <Button variant='outlined' onClick={this.handleOpenDialog}>Generate new token</Button>
            <Dialog open={this.state.open} onClose={this.handleCancel}>
                <DialogTitle>Generate new token</DialogTitle>
                <DialogContent>
                    <DialogContentText>
                        Write a comment. What is the token for?
                    </DialogContentText>
                    <Box my={2}>
                        <TextField disabled={!!this.state.token} required fullWidth label='Token Comment' onChange={this.handleCommentChange} />
                    </Box>
                    <TextField
                        disabled={!this.state.token}
                        margin="dense"
                        label="Generated token..."
                        fullWidth
                        multiline
                        variant="outlined"
                        rows={4}
                        value={this.state.token ? this.state.token.value : ''}
                    />
                    {
                        !this.state.token ? '' : <Box>
                            <Typography color='error' classes={{ root: this.props.classes.boldText }}>
                                Copy and paste this token to a safe place. It will not be displayed again.
                            </Typography>
                        </Box>
                    }
                </DialogContent>
                <DialogActions>
                    <Button onClick={this.handleCancel} color="secondary">
                        {this.state.token ? 'Close' : 'Cancel'}
                    </Button>
                    {
                        !this.state.token ?
                            <Button onClick={this.handleGenerate} variant="contained" color="secondary">
                                Generate Token
                            </Button> : ''
                    }
                </DialogActions>
            </Dialog>
        </React.Fragment>;
    }
}

export namespace GenerateTokenDialogComponent {
    export interface Props extends WithStyles<typeof tokensDialogStyle> {
        user: UserData;
        service: ExtensionRegistryService;
        handleTokenGenerated: () => void;
    }

    export interface State {
        open: boolean;
        tokenComment: string;
        token?: PersonalAccessToken;
    }
}

export const GenerateTokenDialog = withStyles(tokensDialogStyle)(GenerateTokenDialogComponent);