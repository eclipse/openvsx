/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import React, { FunctionComponent, useState, useEffect } from 'react';
import { Extension, VERSION_ALIASES } from '../../extension-registry-types';
import { Grid, makeStyles, Typography, FormControl, FormGroup, FormControlLabel, Checkbox } from '@material-ui/core';
import { ExtensionRemoveDialog } from './extension-remove-dialog';

const useStyles = makeStyles((theme) => ({
    extensionLogo: {
        height: '7.5rem',
        maxWidth: '9rem',
    },
    description: {
        overflow: 'hidden',
        textOverflow: 'ellipsis'
    },
    code: {
        fontFamily: 'Monaco, monospace'
    },
    titleRow: {
        fontWeight: 'bold'
    }
}));

interface ExtensionVersionContainerProps {
    extension: Extension;
    onUpdate: () => void;
}

export const ExtensionVersionContainer: FunctionComponent<ExtensionVersionContainerProps> = props => {
    const { extension } = props;
    const classes = useStyles();


    const getVersions = () => {
        const versionMap = new Map<string, boolean>();
        Object.keys(extension.allVersions).forEach(version => {
            versionMap.set(version, false);
        });
        return versionMap;
    };

    const [versions, setVersions] = useState(getVersions());
    const [allChecked, setAllChecked] = useState(false);

    useEffect(() => {
        setVersions(getVersions());
    }, [props.extension]);

    const handleChange = (event: React.ChangeEvent<HTMLInputElement>) => {
        const newVersionMap = new Map<string, boolean>();
        versions.forEach((checked, version) => {
            if (version === event.target.name) {
                checked = event.target.checked;
                if (allChecked && !checked) {
                    setAllChecked(false);
                }
            } else if (event.target.name === 'checkAll') {
                checked = event.target.checked;
                setAllChecked(event.target.checked);
            }
            newVersionMap.set(version, checked);
        });
        setVersions(newVersionMap);
    };

    const handleUpdate = () => {
        props.onUpdate();
    };

    return <>
        <Grid container direction='column'>
            <Grid item container>
                {
                    extension.files.icon ?
                        <Grid item xs={12} md={4}>
                            <img src={extension.files.icon}
                                className={classes.extensionLogo}
                                alt={extension.displayName || extension.name} />
                        </Grid>
                        : ''
                }
                <Grid item container xs={12} md={8}>
                    <Grid item container direction='column' justify='center'>
                        <Grid item>
                            <Typography variant='h5' className={classes.titleRow}>
                                {extension.displayName || extension.name}
                            </Typography>
                        </Grid>
                        <Grid item>
                            <Typography className={classes.code}>{extension.namespace}.{extension.name}</Typography>
                        </Grid>
                        <Grid item>
                            <Typography classes={{ root: classes.description }}>{extension.description}</Typography>
                        </Grid>
                    </Grid>
                </Grid>
            </Grid>
            <Grid item container>
                <Grid item xs={12} md={4}></Grid>
                <Grid item container xs={12} md={8} direction='column'>
                    <FormControl component='fieldset'>
                        <FormGroup>
                            <FormControlLabel
                                control={<Checkbox checked={allChecked} onChange={handleChange} name='checkAll' />}
                                label='All Versions'
                            />
                            {
                                Array.from(versions.entries()).filter(([version, checked]) => VERSION_ALIASES.indexOf(version) < 0).map(([version, checked], key) => {
                                    return <FormControlLabel
                                        key={key}
                                        control={<Checkbox checked={checked} onChange={handleChange} name={version} />}
                                        label={version}
                                    />;
                                })
                            }
                        </FormGroup>
                    </FormControl>
                    <ExtensionRemoveDialog
                        onUpdate={handleUpdate}
                        extension={extension}
                        removeAll={allChecked}
                        versions={
                            Array.from(versions.entries())
                                .filter(([version, checked]) => VERSION_ALIASES.indexOf(version) < 0 && checked)
                                .map(([version]) => version)} />
                </Grid>
            </Grid>
        </Grid>
    </>;
};