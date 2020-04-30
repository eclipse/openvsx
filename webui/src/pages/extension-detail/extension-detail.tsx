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
import { createRoute } from "../../utils";
import { DelayedLoadIndicator } from "../../custom-mui-components/delayed-load-indicator";
import { ExtensionRegistryService } from "../../extension-registry-service";
import { Extension, UserData, isError, ExtensionRaw } from "../../extension-registry-types";
import { TextDivider } from "../../custom-mui-components/text-divider";
import { HoverPopover } from "../../custom-mui-components/hover-popover";
import { PageSettings } from "../../page-settings";
import { ExtensionDetailOverview } from "./extension-detail-overview";
import { ExtensionDetailReviews } from "./extension-detail-reviews";
import { ExtensionDetailTabs } from "./extension-detail-tabs";
import { ExportRatingStars } from "./extension-rating-stars";
import { ErrorResponse } from '../../server-request';

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
        display: 'contents',
        cursor: 'pointer',
        textDecoration: 'none',
        '&:hover': {
            textDecoration: 'underline'
        }
    },
    lightTheme: {
        color: '#333',
    },
    darkTheme: {
        color: theme.palette.secondary.contrastText,
    },
    titleRow: {
        fontWeight: 'bold',
        marginBottom: theme.spacing(1)
    },
    infoRow: {
        [theme.breakpoints.down('sm')]: {
            justifyContent: 'center'
        }
    },
    head: {
        backgroundColor: theme.palette.secondary.dark,
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
    alignVertically: {
        display: 'flex',
        alignItems: 'center'
    },
    code: {
        fontFamily: 'Monaco, monospace',
        fontSize: '0.8rem'
    },
    avatar: {
        width: '20px',
        height: '20px'
    },
    headerWrapper: {
        [theme.breakpoints.down('sm')]: {
            flexDirection: 'column',
            textAlign: 'center'
        },
        '& > *': {
            '&:first-child': {
                [theme.breakpoints.up('md')]: {
                    marginRight: '2rem'
                }
            }
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
            this.props.setError(err);
            this.setState({ loading: false });
        }
    }

    protected onReviewUpdate = () => this.updateExtension(this.props.match.params as ExtensionRaw);

    render() {
        const { extension } = this.state;
        if (!extension) {
            return <DelayedLoadIndicator loading={this.state.loading} />;
        }

        const darkTheme = (extension.galleryTheme === 'dark' || (!extension.galleryTheme && this.props.pageSettings.themeType !== 'light'));

        return <React.Fragment>
            <Box className={this.props.classes.head}
                style={{
                    backgroundColor: extension.galleryColor,
                    color: darkTheme ? '#fff' : '#333'
                }}
            >
                <Container maxWidth='lg'>
                    <Box display='flex' py={4} className={this.props.classes.headerWrapper}>
                        <Box display='flex' justifyContent='center' alignItems='center'>
                            <img src={extension.files.icon || this.props.pageSettings.extensionDefaultIconURL}
                                className={this.props.classes.extensionLogo}
                                alt={extension.displayName || extension.name} />
                        </Box>
                        {this.renderHeader(extension, darkTheme)}
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
                                    user={this.props.user}
                                    setError={this.props.setError}
                                />
                            </Route>
                            <Route path={ExtensionDetailRoutes.OVERVIEW}>
                                <ExtensionDetailOverview
                                    extension={extension}
                                    service={this.props.service}
                                    pageSettings={this.props.pageSettings}
                                    setError={this.props.setError}
                                />
                            </Route>
                        </Switch>
                    </Box>
                </Box>
            </Container>
        </React.Fragment>;
    }

    protected renderHeader(extension: Extension, darkTheme: boolean): React.ReactNode {
        const classes = this.props.classes;
        const themeClass = darkTheme ? classes.darkTheme : classes.lightTheme;
        const theme = darkTheme ? 'dark' : 'light';
        return ( 
        <Box overflow='auto'>
            <Typography variant='h5' className={classes.titleRow}>
                {extension.displayName || extension.name} {extension.preview ?
                    <span className={`${classes.preview} ${themeClass}`}>preview</span>
                    : ''}
            </Typography>
            <Box className={`${themeClass} ${classes.infoRow} ${classes.alignVertically}`}>
                {this.renderAccessInfo(extension, themeClass)}&nbsp;<span
                    title='Unique identifier'
                    className={classes.code}>
                    {extension.namespace}.{extension.name}
                </span>
                <TextDivider theme={theme}/>
                Published by <Link href={extension.publishedBy.homepage}
                    className={`${classes.link} ${themeClass}`}>
                    {
                        extension.publishedBy.avatarUrl ?
                        <React.Fragment>
                            {extension.publishedBy.loginName}&nbsp;<Avatar
                                src={extension.publishedBy.avatarUrl}
                                alt={extension.publishedBy.loginName}
                                variant='circle'
                                classes={{ root: classes.avatar }} />
                        </React.Fragment>
                        : extension.publishedBy.loginName
                    }
                </Link>
                <TextDivider theme={theme}/>
                {this.renderLicense(extension, themeClass)}
            </Box>
            <Box mt={2} mb={2} overflow='auto'>
                <Typography classes={{ root: classes.description }}>{extension.description}</Typography>
            </Box>
            <Box className={`${themeClass} ${classes.infoRow} ${classes.alignVertically}`}>
                <SaveAltIcon fontSize='small' />&nbsp;{extension.downloadCount || 0}&nbsp;{extension.downloadCount === 1 ? 'download' : 'downloads'}
                <TextDivider theme={theme}/>
                <RouteLink
                    to={createRoute([ExtensionDetailRoutes.ROOT, extension.namespace, extension.name, ExtensionDetailRoutes.TAB_REVIEWS])}
                    className={`${classes.link} ${themeClass} ${classes.alignVertically}`}
                    title={
                        extension.averageRating !== undefined ?
                            `Average rating: ${this.getRoundedRating(extension.averageRating)} out of 5`
                            : 'Not rated yet'
                    }>
                    <ExportRatingStars number={extension.averageRating || 0} fontSize='small' />
                    ({extension.reviewCount})
                </RouteLink>
                </Box>
            </Box>
        );
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
            {
                this.props.pageSettings.namespaceAccessInfoURL ?
                <React.Fragment><br />Click on the icon to learn more.</React.Fragment>
                : null
            }
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
                className={`${this.props.classes.link} ${themeClass}`} >
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
                className={`${this.props.classes.link} ${themeClass}`}
                title={extension.license ? 'License type' : undefined} >
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
        setError: (err: Error | Partial<ErrorResponse>) => void;
    }
    export interface State {
        extension?: Extension;
        loading: boolean;
    }
}

export const ExtensionDetail = withStyles(detailStyles)(ExtensionDetailComponent);
