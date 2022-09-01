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
import { Box, withStyles, Theme, createStyles, WithStyles, Typography, Button, Link, NativeSelect } from '@material-ui/core';
import { RouteComponentProps, Link as RouteLink, withRouter } from 'react-router-dom';
import HomeIcon from '@material-ui/icons/Home';
import GitHubIcon from '@material-ui/icons/GitHub';
import BugReportIcon from '@material-ui/icons/BugReport';
import QuestionAnswerIcon from '@material-ui/icons/QuestionAnswer';
import { MainContext } from '../../context';
import { addQuery, createRoute, getTargetPlatformDisplayName } from '../../utils';
import { DelayedLoadIndicator } from '../../components/delayed-load-indicator';
import { SanitizedMarkdown } from '../../components/sanitized-markdown';
import { Timestamp } from '../../components/timestamp';
import { Extension, ExtensionReference, VERSION_ALIASES } from '../../extension-registry-types';
import { ExtensionListRoutes } from '../extension-list/extension-list-container';
import { ExtensionDetailComponent, ExtensionDetailRoutes } from './extension-detail';
import { ExtensionDetailDownloadsMenu } from './extension-detail-downloads-menu';
import { UrlString } from '../..';

const overviewStyles = (theme: Theme) => createStyles({
    overview: {
        display: 'flex',
        marginTop: theme.spacing(2),
        [theme.breakpoints.down('lg')]: {
            flexDirection: 'column-reverse',
        }
    },
    resourcesWrapper: {
        flex: 1,
        display: 'flex',
        width: '100%',
        minWidth: '290px',
        margin: '0 0 0 4.8rem',
        [theme.breakpoints.down('lg')]: {
            margin: `0 0 ${theme.spacing(2)}px 0`
        },
        [theme.breakpoints.up('xl')]: {
            flexDirection: 'column'
        },
        [theme.breakpoints.down('sm')]: {
            flexDirection: 'column'
        }
    },
    resourcesGroup: {
        display: 'flex',
        flexDirection: 'column',
        flex: 1,
        [theme.breakpoints.up('xl')]: {
            flex: 'none',
            marginBottom: theme.spacing(2)
        },
        [theme.breakpoints.down('sm')]: {
            flex: 'none',
            marginBottom: theme.spacing(2)
        }
    },
    link: {
        textDecoration: 'none',
        color: theme.palette.primary.contrastText,
        '&:hover': {
            textDecoration: 'underline'
        }
    },
    resourceLink: {
        display: 'flex',
        alignItems: 'center',
        marginTop: theme.spacing(0.5)
    },
    versionAlias: {
        color: theme.palette.primary.dark,
        fontWeight: theme.typography.fontWeightBold
    },
    preReleaseFlag: {
        color: theme.palette.primary.dark,
        fontStyle: 'italic',
        marginLeft: theme.spacing(2),
        padding: '4px'
    },
    tagButton: {
        fontWeight: 'normal',
        textTransform: 'none',
        marginRight: theme.spacing(0.5),
        marginBottom: theme.spacing(0.5),
        padding: '1px 6px'
    },
    downloadButton: {
        marginTop: theme.spacing(2)
    },
    moreInfo: {
        maxWidth: '30rem',
    }
});

class ExtensionDetailOverviewComponent extends React.Component<ExtensionDetailOverview.Props, ExtensionDetailOverview.State> {

    static contextType = MainContext;
    declare context: MainContext;

    constructor(props: ExtensionDetailOverview.Props) {
        super(props);
        this.state = { loading: true };
    }

    componentDidMount(): void {
        this.updateReadme();
    }

    componentDidUpdate(prevProps: ExtensionDetailOverview.Props) {
        const prevExt = prevProps.extension;
        const newExt = this.props.extension;
        if (prevExt.namespace !== newExt.namespace || prevExt.name !== newExt.name || prevExt.version !== newExt.version) {
            this.setState({ loading: true });
            this.updateReadme();
        }
    }

    protected async updateReadme(): Promise<void> {
        if (this.props.extension.files.readme) {
            try {
                const readme = await this.context.service.getExtensionReadme(this.props.extension);
                this.setState({ readme, loading: false });
            } catch (err) {
                this.context.handleError(err);
                this.setState({ loading: false });
            }
        } else {
            this.setState({ readme: '## No README available', loading: false });
        }
    }

