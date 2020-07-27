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
import { Typography, Box, createStyles, Theme, WithStyles, withStyles, Container, Link, Avatar, Paper } from '@material-ui/core';
import { RouteComponentProps, Switch, Route, Link as RouteLink } from 'react-router-dom';
import SaveAltIcon from '@material-ui/icons/SaveAlt';
import VerifiedUserIcon from '@material-ui/icons/VerifiedUser';
import PublicIcon from '@material-ui/icons/Public';
import WarningIcon from '@material-ui/icons/Warning';
import { createRoute } from '../../utils';
import { DelayedLoadIndicator } from '../../components/delayed-load-indicator';
import { HoverPopover } from '../../components/hover-popover';
import { ExtensionRegistryService } from '../../extension-registry-service';
import { Extension, UserData, isError } from '../../extension-registry-types';
import { TextDivider } from '../../components/text-divider';
import { PageSettings } from '../../page-settings';
import { ExtensionDetailOverview } from './extension-detail-overview';
import { ExtensionDetailReviews } from './extension-detail-reviews';
import { ExtensionDetailTabs } from './extension-detail-tabs';
import { ExportRatingStars } from './extension-rating-stars';
import { ErrorResponse } from '../../server-request';

export namespace ExtensionDetailRoutes {
    export namespace Parameters {
        export const NAMESPACE = ':namespace';
        export const NAME = ':name';
        export const VERSION = ':version?';
    }

