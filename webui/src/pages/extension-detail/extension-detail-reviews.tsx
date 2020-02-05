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
import { Theme, createStyles, WithStyles, withStyles, Box, Typography, Divider } from "@material-ui/core";
import { ExtensionReview, UserData, Extension, ExtensionReviewList } from "../../extension-registry-types";
import { TextDivider } from "../../custom-mui-components/text-divider";
import { ExportRatingStars } from "./extension-rating-stars";
import { ExtensionReviewDialog } from "./extension-review-dialog";
import { ExtensionRegistryService } from "../../extension-registry-service";
import { handleError, toLocalTime } from "../../utils";

const reviewStyles = (theme: Theme) => createStyles({
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
                {
                    this.props.user ? <Box>
                        <ExtensionReviewDialog
                            saveCompleted={this.saveCompleted}
                            extension={this.props.extension}
                            reviewPostUrl={this.state.reviewList.postUrl}
                            user={this.props.user}
                            service={this.props.service} />
                    </Box> : ''
                }
            </Box>
            <Divider />
            <Box>
                {this.state.reviewList.reviews.map(this.renderReview.bind(this))}
            </Box>
        </React.Fragment>;
    }

    protected renderReview(r: ExtensionReview) {
        const zonedDate = toLocalTime(r.timestamp);
        return <React.Fragment key={r.user.loginName + r.title + r.timestamp}>
            <Box my={2}>
                <Box display='flex'>
                    <Typography variant='body2'>{zonedDate ? zonedDate.toLocaleString() : '-'}</Typography>
                    <TextDivider />
                    <Typography variant='body2'>{r.user.loginName}</Typography>
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

