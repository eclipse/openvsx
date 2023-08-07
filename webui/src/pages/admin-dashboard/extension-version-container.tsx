/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import React, { ChangeEvent, FunctionComponent, useContext, useState, useEffect } from 'react';
import { Extension, TargetPlatformVersion, VERSION_ALIASES } from '../../extension-registry-types';
import { Box, Grid, Typography, FormControl, FormGroup, FormControlLabel, Checkbox } from '@mui/material';
import { ExtensionRemoveDialog } from './extension-remove-dialog';
import { getTargetPlatformDisplayName } from '../../utils';
import { MainContext } from '../../context';

export const ExtensionVersionContainer: FunctionComponent<ExtensionVersionContainerProps> = props => {
    const WILDCARD = '*';
    const { extension } = props;
    const { service } = useContext(MainContext);

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

    const abortController = new AbortController();
    useEffect(() => {
        return () => {
            abortController.abort();
        };
    }, []);

    const [targetPlatformVersions, setTargetPlatformVersions] = useState(getTargetPlatformVersions());
    const [icon, setIcon] = useState<string | undefined>(undefined);
    useEffect(() => {
        if (icon) {
            URL.revokeObjectURL(icon);
        }

        service.getExtensionIcon(abortController, props.extension).then(setIcon);
        setTargetPlatformVersions(getTargetPlatformVersions());
    }, [props.extension]);

    const handleChange = (event: ChangeEvent<HTMLInputElement>) => {
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
        <Grid container direction='column' sx={{ height: '100%' }}>
            <Grid item container>
                {
                    icon ?
                        <Grid item xs={12} md={4}>
                            <Box
                                component='img'
                                src={icon}
                                alt={extension.displayName || extension.name}
                                sx={{
                                    height: '7.5rem',
                                    maxWidth: '9rem'
                                }}
                            />
                        </Grid>
                        : ''
                }
                <Grid item container xs={12} md={8}>
                    <Grid item container direction='column' justifyContent='center'>
                        <Grid item>
                            <Typography variant='h5' sx={{ fontWeight: 'bold' }}>
                                {extension.displayName || extension.name}
                            </Typography>
                        </Grid>
                        <Grid item>
                            <Typography sx={{ fontFamily: 'Monaco, monospace' }}>{extension.namespace}.{extension.name}</Typography>
                        </Grid>
                        <Grid item>
                            <Typography sx={{ overflow: 'hidden', textOverflow: 'ellipsis' }}>{extension.description}</Typography>
                        </Grid>
                    </Grid>
                </Grid>
            </Grid>
            <Grid item container sx={{ flex: 1 }}>
                <Grid item xs={12} md={4}></Grid>
                <Grid item container xs={12} md={8} direction='column'>
                    <FormControl component='fieldset'>
                        <FormGroup>
                            {
                                targetPlatformVersions.map((targetPlatformVersion, index) => {
                                        let label: string;
                                        let indent: number;
                                        if (targetPlatformVersion.version === WILDCARD && targetPlatformVersion.targetPlatform === WILDCARD) {
                                            label = 'All Versions';
                                            indent = 0;
                                        } else if (targetPlatformVersion.targetPlatform === WILDCARD) {
                                            label = targetPlatformVersion.version;
                                            indent = 4;
                                        } else {
                                            label = getTargetPlatformDisplayName(targetPlatformVersion.targetPlatform);
                                            indent = 8;
                                        }

                                        const name = `${targetPlatformVersion.targetPlatform}/${targetPlatformVersion.version}`;
                                        return <FormControlLabel
                                            sx={{ pl: indent }}
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

export interface ExtensionVersionContainerProps {
    extension: Extension;
    onUpdate: () => void;
}