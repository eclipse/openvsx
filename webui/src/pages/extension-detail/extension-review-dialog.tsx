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
import { Button, Dialog, DialogTitle, DialogContent, DialogContentText, TextField, DialogActions, Theme, Box } from "@material-ui/core";
import { withStyles, createStyles, WithStyles } from "@material-ui/styles";
import { ExtensionRegistryService } from "../../extension-registry-service";
import { ExtensionRatingStarSetter } from "./extension-rating-star-setter";
import { UserData, ExtensionRaw } from "../../extension-registry-types";
import { handleError } from "../../utils";

const revivewDialogStyles = (theme: Theme) => createStyles({

});

class ExtensionReviewDialogComponent extends React.Component<ExtensionReviewDialogComponent.Props, ExtensionReviewDialogComponent.State> {

    protected service: ExtensionRegistryService;
    protected starSetter: ExtensionRatingStarSetter | null;

    constructor(props: ExtensionReviewDialogComponent.Props) {
        super(props);

        this.service = ExtensionRegistryService.instance;

        this.state = {
            open: false,
            title: '',
            comment: ''
        };
    }

    protected handleOpenButton = async () => {
        const user = await this.service.getUser();
        if (user && UserData.is(user)) {
            this.setState({ open: true });
        }
    }
    protected handleCancel = () => this.setState({ open: false });
    protected handleSave = async () => {
        try {
            const rating = this.starSetter ? this.starSetter.state.number : 1;
            await this.service.postReview({
                rating,
                title: this.state.title,
                comment: this.state.comment,
                user: this.props.user.name
            }, this.props.reviewPostUrl);
            this.setState({ open: false, title: '', comment: '' });
            this.props.saveCompleted();
        } catch (err) {
            handleError(err);
        }
    }
    protected handleCommentChange = (event: React.ChangeEvent<HTMLTextAreaElement>) => this.setState({ comment: event.target.value });
    protected handleTitleChange = (event: React.ChangeEvent<HTMLInputElement>) => this.setState({ title: event.target.value });

    render() {
        return <React.Fragment>
            <Button variant='contained' color='secondary' onClick={this.handleOpenButton}>
                Write a Review
            </Button>
            <Dialog open={this.state.open} onClose={this.handleCancel}>
                <DialogTitle>{this.props.extension.displayName || this.props.extension.name} Review</DialogTitle>
                <DialogContent>
                    <DialogContentText>
                        Your review will be posted publicly as {this.props.user.name}
                    </DialogContentText>
                    <ExtensionRatingStarSetter ref={ref => this.starSetter = ref} />
                    <Box my={2}>
                        <TextField fullWidth label='Review Title' onChange={this.handleTitleChange} />
                    </Box>
                    <TextField
                        margin="dense"
                        label="Your Review..."
                        fullWidth
                        multiline
                        variant="outlined"
                        rows={4}
                        onChange={this.handleCommentChange}
                    />
                </DialogContent>
                <DialogActions>
                    <Button onClick={this.handleCancel} color="secondary">
                        Cancel
                    </Button>
                    <Button onClick={this.handleSave} variant="contained" color="secondary">
                        Post Review
                    </Button>
                </DialogActions>
            </Dialog>
        </React.Fragment>;
    }
}

export namespace ExtensionReviewDialogComponent {
    export interface Props extends WithStyles<typeof revivewDialogStyles> {
        extension: ExtensionRaw,
        reviewPostUrl: string,
        user: UserData,
        saveCompleted: () => void
    }
    export interface State {
        open: boolean,
        comment: string,
        title: string
    }
}

export const ExtensionReviewDialog = withStyles(revivewDialogStyles)(ExtensionReviewDialogComponent);