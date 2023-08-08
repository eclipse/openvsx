/********************************************************************************
 * Copyright (c) 2019 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import React, { FunctionComponent, ReactNode } from 'react';
import StarIcon from '@mui/icons-material/Star';
import StarHalfIcon from '@mui/icons-material/StarHalf';
import { Box } from '@mui/material';

export interface ExportRatingStarsProps {
    number: number;
    fontSize?: 'inherit' | 'small' | 'medium' | 'large';
}

export const ExportRatingStars: FunctionComponent<ExportRatingStarsProps> = props => {
    const getStar = (i: number): ReactNode => {
        const starsNumber = props.number;
        const fontSize = props.fontSize || 'medium';
        if (i <= starsNumber) {
            return <StarIcon fontSize={fontSize}/>;
        }
        if (i > starsNumber && i - 1 < starsNumber) {
            return <Box component='span' sx={{ position: 'relative', display: 'inline-block' }}>
                <StarHalfIcon fontSize={fontSize} sx={{ position: 'absolute' }}/>
                <StarIcon fontSize={fontSize} sx={{ display: 'block', color: '#bcbcbc' }}/>
            </Box>;
        }
        return <StarIcon fontSize={fontSize} sx={{ color: '#bcbcbc' }}/>;
    };

    return <>{[1, 2, 3, 4, 5].map(getStar)}</>;
};