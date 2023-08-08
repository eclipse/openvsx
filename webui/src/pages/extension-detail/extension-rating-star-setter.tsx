/********************************************************************************
 * Copyright (c) 2019 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import React, { FunctionComponent, ReactNode, useState } from 'react';
import { Box, IconButton } from '@mui/material';
import StarIcon from '@mui/icons-material/Star';
import StarBorderIcon from '@mui/icons-material/StarBorder';
import { StarRating } from '../../extension-registry-types';

export const ExtensionRatingStarSetter: FunctionComponent<ExtensionRatingStarSetterProps> = props => {
    const [rating, setRating] = useState<number>(1);

    const handleStarClick = (rating: StarRating): void => {
        setRating(rating);
        props.handleRatingChange(rating);
    };

    const renderStarButton = (number: StarRating): ReactNode => {
        return <IconButton key={'starbtn' + number} onClick={() => handleStarClick(number)}>
            {number <= rating ? <StarIcon /> : <StarBorderIcon />}
        </IconButton>;
    };

    const renderStars = (): ReactNode[] => {
        const stars: ReactNode[] = [];
        for (let i = 1; i <= 5; i++) {
            stars.push(renderStarButton(i as StarRating));
        }
        return stars;
    };

    return <>
        <Box>
            {renderStars()}
        </Box>
    </>;
};

export interface ExtensionRatingStarSetterProps {
    handleRatingChange: (rating: number) => void
}