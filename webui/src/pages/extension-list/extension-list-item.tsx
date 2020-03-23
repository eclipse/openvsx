/********************************************************************************
 * Copyright (c) 2019 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import React = require("react");
import { Link as RouteLink } from "react-router-dom";
import { Paper, Typography, Box, Grid, Fade } from "@material-ui/core";
import { withStyles, createStyles, WithStyles, Theme } from '@material-ui/core/styles';
import { ExtensionDetailRoutes } from "../extension-detail/extension-detail";
import { ExtensionRaw } from "../../extension-registry-types";
import { ExportRatingStars } from "../extension-detail/extension-rating-stars";
import { ExtensionRegistryService } from "../../extension-registry-service";
import { createRoute } from "../../utils";
import { PageSettings } from "../../page-settings";


const itemStyles = (theme: Theme) => createStyles({
    paper: {
        padding: theme.spacing(3, 2)
    },
    link: {
        textDecoration: 'none'
    }
});

class ExtensionListItemComponent extends React.Component<ExtensionListItemComponent.Props> {
    render() {
        const { classes, extension } = this.props;
        const route = createRoute([ExtensionDetailRoutes.ROOT, extension.namespace, extension.name]);
        return <React.Fragment>
            <Fade in={true} timeout={{ enter: this.props.idx * 200 }}>
                <Grid item xs={12} sm={3} md={2} title={extension.displayName || extension.name}>
                    <RouteLink to={route} className={classes.link}>
                        <Paper className={classes.paper}>
                            <Box display='flex' justifyContent='center' alignItems='center' width='100%' height={80}>
                                <img src={extension.files.icon || this.props.pageSettings.extensionDefaultIconURL}
                                    width='80px'
                                    alt={extension.displayName || extension.name} />
                            </Box>
                            <Box display='flex' justifyContent='center'>
                                <Typography variant='h6' noWrap>
                                    {extension.displayName || extension.name}
                                </Typography>
                            </Box>
                            <Box display='flex' justifyContent='space-between'>
                                <Typography component='div' variant='caption' noWrap={true} align='left'>
                                    {extension.namespace}
                                </Typography>
                                <Typography component='div' variant='caption' noWrap={true} align='right'>
                                    {extension.version}
                                </Typography>
                            </Box>
                            <Box display='flex' justifyContent='center'>
                                <ExportRatingStars number={extension.averageRating || 0} />
                            </Box>
                        </Paper>
                    </RouteLink>
                </Grid>
            </Fade>
        </React.Fragment>;
    }
}

export namespace ExtensionListItemComponent {
    export interface Props extends WithStyles<typeof itemStyles> {
        service: ExtensionRegistryService;
        extension: ExtensionRaw;
        idx: number;
        pageSettings: PageSettings;
    }
}

export const ExtensionListItem = withStyles(itemStyles)(ExtensionListItemComponent);