    render() {
        if (!this.state.readme) {
            return <DelayedLoadIndicator loading={this.state.loading} />;
        }
        const { classes, extension } = this.props;
        const ClaimNamespace = this.context.pageSettings.elements.claimNamespace;
        const ReportAbuse = this.context.pageSettings.elements.reportAbuse;
        const otherAliases = Object.keys(extension.allVersions)
            .filter(version => extension.versionAlias.indexOf(version) < 0 && VERSION_ALIASES.indexOf(version) >= 0);
        // filter internal tags
        const tags = extension.tags?.filter(t => !t.startsWith('__'));
        return <React.Fragment>
            <Box className={classes.overview}>
                <Box flex={5} overflow='auto'>
                    <SanitizedMarkdown content={this.state.readme} />
                </Box>
                <Box className={classes.resourcesWrapper}>
                    <Box className={classes.resourcesGroup}>
                        <Box>
                            {this.renderVersionSection()}
                        </Box>
                        {
                            (otherAliases.length || extension.versionAlias.length) ? <Box>{this.renderAliasesSection(otherAliases)}</Box> : ''
                        }
                    </Box>
                    <Box className={classes.resourcesGroup}>
                        {
                            extension.categories && extension.categories.length > 0 ?
                            <Box>
                                {this.renderButtonList('category', 'Categories', extension.categories)}
                            </Box>
                            : null
                        }
                        {
                            tags && tags.length > 0 ?
                            <Box mt={2}>
                                {this.renderButtonList('search', 'Tags', tags)}
                            </Box>
                            : null
                        }
                    </Box>
                    {
                        extension.downloads ?
                        <Box className={classes.resourcesGroup}>
                            <Box>
                                <Typography variant='h6'>Works With</Typography>
                                {this.renderWorksWithList(extension.downloads)}
                            </Box>
                        </Box>
                        : null
                    }
                    <Box className={classes.resourcesGroup}>
                        <Box>
                            <Typography variant='h6'>Resources</Typography>
                            {this.renderResourceLink('Homepage', extension.homepage)}
                            {this.renderResourceLink('Repository', extension.repository)}
                            {this.renderResourceLink('Bugs', extension.bugs)}
                            {this.renderResourceLink('Q\'n\'A', extension.qna)}
                            {
                                extension.downloads && Object.keys(extension.downloads).length > 1 ?
                                <ExtensionDetailDownloadsMenu downloads={extension.downloads}/>
                                : extension.downloads && Object.keys(extension.downloads).length == 1 ?
                                <Button variant='contained' color='secondary'
                                    href={extension.downloads[Object.keys(extension.downloads)[0]]}
                                    className={classes.downloadButton}>
                                    Download
                                </Button>
                                : null
                            }
                        </Box>
                        {
                            extension.bundledExtensions !== undefined && extension.bundledExtensions.length > 0 ?
                            <Box mt={2}>
                                <Typography variant='h6'>Bundled Extensions</Typography>
                                {extension.bundledExtensions!.map(ref => this.renderExtensionRef(ref))}
                            </Box>
                            : null
                        }
                        {
                            extension.dependencies !== undefined && extension.dependencies.length > 0 ?
                            <Box mt={2}>
                                <Typography variant='h6'>Dependencies</Typography>
                                {extension.dependencies!.map(ref => this.renderExtensionRef(ref))}
                            </Box>
                            : null
                        }
                        <Box mt={2}>
                            {ClaimNamespace ? <ClaimNamespace extension={extension} className={classes.resourceLink} /> : ''}
                            {ReportAbuse ? <ReportAbuse extension={extension} className={classes.resourceLink} /> : ''}
                        </Box>
                    </Box>
                </Box>
            </Box>
        </React.Fragment>;
    }

    protected renderVersionSection(): React.ReactNode {
        const { classes, extension } = this.props;
        const allVersions = Object.keys(extension.allVersions)
            .filter(version => VERSION_ALIASES.indexOf(version) < 0);
        return <React.Fragment>
            <Typography variant='h6'>Version</Typography>
            {
                allVersions.length === 1 ?
                <Typography variant='body1' display='inline'>{allVersions[0]}</Typography>
                :
                <NativeSelect
                    name='Version'
                    value={extension.version}
                    onChange={event => this.props.selectVersion(event.target.value)}
                    inputProps={{ 'aria-label': 'Version' }} >
                    {
                        allVersions.map(version => <option key={version}>{version}</option>)
                    }
                </NativeSelect>
            }
            {
                extension.preRelease ?
                    <span className={classes.preReleaseFlag}>(pre-release version)</span>
                    : ''
            }
            {
                extension.timestamp ?
                <Box mt={1} mb={1}>
                    Published <Timestamp value={extension.timestamp} />
                </Box>
                : null
            }
        </React.Fragment>;
    }

