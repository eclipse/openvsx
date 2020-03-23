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
import { Typography, Box, createStyles, Theme, WithStyles, withStyles, Button, Container, Link } from "@material-ui/core";
import { RouteComponentProps, Switch, Route, Link as RouteLink } from "react-router-dom";
import SaveAltIcon from '@material-ui/icons/SaveAlt';
import VerifiedUserIcon from '@material-ui/icons/VerifiedUser';
import PublicIcon from '@material-ui/icons/Public';
import { handleError, createRoute } from "../../utils";
import { ExtensionRegistryService } from "../../extension-registry-service";
import { Extension, UserData, isError, ExtensionRaw } from "../../extension-registry-types";
import { TextDivider } from "../../custom-mui-components/text-divider";
import { HoverPopover } from "../../custom-mui-components/hover-popover";
import { Optional } from "../../custom-mui-components/optional";
import { PageSettings } from "../../page-settings";
import { ExtensionDetailOverview } from "./extension-detail-overview";
import { ExtensionDetailReviews } from "./extension-detail-reviews";
import { ExtensionDetailTabs } from "./extension-detail-tabs";
import { ExportRatingStars } from "./extension-rating-stars";

export namespace ExtensionDetailRoutes {
    export namespace Parameters {
        export const NAMESPACE = ':namespace';
        export const NAME = ':name';
        export const TAB = ':tab?';
    }

    export const TAB_OVERVIEW = 'overview';
    export const TAB_REVIEWS = 'reviews';

    export const ROOT = 'extension';
    export const MAIN = createRoute([ROOT, Parameters.NAMESPACE, Parameters.NAME, Parameters.TAB]);
    export const OVERVIEW = createRoute([ROOT, Parameters.NAMESPACE, Parameters.NAME]);
    export const REVIEWS = createRoute([ROOT, Parameters.NAMESPACE, Parameters.NAME, TAB_REVIEWS]);
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
    titleRow: {
        marginBottom: theme.spacing(1)
    },
    descriptionRow: {
        marginTop: theme.spacing(2),
        marginBottom: theme.spacing(2)
    },
    head: {
        backgroundColor: theme.palette.grey[200]
    },
    alignVertically: {
        display: 'flex',
        alignItems: 'center'
    },
    code: {
        fontFamily: 'monospace',
        fontSize: '1rem'
    }
});

export class ExtensionDetailComponent extends React.Component<ExtensionDetailComponent.Props, ExtensionDetailComponent.State> {

    constructor(props: ExtensionDetailComponent.Props) {
        super(props);
        this.state = {};
    }

