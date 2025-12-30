/********************************************************************************
 * Copyright (c) 2019 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import React, { Fragment, FunctionComponent, ReactNode, useState } from 'react';
import { Box, Typography, Divider, Link } from '@mui/material';
import { toLocalTime } from '../../utils';
import { ExtensionReview, Extension, ExtensionReviewList, isEqualUser } from '../../extension-registry-types';
import { TextDivider } from '../../components/text-divider';
import { DelayedLoadIndicator } from '../../components/delayed-load-indicator';
import { ButtonWithProgress } from '../../components/button-with-progress';
import { Timestamp } from '../../components/timestamp';
import { ExportRatingStars } from './extension-rating-stars';
import { ExtensionReviewDialog } from './extension-review-dialog';
import { useDeleteReviewMutation, useGetExtensionReviewsQuery, useGetUserQuery } from '../../store/api';

export const ExtensionDetailReviews: FunctionComponent<ExtensionDetailReviewsProps> = props => {
    const { data: reviewList, isLoading } = useGetExtensionReviewsQuery(props.extension);
    const [deleteReview] = useDeleteReviewMutation();
    const [revoked, setRevoked] = useState<boolean>(false);
    const { data: user } = useGetUserQuery();

    const handleRevokeButton = async () => {
        setRevoked(true);
        await deleteReview({ deleteReviewUrl: reviewList!.deleteUrl, extension: props.extension });
    };

    const renderButton = (): ReactNode => {
        if (!user || !reviewList) {
            return '';
        }
        const existingReview = reviewList.reviews.find(r => isEqualUser(r.user, user));
        if (existingReview) {
            const localTime = toLocalTime(existingReview.timestamp);
            return <ButtonWithProgress
                working={revoked}
                onClick={handleRevokeButton}
                title={`Revoke review written by ${user.loginName} on ${localTime}`} >
                Revoke my Review
            </ButtonWithProgress>;
        } else {
            return <Box>
                <ExtensionReviewDialog
                    extension={props.extension}
                    reviewPostUrl={reviewList.postUrl}
                />
            </Box>;
        }
    };

    const renderReviewList = (list?: ExtensionReviewList): ReactNode => {
        if (!list) {
            return '';
        }
        if (list.reviews.length === 0) {
            return <Box mt={3}>
                <Typography>Be the first to review this extension</Typography>
            </Box>;
        }
        return list.reviews.map(renderReview.bind(this));
    };

    const renderReview = (r: ExtensionReview): ReactNode => {
        return <Fragment key={r.user.loginName + r.timestamp}>
            <Box my={2}>
                <Box display='flex'>
                    {
                        r.timestamp ?
                            <>
                                <Typography variant='body2'><Timestamp value={r.timestamp} /></Typography>
                                <TextDivider />
                            </>
                            : null
                    }
                    <Typography variant='body2'>
                        {
                            r.user.homepage ?
                                <Link
                                    href={r.user.homepage}
                                    color='text.primary'
                                    underline='hover'
                                >
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
                    <Typography variant='body1' sx={{ overflow: 'hidden', textOverflow: 'ellipsis' }}>{r.comment}</Typography>
                </Box>
            </Box>
            <Divider />
        </Fragment>;
    };

    return <>
        <Box
            sx={{
                my: 2,
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
                }
            }}
        >
            <Box>
                <Typography variant='h5'>
                    User Reviews
                </Typography>
            </Box>
            {renderButton()}
        </Box>
        <Divider />
        <Box>
            <DelayedLoadIndicator loading={isLoading} />
            {renderReviewList(reviewList)}
        </Box>
    </>;

};

export interface ExtensionDetailReviewsProps {
    extension: Extension;
}