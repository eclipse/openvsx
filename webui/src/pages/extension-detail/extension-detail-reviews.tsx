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
import { Theme, createStyles, WithStyles, withStyles, Box, Typography, Divider, Link } from '@material-ui/core';
import { MainContext } from '../../context';
import { toLocalTime } from '../../utils';
import { ExtensionReview, Extension, ExtensionReviewList, isEqualUser, isError } from '../../extension-registry-types';
import { TextDivider } from '../../components/text-divider';
import { DelayedLoadIndicator } from '../../components/delayed-load-indicator';
import { ButtonWithProgress } from '../../components/button-with-progress';
import { Timestamp } from '../../components/timestamp';
import { ExportRatingStars } from './extension-rating-stars';
import { ExtensionReviewDialog } from './extension-review-dialog';

const reviewStyles = (theme: Theme) => createStyles({
    header: {
        display: 'flex',
        justifyContent: 'space-between',
        ['@media(max-width: 360px)']: {
            flexDirection: 'column',
            '& > div:first-of-type': {
                marginBottom: '1rem'
            },
            '& button': {
                maxWidth: '12rem',
            }
        },
    },
    link: {
        textDecoration: 'none',
        color: theme.palette.text.primary,
        '&:hover': {
            textDecoration: 'underline'
        }
    },
    comment: {
        overflow: 'hidden',
        textOverflow: 'ellipsis'
    }
});

class ExtensionDetailReviewsComponent extends React.Component<ExtensionDetailReviewsComponent.Props, ExtensionDetailReviewsComponent.State> {

    static contextType = MainContext;
    declare context: MainContext;

    constructor(props: ExtensionDetailReviewsComponent.Props) {
        super(props);

        this.state = { loading: true, revoked: false };
    }

    componentDidMount() {
        this.updateReviews();
    }

    protected async updateReviews() {
        try {
            const reviewList = await this.context.service.getExtensionReviews(this.props.extension);
            this.setState({ reviewList, loading: false, revoked: false });
        } catch (err) {
            this.context.handleError(err);
            this.setState({ loading: false, revoked: false });
        }
    }

    protected readonly saveCompleted = () => {
        this.setState({ loading: true });
        this.updateReviews();
        this.props.reviewsDidUpdate();
    };

    protected handleRevokeButton = async () => {
        this.setState({ revoked: true });
        try {
            const result = await this.context.service.deleteReview(this.state.reviewList!.deleteUrl);
            if (isError(result)) {
                throw result;
            }
            this.saveCompleted();
        } catch (err) {
            this.context.handleError(err);
        }
    };

    render() {
        return <React.Fragment>
            <Box className={this.props.classes.header} my={2}>
                <Box>
                    <Typography variant='h5'>
                        User Reviews
                    </Typography>
                </Box>
                {this.renderButton()}
            </Box>
            <Divider />
            <Box>
                <DelayedLoadIndicator loading={this.state.loading}/>
                {this.renderReviewList(this.state.reviewList)}
            </Box>
        </React.Fragment>;
    }

    protected renderButton(): React.ReactNode {
        if (!this.context.user || !this.state.reviewList) {
            return  '';
        }
        const existingReview = this.state.reviewList.reviews.find(r => isEqualUser(r.user, this.context.user!));
        if (existingReview) {
            const localTime = toLocalTime(existingReview.timestamp);
            return <ButtonWithProgress
                    working={this.state.revoked}
                    onClick={this.handleRevokeButton}
                    title={`Revoke review written by ${this.context.user.loginName} on ${localTime}`} >
                Revoke my Review
            </ButtonWithProgress>;
        } else {
            return <Box>
                <ExtensionReviewDialog
                    saveCompleted={this.saveCompleted}
                    extension={this.props.extension}
                    reviewPostUrl={this.state.reviewList.postUrl}
                />
            </Box>;
        }
    }

    protected renderReviewList(list?: ExtensionReviewList): React.ReactNode {
        if (!list) {
            return '';
        }
        if (list.reviews.length === 0) {
            return <Box mt={3}>
                <Typography>Be the first to review this extension</Typography>
            </Box>;
        }
        return list.reviews.map(this.renderReview.bind(this));
    }

    protected renderReview(r: ExtensionReview): React.ReactNode {
        return <React.Fragment key={r.user.loginName + r.timestamp}>
            <Box my={2}>
                <Box display='flex'>
                    {
                        r.timestamp ?
                        <React.Fragment>
                            <Typography variant='body2'><Timestamp value={r.timestamp}/></Typography>
                            <TextDivider />
                        </React.Fragment>
                        : null
                    }
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
                <Box display='flex' alignItems='center'>
                    <ExportRatingStars number={r.rating} />
                </Box>
                <Box overflow='auto'>
                    <Typography variant='body1' classes={{ root: this.props.classes.comment }}>{r.comment}</Typography>
                </Box>
            </Box>
            <Divider />
        </React.Fragment>;
    }
}

export namespace ExtensionDetailReviewsComponent {
    export interface Props extends WithStyles<typeof reviewStyles> {
        extension: Extension;
        reviewsDidUpdate: () => void;
    }
    export interface State {
        reviewList?: ExtensionReviewList;
        loading: boolean;
        revoked: boolean;
    }
}

export const ExtensionDetailReviews = withStyles(reviewStyles)(ExtensionDetailReviewsComponent);

