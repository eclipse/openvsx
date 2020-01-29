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
import { UserData, PersonalAccessToken } from "../../extension-registry-types";
import { ExtensionRegistryService } from "../../extension-registry-service";

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
            tokenFieldDisabled: true,
            tokenComment: ''
        };
    }

    protected handleOpenDialog = () => {
        this.setState({ open: true });
    }

    protected handleCancel = () => {
        this.setState({ open: false });
    }

    protected handleCommentChange = (event: React.ChangeEvent<HTMLInputElement>) => this.setState({ tokenComment: event.target.value });

    protected handleGenerate = () => {
        this.props.service.generateToken(this.state.tokenComment).then(token => {
            this.setState({ tokenFieldDisabled: false, token });
            this.props.handleTokenGenerated();
        });
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
                        <TextField disabled={!this.state.tokenFieldDisabled} required fullWidth label='Token Comment' onChange={this.handleCommentChange} />
                    </Box>
                    <TextField
                        disabled={this.state.tokenFieldDisabled}
                        margin="dense"
                        label="Generated token..."
                        fullWidth
                        multiline
                        variant="outlined"
                        rows={4}
                        value={this.state.token ? this.state.token.tokenValue : ''}
                    />
                    {
                        this.state.tokenFieldDisabled ? '' : <Box>
                            <Typography color='error' classes={{ root: this.props.classes.boldText }}>
                                Copy and paste this token to a save place! Or you wont ever see it again!
                            </Typography>
                        </Box>
                    }
                </DialogContent>
                <DialogActions>
                    <Button onClick={this.handleCancel} color="secondary">
                        {this.state.tokenFieldDisabled ? 'Cancel' : 'Close'}
                    </Button>
                    {
                        this.state.tokenFieldDisabled ?
                            <Button onClick={this.handleGenerate} variant="contained" color="secondary">
                                Generate token
                            </Button> : ''
                    }
                </DialogActions>
            </Dialog>
        </React.Fragment>;
    }
}

export namespace GenerateTokenDialogComponent {
    export interface Props extends WithStyles<typeof tokensDialogStyle> {
        user: UserData
        service: ExtensionRegistryService
        handleTokenGenerated: () => void
    }

    export interface State {
        open: boolean
        tokenComment: string
        tokenFieldDisabled: boolean
        token?: PersonalAccessToken
    }
}

export const GenerateTokenDialog = withStyles(tokensDialogStyle)(GenerateTokenDialogComponent);