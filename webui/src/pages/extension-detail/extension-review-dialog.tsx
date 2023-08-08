/********************************************************************************
 * Copyright (c) 2019 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import React, { ChangeEvent, FunctionComponent, useContext, useEffect, useState } from 'react';
import { Box, Button, Dialog, DialogTitle, DialogContent, DialogContentText, TextField, DialogActions } from '@mui/material';
import { ButtonWithProgress } from '../../components/button-with-progress';
import { Extension, StarRating, isError } from '../../extension-registry-types';
import { ExtensionRatingStarSetter } from './extension-rating-star-setter';
import { MainContext } from '../../context';

const REVIEW_COMMENT_SIZE = 2048;

export const ExtensionReviewDialog: FunctionComponent<ExtensionReviewDialogProps> = props => {
    const [open, setOpen] = useState<boolean>(false);
    const [posted, setPosted] = useState<boolean>(false);
    const [rating, setRating] = useState<StarRating>(1);
    const [comment, setComment] = useState<string>('');
    const [commentError, setCommentError] = useState<string>();
    const context = useContext(MainContext);
    const abortController = new AbortController();

    useEffect(() => {
        document.addEventListener('keydown', handleEnter);
        return () => {
            abortController.abort();
            document.removeEventListener('keydown', handleEnter);
        };
    }, []);

    const handleOpenButton = () => {
        if (context.user) {
            setOpen(true);
            setPosted(false);
        }
    };

    const handleCancel = () => setOpen(false);

    const handlePost = async () => {
        setPosted(true);
        try {
            const result = await context.service.postReview(abortController, { rating, comment }, props.reviewPostUrl);
            if (isError(result)) {
                throw result;
            }

            setOpen(false);
            setComment('');
            props.saveCompleted();
        } catch (err) {
            context.handleError(err);
        }
    };

    const handleCommentChange = (event: ChangeEvent<HTMLTextAreaElement>) => {
        const comment = event.target.value;
        let commentError: string | undefined;
        if (comment.length > REVIEW_COMMENT_SIZE) {
            commentError = `The review comment must not be longer than ${REVIEW_COMMENT_SIZE} characters.`;
        }

        setComment(comment);
        setCommentError(commentError);
    };

    const handleEnter = (e: KeyboardEvent) => {
        if (e.code ===  'Enter') {
            handlePost();
        }
    };

    if (!context.user) {
        return null;
    }
    return <>
        {!posted && (<Button variant='contained' color='secondary' onClick={handleOpenButton}>
            Write a Review
        </Button>)}
        <Dialog open={open} onClose={handleCancel}>
            <DialogTitle>{props.extension.displayName || props.extension.name} Review</DialogTitle>
            <DialogContent>
                <DialogContentText>
                    Your review will be posted publicly as {context.user.loginName}
                </DialogContentText>
                <Box
                    component='div'
                    sx={{
                        width: { xs: '140%', sm: '100%', md: '100%', lg: '100%', xl: '100%' },
                        transform: { xs: 'scale(.7) translateX(-23%)', sm: 'none', md: 'none', lg: 'none', xl: 'none' }
                    }}
                >
                    <ExtensionRatingStarSetter handleRatingChange={(rating: number) => setRating(rating as StarRating)} />
                </Box>
                <TextField
                    margin='dense'
                    label='Your Review...'
                    fullWidth
                    multiline
                    variant='outlined'
                    rows={4}
                    error={Boolean(commentError)}
                    helperText={commentError}
                    onChange={handleCommentChange} />
            </DialogContent>
            <DialogActions sx={{ justifyContent: { xs: 'center', sm: 'normal', md: 'normal', lg: 'normal', xl: 'normal' } }}>
                <Button
                    onClick={handleCancel}
                    color='secondary' >
                    Cancel
                </Button>
                <ButtonWithProgress autoFocus error={Boolean(commentError)} working={posted} sx={{ ml: 1 }} onClick={handlePost}>
                    Post Review
                </ButtonWithProgress>
            </DialogActions>
        </Dialog>
    </>;
};

export interface ExtensionReviewDialogProps {
    extension: Extension;
    reviewPostUrl: string;
    saveCompleted: () => void;
}