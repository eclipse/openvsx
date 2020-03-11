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
import { Theme, createStyles, WithStyles, withStyles, Box, Typography, Divider, Button, Link } from "@material-ui/core";
import { handleError, toLocalTime } from "../../utils";
import { ExtensionReview, UserData, Extension, ExtensionReviewList, isEqualUser, isError } from "../../extension-registry-types";
import { TextDivider } from "../../custom-mui-components/text-divider";
import { ExtensionRegistryService } from "../../extension-registry-service";
import { ExportRatingStars } from "./extension-rating-stars";
import { ExtensionReviewDialog } from "./extension-review-dialog";

const reviewStyles = (theme: Theme) => createStyles({
    link: {
        textDecoration: 'none',
        color: theme.palette.text.primary,
        '&:hover': {
            textDecoration: 'underline'
        }
    },
    boldText: {
        fontWeight: 'bold'
    }
});

class ExtensionDetailReviewsComponent extends React.Component<ExtensionDetailReviewsComponent.Props, ExtensionDetailReviewsComponent.State> {

    constructor(props: ExtensionDetailReviewsComponent.Props) {
        super(props);

        this.state = {};
    }

    componentDidMount() {
        this.updateReviews();
    }

    protected async updateReviews() {
        try {
            const reviewList = await this.props.service.getExtensionReviews(this.props.extension);
            this.setState({ reviewList });
        } catch (err) {
            handleError(err);
        }
    }

    protected readonly saveCompleted = () => {
        this.updateReviews();
        this.props.reviewsDidUpdate();
    }

    protected handleRevokeButton = async () => {
        try {
            const result = await this.props.service.deleteReview(this.state.reviewList!.deleteUrl);
            if (isError(result)) {
                handleError(result);
            } else {
                this.saveCompleted();
            }
        } catch (err) {
            handleError(err);
        }
    }

    render() {
        if (!this.state.reviewList) {
            return '';
        }
        return <React.Fragment>
            <Box display='flex' justifyContent='space-between' my={2}>
                <Box>
                    <Typography variant='h5'>
                        User Reviews
                    </Typography>
                </Box>
                {this.renderButton()}
            </Box>
            <Divider />
            <Box>
                {this.state.reviewList.reviews.map(this.renderReview.bind(this))}
            </Box>
        </React.Fragment>;
    }

    protected renderButton() {
        if (!this.props.user || !this.state.reviewList) {
            return  '';
        }
        const existingReview = this.state.reviewList.reviews.find(r => isEqualUser(r.user, this.props.user!));
        if (existingReview) {
            const zonedDate = toLocalTime(existingReview.timestamp);
            return <Button variant='contained' color='secondary'
                        onClick={this.handleRevokeButton}
                        title={`Revoke review written by ${this.props.user.loginName} on ${zonedDate ? zonedDate.toLocaleString() : ''}`}>
                Revoke my Review
            </Button>;
        } else {
            return <Box>
                <ExtensionReviewDialog
                    saveCompleted={this.saveCompleted}
                    extension={this.props.extension}
                    reviewPostUrl={this.state.reviewList.postUrl}
                    user={this.props.user}
                    service={this.props.service} />
            </Box>;
        }
    }

    protected renderReview(r: ExtensionReview) {
        const zonedDate = toLocalTime(r.timestamp);
        return <React.Fragment key={r.user.loginName + r.title + r.timestamp}>
            <Box my={2}>
                <Box display='flex'>
                    <Typography variant='body2'>{zonedDate ? zonedDate.toLocaleString() : '-'}</Typography>
                    <TextDivider />
                    <Typography variant='body2'>
                        {
                            r.user.homepage ?
                            <Link href={r.user.homepage} className={this.props.classes.link}>
                                {r.user.loginName}
                            </Link>
                            :
                            r.user.loginName
                        }
                    </Typography>
                </Box>
                <Box display='flex'>
                    <Typography className={this.props.classes.boldText}>{r.title}</Typography>
                    <Box ml={4} display='flex' alignItems='center'>
                        <ExportRatingStars number={r.rating} />
                    </Box>
                </Box>
                <Box>
                    <Typography variant='body1'>{r.comment}</Typography>
                </Box>
            </Box>
            <Divider />
        </React.Fragment>;
    }
}

export namespace ExtensionDetailReviewsComponent {
    export interface Props extends WithStyles<typeof reviewStyles> {
        extension: Extension;
        user?: UserData;
        service: ExtensionRegistryService;
        reviewsDidUpdate: () => void;
    }
    export interface State {
        reviewList?: ExtensionReviewList;
    }
}

export const ExtensionDetailReviews = withStyles(reviewStyles)(ExtensionDetailReviewsComponent);

