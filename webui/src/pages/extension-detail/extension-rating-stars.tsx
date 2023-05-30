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
import StarHalfIcon from '@material-ui/icons/StarHalf';
import { Theme, WithStyles, createStyles, withStyles } from '@material-ui/core';

const starStyles = (theme: Theme) => createStyles({
    wrapper: {
        position: 'relative',
        display: 'inline-block',
      },
    topIcon: {
        position: 'absolute',
      },
    bottomIcon: {
        display: 'block',
      }
});

interface ExportRatingStarsProps extends WithStyles<typeof starStyles>{
    number: number;
    fontSize?: 'inherit' | 'default' | 'small' | 'large';
}

class ExportRatingStarsComponent extends React.Component<ExportRatingStarsProps> {
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
            return <span className={this.props.classes.wrapper}>
                <StarHalfIcon fontSize={fontSize} className={this.props.classes.topIcon}/>
                <StarIcon style={{ color: '#bcbcbc' }} fontSize={fontSize} className={this.props.classes.bottomIcon}/>
            </span>;
        }
        return <StarIcon style={{ color: '#bcbcbc' }} fontSize={fontSize}/>;
    }
}

export const ExportRatingStars = withStyles(starStyles)(ExportRatingStarsComponent);