    componentDidMount() {
        const params = this.props.match.params as ExtensionRaw;
        document.title = `${params.name} – ${this.props.pageSettings.pageTitle}`;
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
                document.title = `${extension.displayName || extension.name} – ${this.props.pageSettings.pageTitle}`;
                this.setState({ extension });
            }
        } catch (err) {
            handleError(err);
        }
    }

    protected onReviewUpdate = () => this.updateExtension();

    render() {
        const { extension } = this.state;
        if (!extension) {
            return '';
        }
        return <React.Fragment>
            <Box className={this.props.classes.head}>
                <Container maxWidth='lg'>
                    <Box display='flex' py={4}>
                        <Box display='flex' justifyContent='center' alignItems='center' mr={4}>
                            <img src={extension.files.icon || this.props.pageSettings.extensionDefaultIconURL}
                                width='auto'
                                height='120px'
                                alt={extension.displayName || extension.name} />
                        </Box>
                        {this.renderHeader(extension)}
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
                            <Route path={ExtensionDetailRoutes.REVIEWS}>
                                <ExtensionDetailReviews
                                    extension={extension}
                                    reviewsDidUpdate={this.onReviewUpdate}
                                    service={this.props.service}
                                    user={this.props.user} />
                            </Route>
                            <Route path={ExtensionDetailRoutes.OVERVIEW}>
                                <ExtensionDetailOverview
                                    extension={extension}
                                    service={this.props.service}
                                    pageSettings={this.props.pageSettings} />
                            </Route>
                        </Switch>
                    </Box>
                </Box>
            </Container>
        </React.Fragment>;
    }

    protected renderHeader(extension: Extension): React.ReactNode {
        return <Box>
            <Typography variant='h6' className={this.props.classes.titleRow}>{extension.displayName || extension.name}</Typography>
            <Box display='flex'>
                <Box className={this.props.classes.alignVertically} >
                    {this.renderAccessInfo(extension)}&nbsp;Published by&nbsp;{
                        extension.publishedBy.homepage ?
                        <Link href={extension.publishedBy.homepage} className={this.props.classes.link}>
                            {extension.publishedBy.loginName}
                        </Link>
                        :
                        extension.publishedBy.loginName
                    }
                </Box>
                <TextDivider />
                <Box className={this.props.classes.alignVertically}>
                    <SaveAltIcon fontSize='small'/>&nbsp;{extension.downloadCount || 0} {extension.downloadCount === 1 ? 'download' : 'downloads'}
                </Box>
                <TextDivider />
                <RouteLink
                    to={createRoute([ExtensionDetailRoutes.ROOT, extension.namespace, extension.name, ExtensionDetailRoutes.TAB_REVIEWS])}
                    className={this.props.classes.link}
                    title={
                        extension.averageRating !== undefined ?
                        `Average rating: ${this.getRoundedRating(extension.averageRating)} out of 5`
                        : 'Not rated yet'
                    }>
                    <Box className={this.props.classes.alignVertically}>
                        <ExportRatingStars number={extension.averageRating || 0} fontSize='small' />
                        {`(${extension.reviewCount})`}
                    </Box>
                </RouteLink>
                <TextDivider />
                <Box className={this.props.classes.alignVertically}>{this.renderLicense(extension)}</Box>
            </Box>
            <Box className={this.props.classes.descriptionRow}>
                <Typography>{extension.description}</Typography>
            </Box>
            <Box>
                <Button variant='contained' color='secondary' href={extension.files.download}>
                    Download
                </Button>
            </Box>
        </Box>;
    }

    protected getRoundedRating(rating: number): number {
        return Math.round(rating * 10) / 10;
    }

    protected renderAccessInfo(extension: Extension): React.ReactNode {
        let icon: React.ReactElement;
        let description: string;
        switch (extension.namespaceAccess) {
            case 'public':
                icon = <PublicIcon fontSize='small'/>;
                description = 'Everyone can publish to this namespace, so the identity of the publisher cannot be verified.';
                break;
            case 'restricted':
                icon = <VerifiedUserIcon fontSize='small'/>;
                description = 'Only verified owners and contributors can publish to this namespace.';
                break;
            default:
                return '';
        }

        const popupContent = <Typography variant='body2'>
            {extension.displayName || extension.name} is in the {extension.namespaceAccess} namespace <span className={this.props.classes.code}>
                {extension.namespace}
            </span>. {description}
            <Optional enabled={Boolean(this.props.pageSettings.namespaceAccessInfoURL)}>
                <br/>Click on the icon to learn more.
            </Optional>
        </Typography>;

        const popover = <HoverPopover
            id='namespace-popover'
            popupContent={popupContent}
            className={this.props.classes.alignVertically} >
            {icon}
        </HoverPopover>;

        if (this.props.pageSettings.namespaceAccessInfoURL) {
            return <Link
                href={this.props.pageSettings.namespaceAccessInfoURL}
                target='_blank'
                className={`${this.props.classes.link} ${this.props.classes.alignVertically}`} >
                {popover}
            </Link>;
        } else {
            return popover;
        }
    }

    protected renderLicense(extension: Extension): React.ReactNode {
        if (extension.files.license) {
            return <Link
                    href={extension.files.license}
                    className={this.props.classes.link} >
                {extension.license || 'Provided license'}
            </Link>;
        } else {
            return extension.license || 'Unlicensed';
        }
    }

}

export namespace ExtensionDetailComponent {
    export interface Props extends WithStyles<typeof detailStyles>, RouteComponentProps {
        user?: UserData;
        service: ExtensionRegistryService;
        pageSettings: PageSettings;
    }
    export interface State {
        extension?: Extension;
    }
}

export const ExtensionDetail = withStyles(detailStyles)(ExtensionDetailComponent);
