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
import { ButtonWithProgress } from "../../custom-mui-components/button-with-progress";
import { ExtensionRegistryService } from "../../extension-registry-service";
import { UserData, Extension, isError } from "../../extension-registry-types";
import { ExtensionRatingStarSetter } from "./extension-rating-star-setter";
import { ErrorResponse } from '../../server-request';

const REVIEW_COMMENT_SIZE = 2048;

const reviewDialogStyles = (theme: Theme) => createStyles({
    dialogActions: {
        [theme.breakpoints.down('xs')]: {
            justifyContent: 'center'
        }
    },
    stars: {
        [theme.breakpoints.down('xs')]: {
            width: '135%',
            transform: 'scale(.8) translateX(-15%)',
        },
        ['@media(max-width: 305px)']: {
            width: '140%',
            transform: 'scale(.7) translateX(-23%)',
        }
    }
});

class ExtensionReviewDialogComponent extends React.Component<ExtensionReviewDialogComponent.Props, ExtensionReviewDialogComponent.State> {

    protected starSetter: ExtensionRatingStarSetter | null;

    constructor(props: ExtensionReviewDialogComponent.Props) {
        super(props);

        this.state = {
            open: false,
            posted: false,
            comment: ''
        };
    }

    protected handleOpenButton = () => {
        if (this.props.user) {
            this.setState({ open: true, posted: false });
        }
    }

    protected handleCancel = () => this.setState({ open: false });

    protected handlePost = async () => {
        this.setState({ posted: true });
        try {
            const rating = this.starSetter ? this.starSetter.state.number : 1;
            const result = await this.props.service.postReview({
                rating,
                comment: this.state.comment
            }, this.props.reviewPostUrl);
            if (isError(result)) {
                throw result;
            }
            this.setState({ open: false, comment: '' });
            this.props.saveCompleted();
        } catch (err) {
            this.props.setError(err);
        }
    }

    protected handleCommentChange = (event: React.ChangeEvent<HTMLTextAreaElement>) => {
        const comment = event.target.value;
        let commentError: string | undefined;
        if (comment.length > REVIEW_COMMENT_SIZE) {
            commentError = `The review comment must not be longer than ${REVIEW_COMMENT_SIZE} characters.`;
        }
        this.setState({ comment, commentError });
    }

    handleEnter = (e: KeyboardEvent) => {
        if (e.code ===  'Enter') {
            this.handlePost();
        }
    }

    componentDidMount() {
        document.addEventListener('keydown', this.handleEnter);
    }

    componentWillUnmount() {
        document.removeEventListener('keydown', this.handleEnter);
    }

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
                    <div className={this.props.classes.stars}>
                        <ExtensionRatingStarSetter ref={(ref: any) => this.starSetter = ref} />
                    </div>
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
                <DialogActions className={this.props.classes.dialogActions}>
                    <Button
                        onClick={this.handleCancel}
                        color="secondary" >
                        Cancel
                    </Button>
                    <ButtonWithProgress
                            error={Boolean(this.state.commentError)}
                            working={this.state.posted}
                            onClick={this.handlePost} >
                        Post Review
                    </ButtonWithProgress>
                </DialogActions>
            </Dialog>
        </React.Fragment>;
    }
}

export namespace ExtensionReviewDialogComponent {
    export interface Props extends WithStyles<typeof reviewDialogStyles> {
        extension: Extension;
        reviewPostUrl: string;
        user: UserData;
        service: ExtensionRegistryService;
        saveCompleted: () => void;
        setError: (err: Error | Partial<ErrorResponse>) => void;

    }
    export interface State {
        open: boolean;
        posted: boolean;
        comment: string;
        commentError?: string;
    }
}

export const ExtensionReviewDialog = withStyles(reviewDialogStyles)(ExtensionReviewDialogComponent);