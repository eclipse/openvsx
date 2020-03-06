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
import { Typography, Box, createStyles, Theme, WithStyles, withStyles, Button, Container } from "@material-ui/core";
import { RouteComponentProps, Switch, Route, Link as RouteLink } from "react-router-dom";
import SaveAltIcon from '@material-ui/icons/SaveAlt';
import { createRoute, handleError } from "../../utils";
import { ExtensionDetailOverview } from "../extension-detail/extension-detail-overview";
import { ExtensionRegistryService } from "../../extension-registry-service";
import { Extension, UserData, isError, ExtensionRaw } from "../../extension-registry-types";
import { TextDivider } from "../../custom-mui-components/text-divider";
import { ExtensionDetailReviews } from "./extension-detail-reviews";
import { ExtensionDetailTabs } from "./extension-detail-tabs";
import { ExportRatingStars } from "./extension-rating-stars";

export namespace ExtensionDetailRoutes {
    export const ROOT = 'extension-detail';
    export const TAB_PARAM = ':tab';
    export const NAMESPACE_PARAM = ':namespace';
    export const NAME_PARAM = ':name';
    export const OVERVIEW = 'overview';
    export const REVIEWS = 'reviews';

    export const OVERVIEW_ROUTE = createRoute([ROOT, OVERVIEW, NAMESPACE_PARAM, NAME_PARAM]);
    export const REVIEWS_ROUTE = createRoute([ROOT, REVIEWS, NAMESPACE_PARAM, NAME_PARAM]);

    export const EXTENSION_DETAIL_MAIN_ROUTE = createRoute([ROOT, TAB_PARAM, NAMESPACE_PARAM, NAME_PARAM]);
}

const detailStyles = (theme: Theme) => createStyles({
    link: {
        textDecoration: 'none',
        color: theme.palette.text.primary,
        '&:hover': {
            textDecoration: 'underline'
        }
    },
    clickable: {
        cursor: 'pointer'
    },
    row: {
        marginBottom: theme.spacing(1)
    },
    head: {
        backgroundColor: theme.palette.grey[200]
    },
    alignVertically: {
        display: 'flex',
        alignItems: 'center'
    }
});

export class ExtensionDetailComponent extends React.Component<ExtensionDetailComponent.Props, ExtensionDetailComponent.State> {

    constructor(props: ExtensionDetailComponent.Props) {
        super(props);

        this.state = {};
    }

    componentDidMount() {
        const params = this.props.match.params as ExtensionRaw;
        document.title = `${params.name} – ${this.props.pageTitle}`;
        this.updateExtension();
    }

    protected async updateExtension() {
        try {
            const params = this.props.match.params as ExtensionRaw;
            const extensionUrl = this.props.service.getExtensionApiUrl(params);
            const extension = await this.props.service.getExtensionDetail(extensionUrl);
            if (isError(extension)) {
                handleError(extension);
            } else {
                document.title = `${extension.displayName || extension.name} – ${this.props.pageTitle}`;
                this.setState({ extension });
            }
        } catch (err) {
            handleError(err);
        }
    }

    protected onReviewUpdate = () => this.updateExtension();

    render() {
        if (!this.state.extension) {
            return '';
        }
        const { extension } = this.state;
        return <React.Fragment>
            <Box className={this.props.classes.head}>
                <Container maxWidth='lg'>
                    <Box display='flex' py={4}>
                        <Box display='flex' justifyContent='center' alignItems='center' mr={4}>
                            <img src={extension.iconUrl} width='auto' height='120px' />
                        </Box>
                        <Box>
                            <Typography variant='h6' className={this.props.classes.row}>{extension.displayName || extension.name}</Typography>
                            <Box display='flex' className={this.props.classes.row}>
                                <RouteLink
                                    to={createRoute([], [{ key: 'search', value: extension.namespace}])}
                                    className={this.props.classes.link}>
                                    <Box className={this.props.classes.alignVertically}>
                                        {extension.namespace}
                                    </Box>
                                </RouteLink>
                                <TextDivider />
                                <Box className={this.props.classes.alignVertically}>
                                    <SaveAltIcon/>&nbsp;{extension.downloadCount || 0} {extension.downloadCount === 1 ? 'download' : 'downloads'}
                                </Box>
                                <TextDivider />
                                <RouteLink
                                    to={createRoute([ExtensionDetailRoutes.ROOT, ExtensionDetailRoutes.REVIEWS, extension.namespace, extension.name])}
                                    className={this.props.classes.link}
                                    title={
                                        extension.averageRating !== undefined ?
                                        `Average rating: ${this.getRoundedRating(extension.averageRating)} out of 5`
                                        : 'Not rated yet'
                                    }>
                                    <Box className={this.props.classes.alignVertically}>
                                        <ExportRatingStars number={extension.averageRating || 0} />
                                        {`(${this.state.extension.reviewCount})`}
                                    </Box>
                                </RouteLink>
                                <TextDivider />
                                <Box className={this.props.classes.alignVertically}>{extension.license}</Box>
                            </Box>
                            <Box className={this.props.classes.row}>{extension.description}</Box>
                            <Box className={this.props.classes.row}>
                                <Button variant='contained' color='secondary' href={extension.downloadUrl}>
                                    Download
                                </Button>
                            </Box>
                        </Box>
                    </Box>
                </Container>
            </Box>
            <Container maxWidth='lg'>
                <Box>
                    <Box>
                        <ExtensionDetailTabs/>
                    </Box>
                    <Box>
                        <Switch>
                            <Route path={ExtensionDetailRoutes.OVERVIEW_ROUTE}>
                                <ExtensionDetailOverview
                                    extension={this.state.extension}
                                    service={this.props.service} />
                            </Route>
                            <Route path={ExtensionDetailRoutes.REVIEWS_ROUTE}>
                                <ExtensionDetailReviews
                                    extension={this.state.extension}
                                    reviewsDidUpdate={this.onReviewUpdate}
                                    service={this.props.service}
                                    user={this.props.user} />
                            </Route>
                        </Switch>
                    </Box>
                </Box>
            </Container>
        </React.Fragment>;
    }

    protected getRoundedRating(rating: number) {
        return Math.round(rating * 10) / 10;
    }
}

export namespace ExtensionDetailComponent {
    export interface Props extends WithStyles<typeof detailStyles>, RouteComponentProps {
        user?: UserData;
        service: ExtensionRegistryService;
        pageTitle: string;
    }
    export interface State {
        extension?: Extension;
    }
}

export const ExtensionDetail = withStyles(detailStyles)(ExtensionDetailComponent);
