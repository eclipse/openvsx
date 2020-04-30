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
import { Typography, Box, createStyles, Theme, WithStyles, withStyles, Container, Link, Avatar, Paper } from "@material-ui/core";
import { RouteComponentProps, Switch, Route, Link as RouteLink } from "react-router-dom";
import SaveAltIcon from '@material-ui/icons/SaveAlt';
import VerifiedUserIcon from '@material-ui/icons/VerifiedUser';
import PublicIcon from '@material-ui/icons/Public';
import WarningIcon from '@material-ui/icons/Warning';
import { createRoute } from "../../utils";
import { DelayedLoadIndicator } from "../../custom-mui-components/delayed-load-indicator";
import { ExtensionRegistryService } from "../../extension-registry-service";
import { Extension, UserData, isError, ExtensionRaw } from "../../extension-registry-types";
import { TextDivider } from "../../custom-mui-components/text-divider";
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
        color: '#fff',
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
        maxWidth: '9rem',
        [theme.breakpoints.up('md')]: {
            marginRight: '2rem'
        }
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
    header: {
        display: 'flex',
        flexDirection: 'column',
        padding: `${theme.spacing(4)}px 0`
    },
    iconAndInfo: {
        display: 'flex',
        [theme.breakpoints.down('sm')]: {
            flexDirection: 'column',
            textAlign: 'center',
            alignItems: 'center'
        }
    },
    banner: {
        margin: `0 ${theme.spacing(6)}px ${theme.spacing(4)}px ${theme.spacing(6)}px`,
        padding: theme.spacing(2),
        [theme.breakpoints.down('sm')]: {
            margin: `0 0 ${theme.spacing(2)}px 0`,
        }
    },
    warningLight: {
        backgroundColor: theme.palette.warning.light,
        color: '#000',
        '& > a': {
            color: '#000',
            fontWeight: 'bold'
        }
    },
    warningDark: {
        backgroundColor: theme.palette.warning.dark,
        color: '#fff',
        '& > a': {
            color: '#fff',
            fontWeight: 'bold'
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
        const headerTheme = extension.galleryTheme || this.props.pageSettings.themeType || 'light';

        return <React.Fragment>
            <Box className={this.props.classes.head}
                style={{
                    backgroundColor: extension.galleryColor,
                    color: headerTheme === 'dark' ? '#fff' : '#333'
                }}
            >
                <Container maxWidth='lg'>
                    <Box className={this.props.classes.header}>
                        {this.renderBanner(extension, headerTheme)}
                        <Box className={this.props.classes.iconAndInfo}>
                            <img src={extension.files.icon || this.props.pageSettings.urls.extensionDefaultIcon}
                                className={this.props.classes.extensionLogo}
                                alt={extension.displayName || extension.name} />
                            {this.renderHeaderInfo(extension, headerTheme)}
                        </Box>
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

    protected renderBanner(extension: Extension, themeType: 'light' | 'dark'): React.ReactNode {
        const classes = this.props.classes;
        const warningClass = themeType === 'dark' ? classes.warningDark : classes.warningLight;
        const themeClass = themeType === 'dark' ? classes.darkTheme : classes.lightTheme;
        let extensionName = extension.displayName || extension.name;
        if (!extensionName.toLowerCase().endsWith('extension')) {
            extensionName += ' extension';
        }
        if (extension.namespaceAccess === 'public') {
            return <Paper className={`${classes.banner} ${warningClass} ${themeClass}`}>
                The namespace <span className={classes.code}>{extension.namespace}</span> is public,
                which means that everyone can publish new versions of the {extensionName}.
                If you would like to become the owner of <span className={classes.code}>{extension.namespace}</span>,
                please <Link
                    href={this.props.pageSettings.urls.namespaceAccessInfo}
                    target='_blank'
                    className={`${classes.link}`} >
                    read this guide
                </Link>.
            </Paper>;
        } else if (extension.unrelatedPublisher) {
            return <Paper className={`${classes.banner} ${warningClass} ${themeClass}`}>
                The {extensionName} was published by <Link href={extension.publishedBy.homepage}
                    className={`${classes.link}`}>
                    {extension.publishedBy.loginName}
                </Link>. This user account is not related to
                the namespace <span className={classes.code}>{extension.namespace}</span> of
                this extension. <Link
                    href={this.props.pageSettings.urls.namespaceAccessInfo}
                    target='_blank'
                    className={`${classes.link}`} >
                    See the documentation
                </Link> to learn how we handle namespaces.
            </Paper>;
        }
        return null;
    }

    protected renderHeaderInfo(extension: Extension, themeType: 'light' | 'dark'): React.ReactNode {
        const classes = this.props.classes;
        const themeClass = themeType === 'dark' ? classes.darkTheme : classes.lightTheme;
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
                <TextDivider themeType={themeType}/>
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
                <TextDivider themeType={themeType}/>
                {this.renderLicense(extension, themeClass)}
            </Box>
            <Box mt={2} mb={2} overflow='auto'>
                <Typography classes={{ root: classes.description }}>{extension.description}</Typography>
            </Box>
            <Box className={`${themeClass} ${classes.infoRow} ${classes.alignVertically}`}>
                <SaveAltIcon fontSize='small' />&nbsp;{extension.downloadCount || 0}&nbsp;{extension.downloadCount === 1 ? 'download' : 'downloads'}
                <TextDivider themeType={themeType}/>
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
        let title: string;
        switch (extension.namespaceAccess) {
            case 'public':
                icon = <PublicIcon fontSize='small' />;
                title = 'Public namespace access';
                break;
            case 'restricted':
                if (extension.unrelatedPublisher) {
                    icon = <WarningIcon fontSize='small' />;
                    title = 'Published to a restricted namespace by an unrelated user';
                } else {
                    icon = <VerifiedUserIcon fontSize='small' />;
                    title = 'Restricted namespace access';
                }
                break;
            default:
                return null;
        }
        return <Link
            href={this.props.pageSettings.urls.namespaceAccessInfo}
            target='_blank'
            title={title}
            className={`${this.props.classes.link} ${themeClass}`} >
            {icon}
        </Link>;
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
