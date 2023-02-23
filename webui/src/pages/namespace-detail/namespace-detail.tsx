/********************************************************************************
 * Copyright (c) 2022 Precies. Software and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import * as React from 'react';
import { Typography, Box, createStyles, Theme, WithStyles, withStyles, Container, Grid, Link, Divider } from '@material-ui/core';
import GitHubIcon from '@material-ui/icons/GitHub';
import LinkedInIcon from '@material-ui/icons/LinkedIn';
import TwitterIcon from '@material-ui/icons/Twitter';
import { RouteComponentProps } from 'react-router-dom';
import Truncate from 'react-truncate';
import { ExtensionListItem } from '../extension-list/extension-list-item';
import { MainContext } from '../../context';
import { createRoute } from '../../utils';
import { DelayedLoadIndicator } from '../../components/delayed-load-indicator';
import { NamespaceDetails, isError, UrlString } from '../../extension-registry-types';

export namespace NamespaceDetailRoutes {
    export namespace Parameters {
        export const NAME = ':name';
    }

    export const ROOT = 'namespace';
    export const MAIN = createRoute([ROOT, Parameters.NAME]);
}

const detailStyles = (theme: Theme) => createStyles({
    extensionsContainer: {
        justifyContent: 'center',
        paddingTop: theme.spacing(6)
    },
    head: {
        backgroundColor: theme.palette.neutral.dark,
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
    linksDivider: {
        margin: theme.spacing(0.25),
        width: theme.spacing(0.25)
    },
    namespaceLogo: {
        height: '7.5rem',
        maxWidth: '9rem',
        [theme.breakpoints.up('md')]: {
            marginRight: '2rem'
        }
    },
    badgePadding: {
        paddingTop: theme.spacing(1)
    },
    descriptionPadding: {
        paddingRight: '0 !important'
    }
});

export class NamespaceDetailComponent extends React.Component<NamespaceDetailComponent.Props, NamespaceDetailComponent.State> {

    static contextType = MainContext;
    declare context: MainContext;

    protected abortController = new AbortController();

    constructor(props: NamespaceDetailComponent.Props) {
        super(props);
        this.state = { loading: true, truncateReadMore: true };
    }

    componentDidMount(): void {
        const params = this.props.match.params as NamespaceDetailComponent.Params;
        this.updateNamespaceDetails(params);
    }

    componentDidUpdate(prevProps: NamespaceDetailComponent.Props): void {
        const prevParams = prevProps.match.params as NamespaceDetailComponent.Params;
        const newParams = this.props.match.params as NamespaceDetailComponent.Params;
        if (newParams.name !== prevParams.name) {
            this.setState({ namespaceDetails: undefined, loading: true });
            this.updateNamespaceDetails(newParams);
        }
    }

    protected async updateNamespaceDetails(params: NamespaceDetailComponent.Params): Promise<void> {
        try {
            const namespaceDetails = await this.context.service.getNamespaceDetails(this.abortController, params.name);
            if (isError(namespaceDetails)) {
                throw namespaceDetails;
            }
            this.setState({ namespaceDetails, loading: false, truncateReadMore: true });
        } catch (err) {
            if (err && err.status === 404) {
                this.setState({
                    notFoundError: `Namespace Not Found: ${params.name}`,
                    loading: false
                });
            } else {
                this.context.handleError(err);
            }
            this.setState({ loading: false });
        }
    }

    protected readMore = () => {
        this.setState({ truncateReadMore: false });
    };

    protected displayLink = (link: UrlString) => {
        return link.replace(/https?:\/\//, '');
    };

    render(): React.ReactNode {
        const { namespaceDetails, truncateReadMore } = this.state;
        const params = this.props.match.params as NamespaceDetailComponent.Params;
        return <>
            { this.renderHeaderTags(params, namespaceDetails) }
            <DelayedLoadIndicator loading={this.state.loading} />
            {
                namespaceDetails
                    ? this.renderNamespaceDetails(namespaceDetails, truncateReadMore)
                    : this.renderNotFound()
            }
        </>;
    }

    protected renderHeaderTags(params: NamespaceDetailComponent.Params, namespaceDetails?: NamespaceDetails): React.ReactNode {
        const pageSettings = this.context.pageSettings;
        const { namespaceHeadTags: NamespaceHeadTagsComponent } = pageSettings.elements;
        return <React.Fragment>
            { NamespaceHeadTagsComponent
                ? <NamespaceHeadTagsComponent namespaceDetails={namespaceDetails} params={params} pageSettings={pageSettings}/>
                : null
            }
        </React.Fragment>;
    }

    protected renderNotFound(): React.ReactNode {
        return <>
            {
                this.state.notFoundError ?
                <Box p={4}>
                    <Typography variant='h5'>
                        {this.state.notFoundError}
                    </Typography>
                </Box>
                : null
            }
        </>;
    }

    protected renderNamespaceDetails(namespaceDetails: NamespaceDetails, truncateReadMore: boolean): React.ReactNode {
        const classes = this.props.classes;
        return <>
            <Box className={classes.head}>
                <Container maxWidth='xl'>
                    <Box className={classes.header}>
                        <Grid container>
                            <Grid item>
                                <img src={namespaceDetails.logo || this.context.pageSettings.urls.extensionDefaultIcon}
                                    className={`${classes.namespaceLogo} ${classes.badgePadding}`}
                                    alt={namespaceDetails.displayName || namespaceDetails.name} />
                            </Grid>
                            <Grid item xs={7}>
                                <Grid container spacing={2}>
                                    <Grid item>
                                        <Typography variant='h5'>{namespaceDetails.displayName || namespaceDetails.name}</Typography>
                                    </Grid>
                                </Grid>
                                <Grid container spacing={2}>
                                    <Grid item className={classes.descriptionPadding}>
                                        {
                                            namespaceDetails.description
                                            ? truncateReadMore
                                              ? <Truncate lines={2} ellipsis={<span>... <Link color='secondary' onClick={this.readMore}>Read more</Link></span>}>
                                                { namespaceDetails.description }
                                              </Truncate>
                                              : <Typography>{ namespaceDetails.description }</Typography>
                                            : null
                                        }
                                    </Grid>
                                </Grid>
                                <Grid container spacing={2}>
                                    {
                                        namespaceDetails.website
                                        ? <Grid item><Link color='secondary' target='_blank' href={namespaceDetails.website}>{this.displayLink(namespaceDetails.website)}</Link></Grid>
                                        : null
                                    }
                                    {
                                        namespaceDetails.website && namespaceDetails.supportLink ? <Divider className={classes.linksDivider} orientation='vertical' flexItem /> : null
                                    }
                                    {
                                        namespaceDetails.supportLink
                                        ? <Grid item><Link color='secondary' target='_blank' href={namespaceDetails.supportLink}>{this.displayLink(namespaceDetails.supportLink)}</Link></Grid>
                                        : null
                                    }
                                </Grid>
                                <Grid container spacing={2}>
                                    {
                                        namespaceDetails.socialLinks.linkedin
                                        ? <Grid item><Link target='_blank' color='textPrimary' href={namespaceDetails.socialLinks.linkedin}><LinkedInIcon/></Link></Grid>
                                        : null
                                    }
                                    {
                                        namespaceDetails.socialLinks.github
                                        ? <Grid item><Link target='_blank' color='textPrimary' href={namespaceDetails.socialLinks.github}><GitHubIcon/></Link></Grid>
                                        : null
                                    }
                                    {
                                        namespaceDetails.socialLinks.twitter
                                        ? <Grid item><Link target='_blank' color='textPrimary' href={namespaceDetails.socialLinks.twitter}><TwitterIcon/></Link></Grid>
                                        : null
                                    }
                                </Grid>
                            </Grid>
                        </Grid>
                    </Box>
                </Container>
            </Box>
            { namespaceDetails.extensions ?
                <Container maxWidth='xl'>
                    <Grid container spacing={2} className={classes.extensionsContainer}>
                        {
                            namespaceDetails.extensions.map((ext, idx) => (
                                <ExtensionListItem
                                    idx={idx}
                                    extension={ext}
                                    filterSize={10}
                                    pageSettings={this.context.pageSettings}
                                    key={`${ext.namespace}.${ext.name}`} />
                            ))
                        }
                    </Grid>
                </Container>
                : null
            }
        </>;
    }
}

export namespace NamespaceDetailComponent {
    export interface Props extends WithStyles<typeof detailStyles>, RouteComponentProps {
    }

    export interface State {
        namespaceDetails?: NamespaceDetails;
        truncateReadMore: boolean;
        loading: boolean;
        notFoundError?: string;
    }

    export interface Params {
        readonly name: string;
    }
}

export const NamespaceDetail = withStyles(detailStyles)(NamespaceDetailComponent);