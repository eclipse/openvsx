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
import { Extension, TargetPlatformVersion, VERSION_ALIASES } from '../../extension-registry-types';
import { Grid, makeStyles, Typography, FormControl, FormGroup, FormControlLabel, Checkbox } from '@material-ui/core';
import { ExtensionRemoveDialog } from './extension-remove-dialog';
import { getTargetPlatformDisplayName } from '../../utils';

const useStyles = makeStyles((theme) => ({
    indent0: {
        paddingLeft: '0 px'
    },
    indent1: {
        paddingLeft: `${theme.spacing(4)}px`
    },
    indent2: {
        paddingLeft: `${theme.spacing(8)}px`
    },
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
    },
    extensionContainer: {
        height: '100%'
    },
    versionsContainer: {
        flex: '1'
    }
}));

export const ExtensionVersionContainer: FunctionComponent<ExtensionVersionContainer.Props> = props => {
    const WILDCARD = '*';
    const { extension } = props;
    const classes = useStyles();

    const getTargetPlatformVersions = () => {
        const versionMap: TargetPlatformVersion[] = [];
        versionMap.push({ targetPlatform: WILDCARD, version: WILDCARD, checked: false });
        Object.keys(extension.allTargetPlatformVersions)
            .filter(version => VERSION_ALIASES.indexOf(version) < 0)
            .forEach(version => {
                versionMap.push({ targetPlatform: WILDCARD, version: version, checked: false });
                const targetPlatforms = extension.allTargetPlatformVersions[version];
                targetPlatforms.forEach(targetPlatform => versionMap.push({ targetPlatform: targetPlatform, version: version, checked: false }));
            });

        return versionMap;
    };

    const [targetPlatformVersions, setTargetPlatformVersions] = useState(getTargetPlatformVersions());

    useEffect(() => {
        setTargetPlatformVersions(getTargetPlatformVersions());
    }, [props.extension]);

    const handleChange = (event: React.ChangeEvent<HTMLInputElement>) => {
        const newTargetPlatformVersions: TargetPlatformVersion[] = [];
        targetPlatformVersions.forEach((targetPlatformVersion) => {
            const equals = (change: string, current: string) => {
                return change === WILDCARD || current === change;
            };

            const [changedTarget, changedVersion] = event.target.name.split('/');
            if (equals(changedVersion, targetPlatformVersion.version) && equals(changedTarget, targetPlatformVersion.targetPlatform)) {
                targetPlatformVersion.checked = event.target.checked;
            }

            newTargetPlatformVersions.push(targetPlatformVersion);
        });

        const newVersionsMap = new Map<string, boolean>();
        newTargetPlatformVersions.forEach((targetPlatformVersion) => {
            if (targetPlatformVersion.version !== WILDCARD && targetPlatformVersion.targetPlatform !== WILDCARD) {
                let checked = newVersionsMap.get(targetPlatformVersion.version);
                if (checked === undefined) {
                    checked = true;
                }

                newVersionsMap.set(targetPlatformVersion.version, checked && targetPlatformVersion.checked);
            }
        });

        newVersionsMap.forEach((checked, version) => {
            const targetVersion = newTargetPlatformVersions.find(t => t.version === version && t.targetPlatform === WILDCARD);
            targetVersion!.checked = checked;
        });

        const checkedCount = Array.from(newTargetPlatformVersions).filter(t => t.checked === true).length;
        if (checkedCount < newTargetPlatformVersions.length) {
            const allChecked = checkedCount === newTargetPlatformVersions.length - 1;
            const allVersions = newTargetPlatformVersions.find(t => t.version === WILDCARD && t.targetPlatform === WILDCARD);
            allVersions!.checked = allChecked;
        }

        setTargetPlatformVersions(newTargetPlatformVersions);
    };

    return <>
        <Grid container direction='column' className={classes.extensionContainer}>
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
            <Grid item container className={classes.versionsContainer}>
                <Grid item xs={12} md={4}></Grid>
                <Grid item container xs={12} md={8} direction='column'>
                    <FormControl component='fieldset'>
                        <FormGroup>
                            {
                                targetPlatformVersions.map((targetPlatformVersion, index) => {
                                        let label: string;
                                        let indentClass: string;
                                        if (targetPlatformVersion.version === WILDCARD && targetPlatformVersion.targetPlatform === WILDCARD) {
                                            label = 'All Versions';
                                            indentClass = classes.indent0;
                                        } else if (targetPlatformVersion.targetPlatform === WILDCARD) {
                                            label = targetPlatformVersion.version;
                                            indentClass = classes.indent1;
                                        } else {
                                            label = getTargetPlatformDisplayName(targetPlatformVersion.targetPlatform);
                                            indentClass = classes.indent2;
                                        }

                                        const name = `${targetPlatformVersion.targetPlatform}/${targetPlatformVersion.version}`;
                                        return <FormControlLabel
                                            classes={{ root: indentClass }}
                                            key={`${name}_${index}`}
                                            control={<Checkbox checked={targetPlatformVersion.checked} onChange={handleChange} name={name} />}
                                            label={label} />;
                                    })
                            }
                        </FormGroup>
                    </FormControl>
                </Grid>
            </Grid>
            <Grid item container>
                <Grid item xs={12} md={4}>
                </Grid>
                <Grid item container xs={12} md={8}>
                    <ExtensionRemoveDialog
                        onUpdate={props.onUpdate}
                        extension={extension}
                        targetPlatformVersions={targetPlatformVersions.filter((targetPlatformVersion) => targetPlatformVersion.checked)} />
                </Grid>
            </Grid>
        </Grid>
    </>;
};

export namespace ExtensionVersionContainer {
    export interface Props {
        extension: Extension;
        onUpdate: () => void;
    }
}
