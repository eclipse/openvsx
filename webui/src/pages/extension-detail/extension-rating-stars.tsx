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
import StarIcon from '@material-ui/icons/Star';
import StarBorderIcon from '@material-ui/icons/StarBorder';
import StarHalfIcon from '@material-ui/icons/StarHalf';

interface ExportRatingStarsProps {
    number: number;
    fontSize?: 'inherit' | 'default' | 'small' | 'large';
}

export class ExportRatingStars extends React.Component<ExportRatingStarsProps> {
    render(): React.ReactNode {
        return <React.Fragment>
            {this.getStar(1)}{this.getStar(2)}{this.getStar(3)}{this.getStar(4)}{this.getStar(5)}
        </React.Fragment>;
    }

    protected getStar(i: number): React.ReactNode {
        const starsNumber = this.props.number;
        const fontSize = this.props.fontSize || 'default';
        if (i <= starsNumber) {
            return <StarIcon fontSize={fontSize}/>;
        }
        if (i > starsNumber && i - 1 < starsNumber) {
            return <StarHalfIcon fontSize={fontSize}/>;
        }
        return <StarBorderIcon fontSize={fontSize}/>;
    }
}
