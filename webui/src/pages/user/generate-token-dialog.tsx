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
import {
    Button,
    Theme,
    createStyles,
    WithStyles,
    withStyles,
    Dialog,
    DialogTitle,
    DialogContent,
    DialogContentText,
    Box,
    TextField,
    DialogActions,
    Typography,
    CircularProgress
} from "@material-ui/core";
import { UserData, PersonalAccessToken, isError } from "../../extension-registry-types";
import { ExtensionRegistryService } from "../../extension-registry-service";
import { handleError } from "../../utils";
import { Optional } from "../../custom-mui-components/optional";

const TOKEN_DESCRIPTION_SIZE = 255;

const tokensDialogStyle = (theme: Theme) => createStyles({
    boldText: {
        fontWeight: 'bold'
    },
    buttonProgress: {
        color: theme.palette.secondary.main,
        position: 'absolute',
        top: '50%',
        left: '50%',
        marginTop: -12,
        marginLeft: -12,
    },
    buttonWrapper: {
        position: 'relative'
    }
});

class GenerateTokenDialogComponent extends React.Component<GenerateTokenDialogComponent.Props, GenerateTokenDialogComponent.State> {

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
    }

    protected handleCancel = () => {
        this.setState({
            open: false,
            description: '',
            token: undefined
        });
    }

    protected handleDescriptionChange = (event: React.ChangeEvent<HTMLInputElement>) => {
        const description = event.target.value;
        let descriptionError: string | undefined;
        if (description.length > TOKEN_DESCRIPTION_SIZE) {
            descriptionError = `The description must not be longer than ${TOKEN_DESCRIPTION_SIZE} characters.`;
        }
        this.setState({ description, descriptionError });
    }

    protected handleGenerate = async () => {
        this.setState({ posted: true });
        try {
            const token = await this.props.service.createAccessToken(this.props.user, this.state.description);
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
                        margin="dense"
                        label="Generated Token..."
                        fullWidth
                        multiline
                        variant="outlined"
                        rows={4}
                        value={this.state.token ? this.state.token.value : ''}
                    />
                    <Optional enabled={Boolean(this.state.token)}>
                        <Box>
                            <Typography color='error' classes={{ root: this.props.classes.boldText }}>
                                Copy and paste this token to a safe place. It will not be displayed again.
                            </Typography>
                        </Box>
                    </Optional>
                </DialogContent>
                <DialogActions>
                    <Button onClick={this.handleCancel} color="secondary">
                        {this.state.token ? 'Close' : 'Cancel'}
                    </Button>
                    <Optional enabled={!this.state.token}>
                        <div className={this.props.classes.buttonWrapper}>
                            <Button
                                onClick={this.handleGenerate}
                                disabled={Boolean(this.state.descriptionError) || this.state.posted}
                                variant="contained"
                                color="secondary" >
                                Generate Token
                            </Button>
                            <Optional enabled={this.state.posted}>
                                <CircularProgress size={24} className={this.props.classes.buttonProgress} />
                            </Optional>
                        </div>
                    </Optional>
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
        posted: boolean;
        description: string;
        descriptionError?: string;
        token?: PersonalAccessToken;
    }
}

export const GenerateTokenDialog = withStyles(tokensDialogStyle)(GenerateTokenDialogComponent);