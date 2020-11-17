/********************************************************************************
 * Copyright (c) 2019 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import * as React from 'react';
import {
    Button, Theme, createStyles, WithStyles, withStyles, Dialog, DialogTitle,
    DialogContent, DialogContentText, Box, TextField, DialogActions, Typography
} from '@material-ui/core';
import { ButtonWithProgress } from '../../components/button-with-progress';
import { CopyToClipboard } from '../../components/copy-to-clipboard';
import { PersonalAccessToken, isError } from '../../extension-registry-types';
import { MainContext } from '../../context';

const TOKEN_DESCRIPTION_SIZE = 255;

const tokensDialogStyle = (theme: Theme) => createStyles({
    boldText: {
        color: 'red',
        fontWeight: 'bold'
    }
});

class GenerateTokenDialogComponent extends React.Component<GenerateTokenDialogComponent.Props, GenerateTokenDialogComponent.State> {

    static contextType = MainContext;
    declare context: MainContext;

    constructor(props: GenerateTokenDialogComponent.Props) {
        super(props);

        this.state = {
            open: false,
            posted: false,
            description: ''
        };
    }

    protected handleOpenDialog = () => {
        this.setState({ open: true, posted: false });
    };

    protected handleCancel = () => {
        this.setState({
            open: false,
            description: '',
            token: undefined
        });
    };

    protected handleDescriptionChange = (event: React.ChangeEvent<HTMLInputElement>) => {
        const description = event.target.value;
        let descriptionError: string | undefined;
        if (description.length > TOKEN_DESCRIPTION_SIZE) {
            descriptionError = `The description must not be longer than ${TOKEN_DESCRIPTION_SIZE} characters.`;
        }
        this.setState({ description, descriptionError });
    };

    protected handleGenerate = async () => {
        if (!this.context.user) {
            return;
        }
        this.setState({ posted: true });
        try {
            const token = await this.context.service.createAccessToken(this.context.user, this.state.description);
            if (isError(token)) {
                throw token;
            }
            this.setState({ token });
            this.props.handleTokenGenerated();
        } catch (err) {
            this.context.handleError(err);
        }
    };

    handleEnter = (e: KeyboardEvent) => {
        if (e.code ===  'Enter') {
            this.handleGenerate();
        }
    };

    componentDidMount() {
        document.addEventListener('keydown', this.handleEnter);
    }

    componentWillUnmount() {
        document.removeEventListener('keydown', this.handleEnter);
    }

    render() {
        return <React.Fragment>
            <Button variant='outlined' onClick={this.handleOpenDialog}>Generate new token</Button>
            <Dialog open={this.state.open} onClose={this.handleCancel}>
                <DialogTitle>Generate new token</DialogTitle>
                <DialogContent>
                    <DialogContentText>
                        Describe where you will use this token.
                    </DialogContentText>
                    <Box my={2}>
                        <TextField
                            disabled={Boolean(this.state.token)}
                            fullWidth
                            label='Token Description'
                            error={Boolean(this.state.descriptionError)}
                            helperText={this.state.descriptionError}
                            onChange={this.handleDescriptionChange} />
                    </Box>
                    <TextField
                        disabled={!this.state.token}
                        margin='dense'
                        label='Generated Token...'
                        fullWidth
                        multiline
                        variant='outlined'
                        rows={4}
                        value={this.state.token ? this.state.token.value : ''}
                    />
                    {
                        this.state.token ?
                        <Box>
                            <Typography classes={{ root: this.props.classes.boldText }}>
                                Copy and paste this token to a safe place. It will not be displayed again.
                            </Typography>
                        </Box> : null
                    }
                </DialogContent>
                <DialogActions>
                     {
                        this.state.token ?
                        <CopyToClipboard tooltipProps={{ placement: 'left' }}>
                            {({ copy }) => (
                                <Button
                                    variant='contained'
                                    color='secondary'
                                    onClick={() => {
                                        copy(this.state.token!.value);
                                    }}
                                >
                                    Copy
                                </Button>
                            )}
                        </CopyToClipboard> : null
                    }
                    <Button onClick={this.handleCancel} color='secondary'>
                        {this.state.token ? 'Close' : 'Cancel'}
                    </Button>
                    {
                        !this.state.token ?
                        <ButtonWithProgress
                                autoFocus
                                error={Boolean(this.state.descriptionError)}
                                working={this.state.posted}
                                onClick={this.handleGenerate} >
                            Generate Token
                        </ButtonWithProgress> : null
                    }
                </DialogActions>
            </Dialog>
        </React.Fragment>;
    }
}

export namespace GenerateTokenDialogComponent {
    export interface Props extends WithStyles<typeof tokensDialogStyle> {
        handleTokenGenerated: () => void;
    }

    export interface State {
        open: boolean;
        posted: boolean;
        description: string;
        descriptionError?: string;
        token?: PersonalAccessToken;
    }
}

export const GenerateTokenDialog = withStyles(tokensDialogStyle)(GenerateTokenDialogComponent);