    export const ROOT = 'extension';
    export const MAIN = createRoute([ROOT, Parameters.NAMESPACE, Parameters.NAME, Parameters.VERSION]);
    export const LATEST = createRoute([ROOT, Parameters.NAMESPACE, Parameters.NAME]);
    export const PREVIEW = createRoute([ROOT, Parameters.NAMESPACE, Parameters.NAME, 'preview']);
    export const REVIEWS = createRoute([ROOT, Parameters.NAMESPACE, Parameters.NAME, 'reviews']);
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
    infoRowBreak: {
        [theme.breakpoints.down('sm')]: {
            flexDirection: 'column'
        }
    },
    infoRowNonBreak: {
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
    avatarPopover: {
        width: '60px',
        height: '60px'
    },
    header: {
        display: 'flex',
        alignItems: 'center',
        flexDirection: 'column',
        padding: `${theme.spacing(4)}px 0`
    },
    iconAndInfo: {
        display: 'flex',
        width: '100%',
        [theme.breakpoints.down('sm')]: {
            flexDirection: 'column',
            textAlign: 'center',
            alignItems: 'center'
        }
    },
    banner: {
        maxWidth: '800px',
        margin: `0 ${theme.spacing(6)}px ${theme.spacing(4)}px ${theme.spacing(6)}px`,
        padding: theme.spacing(2),
        display: 'flex',
        [theme.breakpoints.down('sm')]: {
            margin: `0 0 ${theme.spacing(2)}px 0`,
        }
    },
    warningLight: {
        backgroundColor: theme.palette.warning.light,
        color: '#000',
        '& a': {
            color: '#000',
            fontWeight: 'bold'
        }
    },
    warningDark: {
        backgroundColor: theme.palette.warning.dark,
        color: '#fff',
        '& a': {
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

    componentDidMount(): void {
        const params = this.props.match.params as ExtensionDetailComponent.Params;
        document.title = `${params.name} – ${this.props.pageSettings.pageTitle}`;
        this.updateExtension(this.getExtensionApiUrl(params));
    }

    componentDidUpdate(prevProps: ExtensionDetailComponent.Props): void {
        const prevParams = prevProps.match.params as ExtensionDetailComponent.Params;
        const newParams = this.props.match.params as ExtensionDetailComponent.Params;
        if (newParams.namespace !== prevParams.namespace || newParams.name !== prevParams.name
                || newParams.version !== prevParams.version) {
            if (newParams.namespace === prevParams.namespace && newParams.name === prevParams.name) {
                this.setState({ loading: true });
            } else {
                this.setState({ extension: undefined, loading: true });
            }
            this.updateExtension(this.getExtensionApiUrl(newParams));
        }
    }

    protected getExtensionApiUrl(params: ExtensionDetailComponent.Params): string {
        if (params.version === 'reviews') {
            return this.props.service.getExtensionApiUrl({ namespace: params.namespace, name: params.name });
        }
        return this.props.service.getExtensionApiUrl(params);
    }

    protected async updateExtension(extensionUrl: string): Promise<void> {
        try {
            const extension = await this.props.service.getExtensionDetail(extensionUrl);
            if (isError(extension)) {
                throw extension;
            }
            document.title = `${extension.displayName || extension.name} – ${this.props.pageSettings.pageTitle}`;
            this.setState({ extension, loading: false });
        } catch (err) {
            this.props.handleError(err);
            this.setState({ loading: false });
        }
    }

    protected onReviewUpdate = (): void => {
        const params = this.props.match.params as ExtensionDetailComponent.Params;
        const extensionUrl = this.getExtensionApiUrl(params);
        this.updateExtension(extensionUrl);
    };
    protected onVersionSelect = (version: string): void => {
        const params = this.props.match.params as ExtensionDetailComponent.Params;
        let newRoute: string;
        if (version === 'latest') {
            newRoute = createRoute([ExtensionDetailRoutes.ROOT, params.namespace, params.name]);
        } else {
            newRoute = createRoute([ExtensionDetailRoutes.ROOT, params.namespace, params.name, version]);
        }
        this.props.history.push(newRoute);
    };

    render(): React.ReactNode {
        const { extension } = this.state;
        if (!extension) {
            return <DelayedLoadIndicator loading={this.state.loading} />;
        }
        const classes = this.props.classes;
        const headerTheme = extension.galleryTheme || this.props.pageSettings.themeType || 'light';

        return <React.Fragment>
            <DelayedLoadIndicator loading={this.state.loading} />
            <Box className={classes.head}
                style={{
                    backgroundColor: extension.galleryColor,
                    color: headerTheme === 'dark' ? '#fff' : '#333'
                }}
            >
                <Container maxWidth='xl'>
                    <Box className={classes.header}>
                        {this.renderBanner(extension, headerTheme)}
                        <Box className={classes.iconAndInfo}>
                            <img src={extension.files.icon || this.props.pageSettings.urls.extensionDefaultIcon}
                                className={classes.extensionLogo}
                                alt={extension.displayName || extension.name} />
                            {this.renderHeaderInfo(extension, headerTheme)}
                        </Box>
                    </Box>
                </Container>
            </Box>
            <Container maxWidth='xl'>
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
                                    handleError={this.props.handleError}
                                />
                            </Route>
                            <Route path={ExtensionDetailRoutes.LATEST}>
                                <ExtensionDetailOverview
                                    extension={extension}
                                    service={this.props.service}
                                    pageSettings={this.props.pageSettings}
                                    selectVersion={this.onVersionSelect}
                                    handleError={this.props.handleError}
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
        if (extension.namespaceAccess === 'public') {
            return <Paper className={`${classes.banner} ${warningClass} ${themeClass}`}>
                <PublicIcon fontSize='large' />
                <Box ml={1}>
                    The namespace <span className={classes.code}>{extension.namespace}</span> is public,
                    which means that everyone can publish new versions of
                    the &ldquo;{extension.displayName || extension.name}&rdquo; extension.
                    If you would like to become the owner of <span className={classes.code}>{extension.namespace}</span>,
                    please <Link
                        href={this.props.pageSettings.urls.namespaceAccessInfo}
                        target='_blank'
                        className={`${classes.link}`} >
                        read this guide
                    </Link>.
                </Box>
            </Paper>;
        } else if (extension.unrelatedPublisher) {
            return <Paper className={`${classes.banner} ${warningClass} ${themeClass}`}>
                <WarningIcon fontSize='large' />
                <Box ml={1}>
                    The &ldquo;{extension.displayName || extension.name}&rdquo; extension was published
                    by <Link href={extension.publishedBy.homepage}
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
                </Box>
            </Paper>;
        }
        return null;
    }

    protected renderHeaderInfo(extension: Extension, themeType: 'light' | 'dark'): React.ReactNode {
        const classes = this.props.classes;
        const themeClass = themeType === 'dark' ? classes.darkTheme : classes.lightTheme;
        const numberFormat = new Intl.NumberFormat(undefined, { notation: 'compact', compactDisplay: 'short' } as any);
        const downloadCountFormatted = numberFormat.format(extension.downloadCount || 0);
        const reviewCountFormatted = numberFormat.format(extension.reviewCount || 0);
        return (
        <Box overflow='auto'>
            <Typography variant='h5' className={classes.titleRow}>
                {extension.displayName || extension.name}
            </Typography>
            <Box className={`${themeClass} ${classes.infoRowBreak} ${classes.alignVertically}`}>
                <Box className={classes.alignVertically}>
                    {this.renderAccessInfo(extension, themeClass)}&nbsp;<span
                        title='Unique identifier'
                        className={classes.code}>
                        {extension.namespace}.{extension.name}
                    </span>
                </Box>
                <TextDivider themeType={themeType} collapseSmall={true} />
                <Box className={classes.alignVertically}>
                    Published by&nbsp;{this.renderUser(extension.publishedBy, themeClass)}
                </Box>
                <TextDivider themeType={themeType} collapseSmall={true} />
                <Box className={classes.alignVertically}>
                    {this.renderLicense(extension, themeClass)}
                </Box>
            </Box>
            <Box mt={2} mb={2} overflow='auto'>
                <Typography classes={{ root: classes.description }}>{extension.description}</Typography>
            </Box>
            <Box className={`${themeClass} ${classes.infoRowNonBreak} ${classes.alignVertically}`}>
                <span className={classes.alignVertically}
                    title={extension.downloadCount && extension.downloadCount >= 1000 ? `${extension.downloadCount} downloads` : undefined}>
                    <SaveAltIcon fontSize='small' />&nbsp;{downloadCountFormatted}&nbsp;{extension.downloadCount === 1 ? 'download' : 'downloads'}
                </span>
                <TextDivider themeType={themeType} />
                <RouteLink
                    to={createRoute([ExtensionDetailRoutes.ROOT, extension.namespace, extension.name, 'reviews'])}
                    className={`${classes.link} ${themeClass} ${classes.alignVertically}`}
                    title={
                        extension.averageRating !== undefined ?
                            `Average rating: ${this.getRoundedRating(extension.averageRating)} out of 5 (${extension.reviewCount} reviews)`
                            : 'Not rated yet'
                    }>
                    <ExportRatingStars number={extension.averageRating || 0} fontSize='small' />
                    ({reviewCountFormatted})
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

    protected renderUser(user: UserData, themeClass: string): React.ReactNode {
        const popupContent = <Box display='flex' flexDirection='row'>
            {
                user.avatarUrl ?
                <Avatar
                    src={user.avatarUrl}
                    alt={user.fullName || user.loginName}
                    variant='rounded'
                    classes={{ root: this.props.classes.avatarPopover }} />
                : null
            }
            <Box ml={2}>
                {
                    user.fullName ?
                    <Typography variant='h6'>{user.fullName}</Typography>
                    : null
                }
                <Typography variant='body1'>{user.loginName}</Typography>
            </Box>
        </Box>;
        return <HoverPopover
            id={`user_${user.loginName}_popover`}
            popupContent={popupContent}
            className={this.props.classes.alignVertically} >
            <Link href={user.homepage}
                className={`${this.props.classes.link} ${themeClass}`}>
                {
                    user.avatarUrl ?
                    <React.Fragment>
                        {user.loginName}&nbsp;<Avatar
                            src={user.avatarUrl}
                            alt={user.loginName}
                            variant='circle'
                            classes={{ root: this.props.classes.avatar }} />
                    </React.Fragment>
                    : user.loginName
                }
            </Link>
        </HoverPopover>;
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
        handleError: (err: Error | Partial<ErrorResponse>) => void;
    }

    export interface State {
        extension?: Extension;
        loading: boolean;
    }

    export interface Params {
        readonly namespace: string;
        readonly name: string;
        readonly version?: string;
    }
}

export const ExtensionDetail = withStyles(detailStyles)(ExtensionDetailComponent);