    protected renderAliasesSection(otherAliases: string[]): React.ReactNode {
        const { classes, extension, params } = this.props;
        const aliasButtons = otherAliases.length ?
            otherAliases.map(alias => {
                const arr = [ExtensionDetailRoutes.ROOT, extension.namespace, extension.name];
                if (params.target) {
                    arr.push(params.target);
                }
                if (alias !== 'latest') {
                    arr.push(alias);
                }

                const route = createRoute(arr);
                return <Button
                    className={classes.tagButton}
                    size='small'
                    variant='outlined'
                    key={alias}
                    title={`Switch to version with "${alias}" alias`}
                    onClick={() => this.props.history.push(route)} >
                    {alias}
                </Button>;
            }) : '';
        return <React.Fragment>
            <Typography variant='h6'>Version Alias{extension.versionAlias.length > 1 ? 'es' : ''}</Typography>
            {
                extension.versionAlias.map((alias, idx) =>
                    <span key={alias} className={classes.versionAlias}>{idx > 0 ? ', ' : ''}{alias}</span>
                )
            }
            {
                aliasButtons ? <>
                    {extension.versionAlias.length > 0 ? ' ' : ''}Switch to {aliasButtons}
                </> : ''
            }
        </React.Fragment>;
    }

    protected renderButtonList(kind: 'category' | 'search', title: string, arr: string[]): React.ReactNode {
        const filtered = Array.from(new Set(arr)).sort((a, b) => a.localeCompare(b));
        return <React.Fragment>
            <Typography variant='h6'>{title}</Typography>
            {
                filtered.map((buttonLabel: string) =>
                    <Button
                        className={this.props.classes.tagButton}
                        size='small'
                        variant='outlined'
                        key={buttonLabel}
                        title={
                            kind === 'category'
                            ? `Search for extensions in "${buttonLabel}" category`
                            : `Search for extensions containing "${buttonLabel}"`
                        }
                        onClick={() => {
                            const route = addQuery(ExtensionListRoutes.MAIN, [{ key: kind, value: buttonLabel }]);
                            this.props.history.push(route);
                        }} >
                        {buttonLabel}
                    </Button>)
            }
        </React.Fragment>;
    }

    protected renderWorksWithList(downloads: {[targetPlatform: string]: UrlString}): React.ReactNode {
        return Object.keys(downloads).map((targetPlatform, index) => {
            const displayName = getTargetPlatformDisplayName(targetPlatform);
            return displayName ? <span key={targetPlatform}>{index > 0 ? ', ' : ''}{displayName}</span> : null;
        });
    }

    protected renderResourceLink(label: string, href?: string): React.ReactNode {
        if (!href || !(href.startsWith('http') || href.startsWith('mailto'))) {
            return '';
        }
        let icon: React.ReactNode;
        if (label === 'Homepage') {
            icon = <HomeIcon fontSize='small' />;
        } else if (label === 'Repository' && href.startsWith('https://github.com/')) {
            icon = <GitHubIcon fontSize='small' />;
        } else if (label === 'Bugs') {
            icon = <BugReportIcon fontSize='small' />;
        } else if (label === 'Q\'n\'A') {
            icon = <QuestionAnswerIcon fontSize='small' />;
        }
        return <Box>
            <Link href={href} target='_blank' variant='body2' color='secondary'
                className={this.props.classes.resourceLink} >
                {icon}&nbsp;{label}
            </Link>
        </Box>;
    }

    protected renderExtensionRef(ref: ExtensionReference): React.ReactNode {
        return <Box key={`${ref.namespace}.${ref.extension}`}>
            <RouteLink to={createRoute([ExtensionDetailRoutes.ROOT, ref.namespace, ref.extension])}
                className={this.props.classes.link} >
                {ref.namespace}.{ref.extension}
            </RouteLink>
        </Box>;
    }

    protected renderInfo(key: string, value: React.ReactNode): React.ReactNode {
        return <Box display='flex'>
            <Box flex='1'>
                <Typography variant='body2'>{key}</Typography>
            </Box>
            <Box flex='1'>
                <Typography variant='body2'>{value}</Typography>
            </Box>
        </Box>;
    }

}

export namespace ExtensionDetailOverview {
    export interface Props extends WithStyles<typeof overviewStyles>, RouteComponentProps {
        params: ExtensionDetailComponent.Params;
        extension: Extension;
        selectVersion: (version: string) => void;
    }
    export interface State {
        readme?: string;
        loading: boolean;
    }
}

export const ExtensionDetailOverview = withStyles(overviewStyles)(withRouter(ExtensionDetailOverviewComponent));
