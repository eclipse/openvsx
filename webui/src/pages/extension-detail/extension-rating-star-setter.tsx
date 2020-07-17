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
import { Box, IconButton } from '@material-ui/core';
import StarIcon from '@material-ui/icons/Star';
import StarBorderIcon from '@material-ui/icons/StarBorder';
import { StarRating } from '../../extension-registry-types';

export class ExtensionRatingStarSetter extends React.Component<{}, { number: StarRating }> {

    constructor(props: {}) {
        super(props);

        this.state = { number: 1 };
    }

    protected handleStarClick = (number: StarRating): void => {
        this.setState({ number });
    };

    protected renderStarButton(number: StarRating): React.ReactNode {
        return <IconButton key={'starbtn' + number} onClick={() => this.handleStarClick(number)}>
            {number <= this.state.number ? <StarIcon /> : <StarBorderIcon />}
        </IconButton>;
    }

    protected renderStars(): React.ReactNode[] {
        const stars: React.ReactNode[] = [];
        for (let i = 1; i <= 5; i++) {
            stars.push(this.renderStarButton(i as StarRating));
        }
        return stars;
    }

    render(): React.ReactNode {
        return <React.Fragment>
            <Box>
                {this.renderStars()}
            </Box>
        </React.Fragment>;
    }
}