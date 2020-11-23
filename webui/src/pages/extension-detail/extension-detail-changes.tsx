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
import { Box, Divider, Typography, withStyles, Theme, createStyles, WithStyles } from '@material-ui/core';
import { RouteComponentProps, withRouter } from 'react-router-dom';
import { MainContext } from '../../context';
import { SanitizedMarkdown } from '../../components/sanitized-markdown';
import { DelayedLoadIndicator } from '../../components/delayed-load-indicator';
import { Extension } from '../../extension-registry-types';

const changesStyles = (theme: Theme) => createStyles({
    changes: {
        display: 'flex',
        marginTop: theme.spacing(2),
        [theme.breakpoints.down('lg')]: {
            flexDirection: 'column-reverse',
        }
    },
    link: {
        textDecoration: 'none',
        color: theme.palette.primary.contrastText,
        '&:hover': {
            textDecoration: 'underline'
        }
    },
    header: {
        display: 'flex',
        justifyContent: 'space-between',
        ['@media(max-width: 360px)']: {
            flexDirection: 'column',
            '& > div:first-of-type': {
                marginBottom: '1rem'
            },
            '& button': {
                maxWidth: '12rem',
            }
        },
    }
});

class ExtensionDetailChangesComponent extends React.Component<ExtensionDetailChanges.Props, ExtensionDetailChanges.State> {

    static contextType = MainContext;
    declare context: MainContext;

    constructor(props: ExtensionDetailChanges.Props) {
        super(props);
        this.state = { loading: true };
    }

    componentDidMount(): void {
        this.updateChanges();
    }

    componentDidUpdate(prevProps: ExtensionDetailChanges.Props) {
        const prevExt = prevProps.extension;
        const newExt = this.props.extension;
        if (prevExt.namespace !== newExt.namespace || prevExt.name !== newExt.name || prevExt.version !== newExt.version) {
            this.setState({ loading: true });
            this.updateChanges();
        }
    }

    protected async updateChanges(): Promise<void> {
        if (this.props.extension.files.changelog) {
            try {
                const changelog = await this.context.service.getExtensionChangelog(this.props.extension);
                this.setState({ changelog, loading: false });
            } catch (err) {
                this.context.handleError(err);
                this.setState({ loading: false });
            }
        } else {
            this.setState({ changelog: "", loading: false });
        }
    }

    render() {
        if (typeof this.state.changelog === 'undefined') {
            return <DelayedLoadIndicator loading={this.state.loading} />;
        }
        if (this.state.changelog.length === 0) {
            return <React.Fragment>
                <Box className={this.props.classes.header} my={2}>
                    <Typography variant='h5'>
                        Changelog
                    </Typography>
                </Box>
                <Divider />
                <Box mt={3}>
                    <Typography>No changelog available</Typography>
                </Box>
            </React.Fragment>;
        }
        return <React.Fragment>
            <Box className={this.props.classes.changes}>
                <Box flex={5} overflow='auto'>
                    <SanitizedMarkdown content={this.state.changelog} />
                </Box>
            </Box>
        </React.Fragment>;
    }

}

export namespace ExtensionDetailChanges {
    export interface Props extends WithStyles<typeof changesStyles>, RouteComponentProps {
        extension: Extension;
    }
    export interface State {
        changelog?: string;
        loading: boolean;
    }
}

export const ExtensionDetailChanges = withStyles(changesStyles)(withRouter(ExtensionDetailChangesComponent));
