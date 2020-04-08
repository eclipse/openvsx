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
import { Typography, Box, createStyles, Theme, WithStyles, withStyles, Container, Link, Avatar } from "@material-ui/core";
import { RouteComponentProps, Switch, Route, Link as RouteLink } from "react-router-dom";
import SaveAltIcon from '@material-ui/icons/SaveAlt';
import VerifiedUserIcon from '@material-ui/icons/VerifiedUser';
import PublicIcon from '@material-ui/icons/Public';
import { handleError, createRoute, addQuery } from "../../utils";
import { DelayedLoadIndicator } from "../../custom-mui-components/delayed-load-indicator";
import { ExtensionRegistryService } from "../../extension-registry-service";
import { Extension, UserData, isError, ExtensionRaw } from "../../extension-registry-types";
import { TextDivider } from "../../custom-mui-components/text-divider";
import { HoverPopover } from "../../custom-mui-components/hover-popover";
import { Optional } from "../../custom-mui-components/optional";
import { PageSettings } from "../../page-settings";
import { ExtensionListRoutes } from "../extension-list/extension-list-container";
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
        '&:hover': {
            textDecoration: 'underline'
        }
    },
    lightTheme: {
        color: theme.palette.primary.contrastText,
    },
    darkTheme: {
        color: theme.palette.secondary.contrastText,
    },
    clickable: {
        cursor: 'pointer'
    },
    titleRow: {
        fontWeight: 'bold',
        marginBottom: theme.spacing(1)
    },
    head: {
        backgroundColor: theme.palette.grey[200]
    },
    alignVertically: {
        display: 'flex',
        alignItems: 'center'
    },
    extensionLogo: {
        height: '7.5rem',
        maxWidth: '9rem'
    },
    preview: {
        fontSize: '0.6em',
        fontStyle: 'italic',
        marginLeft: theme.spacing(3)
    },
    description: {
        overflow: 'hidden',
        textOverflow: 'ellipsis'
    },
    code: {
        fontFamily: 'monospace',
        fontSize: '1rem'
    },
    avatar: {
        width: '20px',
        height: '20px'
    },
    headerWrapper: {
        [theme.breakpoints.down('sm')]: {
            flexDirection: 'column',
            textAlign: 'center',
            fontSize: '85%',
        },
        '& > *': {
            '&:first-child': {
                [theme.breakpoints.up('md')]: {
                    marginRight: '2rem'
                }
            }
        }
    },
    info: {
        [theme.breakpoints.down('sm')]: {
            justifyContent: 'center'
        }
    },
    count: {
        [theme.breakpoints.down('sm')]: {
            justifyContent: 'center'
        }
    }
});

export class ExtensionDetailComponent extends React.Component<ExtensionDetailComponent.Props, ExtensionDetailComponent.State> {

    constructor(props: ExtensionDetailComponent.Props) {
        super(props);
        this.state = { loading: true };
    }

    componentDidMount() {
        const params = this.props.match.params as ExtensionRaw;
        document.title = `${params.name} – ${this.props.pageSettings.pageTitle}`;
        this.updateExtension(params);
    }

    componentDidUpdate(prevProps: ExtensionDetailComponent.Props) {
        const prevParams = prevProps.match.params as ExtensionRaw;
        const newParams = this.props.match.params as ExtensionRaw;
        if (newParams.namespace !== prevParams.namespace || newParams.name !== prevParams.name) {
            this.setState({ extension: undefined, loading: true });
            this.updateExtension(newParams);
        }
    }

    protected async updateExtension(params: ExtensionRaw) {
        try {
            const extensionUrl = this.props.service.getExtensionApiUrl(params);
            const extension = await this.props.service.getExtensionDetail(extensionUrl);
            if (isError(extension)) {
                throw extension;
            }
            document.title = `${extension.displayName || extension.name} – ${this.props.pageSettings.pageTitle}`;
            this.setState({ extension, loading: false });
        } catch (err) {
            handleError(err);
            this.setState({ loading: false });
        }
    }

    protected onReviewUpdate = () => this.updateExtension(this.props.match.params as ExtensionRaw);

