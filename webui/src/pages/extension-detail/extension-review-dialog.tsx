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
import { Button, Dialog, DialogTitle, DialogContent, DialogContentText, TextField, DialogActions, Theme } from "@material-ui/core";
import { withStyles, createStyles, WithStyles } from "@material-ui/styles";
import { handleError } from "../../utils";
import { ExtensionRegistryService } from "../../extension-registry-service";
import { UserData, ExtensionRaw, isError } from "../../extension-registry-types";
import { ExtensionRatingStarSetter } from "./extension-rating-star-setter";

const REVIEW_COMMENT_SIZE = 2048;

const revivewDialogStyles = (theme: Theme) => createStyles({

});

class ExtensionReviewDialogComponent extends React.Component<ExtensionReviewDialogComponent.Props, ExtensionReviewDialogComponent.State> {

    protected starSetter: ExtensionRatingStarSetter | null;

    constructor(props: ExtensionReviewDialogComponent.Props) {
        super(props);

        this.state = {
            open: false,
            comment: ''
        };
    }

    protected handleOpenButton = () => {
        if (this.props.user) {
            this.setState({ open: true });
        }
    }

    protected handleCancel = () => this.setState({ open: false });

    protected handlePost = async () => {
        try {
            const rating = this.starSetter ? this.starSetter.state.number : 1;
            const result = await this.props.service.postReview({
                rating,
                comment: this.state.comment
            }, this.props.reviewPostUrl);
            if (isError(result)) {
                handleError(result);
            } else {
                this.setState({ open: false, comment: '' });
                this.props.saveCompleted();
            }
        } catch (err) {
            handleError(err);
        }
    }

    protected handleCommentChange = (event: React.ChangeEvent<HTMLTextAreaElement>) => {
        const comment = event.target.value;
        var commentError: string | undefined;
        if (comment.length > REVIEW_COMMENT_SIZE) {
            commentError = `The review comment must not be longer than ${REVIEW_COMMENT_SIZE} characters.`;
        }
        this.setState({ comment, commentError });
    };

    render() {
        return <React.Fragment>
            <Button variant='contained' color='secondary' onClick={this.handleOpenButton}>
                Write a Review
            </Button>
            <Dialog open={this.state.open} onClose={this.handleCancel}>
                <DialogTitle>{this.props.extension.displayName || this.props.extension.name} Review</DialogTitle>
                <DialogContent>
                    <DialogContentText>
                        Your review will be posted publicly as {this.props.user.loginName}
                    </DialogContentText>
                    <ExtensionRatingStarSetter ref={ref => this.starSetter = ref} />
                    <TextField
                        margin="dense"
                        label="Your Review..."
                        fullWidth
                        multiline
                        variant="outlined"
                        rows={4}
                        error={Boolean(this.state.commentError)}
                        helperText={this.state.commentError}
                        onChange={this.handleCommentChange} />
                </DialogContent>
                <DialogActions>
                    <Button
                        onClick={this.handleCancel}
                        color="secondary" >
                        Cancel
                    </Button>
                    <Button
                        onClick={this.handlePost}
                        disabled={Boolean(this.state.commentError)}
                        variant="contained"
                        color="secondary" >
                        Post Review
                    </Button>
                </DialogActions>
            </Dialog>
        </React.Fragment>;
    }
}

export namespace ExtensionReviewDialogComponent {
    export interface Props extends WithStyles<typeof revivewDialogStyles> {
        extension: ExtensionRaw;
        reviewPostUrl: string;
        user: UserData;
        service: ExtensionRegistryService;
        saveCompleted: () => void;
    }
    export interface State {
        open: boolean;
        comment: string;
        commentError?: string;
    }
}

export const ExtensionReviewDialog = withStyles(revivewDialogStyles)(ExtensionReviewDialogComponent);