    render() {
        const { extension } = this.state;
        if (!extension) {
            return <DelayedLoadIndicator loading={this.state.loading} />;
        }

        return <React.Fragment>
            <Box className={this.props.classes.head} style={{
                backgroundColor: extension.galleryColor,
                color: extension.galleryTheme === 'dark' ? '#ffffff' : undefined
            }} >
                <Container maxWidth='lg'>
                    <Box display='flex' py={4} className={this.props.classes.headerWrapper}>
                        <Box display='flex' justifyContent='center' alignItems='center'>
                            <img src={extension.files.icon || this.props.pageSettings.extensionDefaultIconURL}
                                className={this.props.classes.extensionLogo}
                                alt={extension.displayName || extension.name} />
                        </Box>
                        {this.renderHeader(extension)}
                    </Box>
                </Container>
            </Box>
            <Container maxWidth='lg'>
                <Box>
                    <Box>
                        <ExtensionDetailTabs />
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
        const themeClass = extension.galleryTheme === 'dark' ? this.props.classes.darkTheme : this.props.classes.lightTheme;
        return <Box overflow='auto'>
            <Typography variant='h5' className={this.props.classes.titleRow}>
                {extension.displayName || extension.name} {extension.preview ?
                    <span className={`${this.props.classes.preview} ${themeClass}`}>preview</span>
                    : ''}
            </Typography>
            <Box display='flex' className={`${themeClass} ${this.props.classes.info}`}>
                <Box className={this.props.classes.alignVertically} >
                    {this.renderAccessInfo(extension, themeClass)}&nbsp;<RouteLink
                        to={addQuery(ExtensionListRoutes.MAIN, [{ key: 'search', value: extension.namespace }])}
                        className={`${this.props.classes.link} ${themeClass}`}
                        title={`Namespace: ${extension.namespace}`} >
                        {extension.namespace}
                    </RouteLink>
                </Box>
                <TextDivider theme={extension.galleryTheme} />
                <Box className={this.props.classes.alignVertically}>
                    Published&nbsp;by&nbsp;<Link href={extension.publishedBy.homepage}
                        className={`${this.props.classes.link} ${themeClass} ${this.props.classes.alignVertically}`}>{
                            extension.publishedBy.avatarUrl ?
                                <React.Fragment>
                                    {extension.publishedBy.loginName}&nbsp;<Avatar
                                        src={extension.publishedBy.avatarUrl}
                                        alt={extension.publishedBy.loginName}
                                        variant='circle'
                                        classes={{ root: this.props.classes.avatar }} />
                                </React.Fragment>
                                : extension.publishedBy.loginName
                        }</Link>
                </Box>
                <TextDivider theme={extension.galleryTheme} />
                <Box className={this.props.classes.alignVertically}>{this.renderLicense(extension, themeClass)}</Box>
            </Box>
            <Box mt={2} mb={2} overflow='auto'>
                <Typography classes={{ root: this.props.classes.description }}>{extension.description}</Typography>
            </Box>
            <Box display='flex' className={`${themeClass} ${this.props.classes.count}`}>
                <Box className={this.props.classes.alignVertically}>
                    <SaveAltIcon fontSize='small' />&nbsp;{extension.downloadCount || 0}&nbsp;{extension.downloadCount === 1 ? 'download' : 'downloads'}
                </Box>
                <TextDivider theme={extension.galleryTheme} />
                <RouteLink
                    to={createRoute([ExtensionDetailRoutes.ROOT, extension.namespace, extension.name, ExtensionDetailRoutes.TAB_REVIEWS])}
                    className={`${this.props.classes.link} ${themeClass}`}
                    title={
                        extension.averageRating !== undefined ?
                            `Average rating: ${this.getRoundedRating(extension.averageRating)} out of 5`
                            : 'Not rated yet'
                    }>
                    <Box className={this.props.classes.alignVertically}>
                        <ExportRatingStars number={extension.averageRating || 0} fontSize='small' />
                        ({extension.reviewCount})
                    </Box>
                </RouteLink>
            </Box>
        </Box>;
    }

    protected getRoundedRating(rating: number): number {
        return Math.round(rating * 10) / 10;
    }

    protected renderAccessInfo(extension: Extension, themeClass: string): React.ReactNode {
        let icon: React.ReactElement;
        let description: string;
        switch (extension.namespaceAccess) {
            case 'public':
                icon = <PublicIcon fontSize='small' />;
                description = 'Everyone can publish to this namespace, so the identity of the publisher cannot be verified.';
                break;
            case 'restricted':
                icon = <VerifiedUserIcon fontSize='small' />;
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
                <br />Click on the icon to learn more.
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
                className={`${this.props.classes.link} ${themeClass} ${this.props.classes.alignVertically}`} >
                {popover}
            </Link>;
        } else {
            return popover;
        }
    }

    protected renderLicense(extension: Extension, themeClass: string): React.ReactNode {
        if (extension.files.license) {
            return <Link
                href={extension.files.license}
                className={`${this.props.classes.link} ${themeClass}`} >
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
        loading: boolean;
    }
}

export const ExtensionDetail = withStyles(detailStyles)(ExtensionDetailComponent);
