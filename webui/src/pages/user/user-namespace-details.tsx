/********************************************************************************
 * Copyright (c) 2022 Precies. Software and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import React, { ChangeEvent, FunctionComponent, useContext, useEffect, useMemo, useState } from 'react';
import { TextField, Typography, Grid, Button, IconButton, InputAdornment, Select, MenuItem, Paper, SelectChangeEvent } from '@mui/material';
import { CheckCircleOutline } from '@mui/icons-material';
import BusinessIcon from '@mui/icons-material/Business';
import GitHubIcon from '@mui/icons-material/GitHub';
import LinkedInIcon from '@mui/icons-material/LinkedIn';
import PersonIcon from '@mui/icons-material/Person';
import TwitterIcon from '@mui/icons-material/Twitter';
import CloseIcon from '@mui/icons-material/Close';
import { MainContext } from '../../context';
import { DelayedLoadIndicator } from '../../components/delayed-load-indicator';
import { Namespace, NamespaceDetails } from '../../extension-registry-types';
import _ from 'lodash';
import { styled } from '@mui/material/styles';
import { useGetNamespaceDetailsQuery, useSetNamespaceDetailsMutation, useSetNamespaceLogoMutation } from '../../store/api';
import { NamespaceDetailsLogo, UserNamespaceDetailsLogo } from './user-namespace-details-logo';

const GridIconItem = styled(Grid)({
    display: 'flex',
    alignItems: 'center'
});

export const UserNamespaceDetails: FunctionComponent<UserNamespaceDetailsProps> = props => {
    const INPUT_DISPLAY_NAME = 'display-name';
    const INPUT_DESCRIPTION = 'description';
    const INPUT_WEBSITE = 'website';
    const INPUT_SUPPORT_LINK = 'support-link';
    const INPUT_LINKEDIN = 'linkedin';
    const INPUT_GITHUB = 'github';
    const INPUT_TWITTER = 'twitter';
    const LINKED_IN_PERSONAL = 'in';
    const LINKED_IN_COMPANY = 'company';

    const context = useContext(MainContext);
    const [currentDetails, setCurrentDetails] = useState<NamespaceDetails>();
    const [newDetails, setNewDetails] = useState<NamespaceDetails>();
    const [newDetailsLogo, setNewDetailsLogo] = useState<NamespaceDetailsLogo>();
    const [detailsUpdated, setDetailsUpdated] = useState<boolean>(false);
    const [bannerNamespaceName, setBannerNamespaceName] = useState<string>('');
    const [linkedInAccountType, setLinkedInAccountType] = useState<string>(LINKED_IN_PERSONAL);
    const { data: details, isLoading } = useGetNamespaceDetailsQuery(props.namespace.name);
    const [setNamespaceDetails] = useSetNamespaceDetailsMutation();
    const [setNamespaceLogo] = useSetNamespaceLogoMutation();

    useEffect(() => {
        if (isLoading) {
            return;
        }
        if (details == null) {
            setCurrentDetails(undefined);
            setNewDetails(undefined);
            setLinkedInAccountType(LINKED_IN_PERSONAL);
            return;
        }

        let linkedInAccountType = LINKED_IN_PERSONAL;
        const linkedin = details.socialLinks.linkedin;
        if (linkedin) {
            const linkedinPath = linkedin.split('/');
            details.socialLinks.linkedin = linkedinPath[linkedinPath.length - 1];
            linkedInAccountType = linkedinPath[linkedinPath.length - 2];
        }

        const github = details.socialLinks.github;
        if (github) {
            details.socialLinks.github = github.substring(github.lastIndexOf('/') + 1);
        }

        const twitter = details.socialLinks.twitter;
        if (twitter) {
            details.socialLinks.twitter = twitter.substring(twitter.lastIndexOf('/') + 1);
        }

        setCurrentDetails(copy(details));
        setNewDetails(copy(details));
        setLinkedInAccountType(linkedInAccountType);
    }, [details, isLoading]);

    const noChanges = useMemo(() => {
        const isFalsy = (x: unknown) => !!x === false;
        return _.isEqual(_.omitBy(currentDetails, isFalsy), _.omitBy(newDetails, isFalsy)) && newDetailsLogo == null;
    }, [currentDetails, newDetails, newDetailsLogo]);

    const copy = (arg: NamespaceDetails): NamespaceDetails => {
        return JSON.parse(JSON.stringify(arg));
    };

    const saveNamespaceDetails = async () => {
        if (!newDetails) {
            return;
        }

        setDetailsUpdated(false);
        const details = copy(newDetails);
        details.socialLinks.linkedin = details.socialLinks.linkedin
            ? `https://www.linkedin.com/${linkedInAccountType}/${details.socialLinks.linkedin}`
            : undefined;

        details.socialLinks.github = details.socialLinks.github
            ? 'https://github.com/' + details.socialLinks.github
            : undefined;

        details.socialLinks.twitter = details.socialLinks.twitter
            ? 'https://twitter.com/' + details.socialLinks.twitter
            : undefined;

        const detailsPromise = setNamespaceDetails({ endpoint: props.namespace.detailsUrl, details });
        const logoPromise = newDetailsLogo != null
            ? setNamespaceLogo({ endpoint: props.namespace.detailsUrl, name: props.namespace.name, logoFile: newDetailsLogo.file, logoName: newDetailsLogo.name })
            : Promise.resolve();

        await Promise.all([detailsPromise, logoPromise]);
        setDetailsUpdated(true);
        setBannerNamespaceName(details.displayName || details.name);
    };

    const handleInputChange = (event: ChangeEvent<HTMLInputElement>) => {
        if (!newDetails) {
            return;
        }

        const input = event.target;
        const details = copy(newDetails);
        switch (input.name) {
            case INPUT_DISPLAY_NAME:
                details.displayName = input.value;
                break;
            case INPUT_DESCRIPTION:
                details.description = input.value;
                break;
            case INPUT_WEBSITE:
                details.website = input.value;
                break;
            case INPUT_SUPPORT_LINK:
                details.supportLink = input.value;
                break;
            case INPUT_LINKEDIN:
                if (input.value.startsWith('https://www.linkedin.com/')) {
                    if (input.value.lastIndexOf('/') === input.value.length - 1) {
                        input.value = input.value.substring(0, input.value.length - 1);
                    }

                    const linkedinPath = input.value.split('/');
                    details.socialLinks.linkedin = linkedinPath[linkedinPath.length - 1];
                    const linkedInAccountType = linkedinPath[linkedinPath.length - 2];
                    setLinkedInAccountType(linkedInAccountType);
                } else {
                    details.socialLinks.linkedin = input.value;
                }
                break;
            case INPUT_GITHUB:
                if (input.value.startsWith('https://github.com/')) {
                    if (input.value.lastIndexOf('/') === input.value.length - 1) {
                        input.value = input.value.substring(0, input.value.length - 1);
                    }

                    details.socialLinks.github = input.value.substring(input.value.lastIndexOf('/') + 1);
                } else {
                    details.socialLinks.github = input.value;
                }
                break;
            case INPUT_TWITTER:
                if (input.value.startsWith('https://twitter.com/')) {
                    if (input.value.lastIndexOf('/') === input.value.length - 1) {
                        input.value = input.value.substring(0, input.value.length - 1);
                    }

                    details.socialLinks.twitter = input.value.substring(input.value.lastIndexOf('/') + 1);
                } else {
                    details.socialLinks.twitter = input.value;
                }
                break;
        }

        setNewDetails(details);
    };

    const handleSelectChange = (event: SelectChangeEvent<string>) => setLinkedInAccountType(event.target.value);
    const handleClose = () => setDetailsUpdated(false);
    const handleLogoChange = (logo: NamespaceDetailsLogo) => setNewDetailsLogo(logo);

    if (!newDetails) {
        return <DelayedLoadIndicator loading={isLoading} />;
    }

    const successColor = context.pageSettings.themeType === 'dark' ? '#fff' : '#000';
    return <>
        <Grid container spacing={2}>
            <Grid item xs={12}>
                <Typography variant='h5'>Details</Typography>
            </Grid>
            {detailsUpdated
                ? <Grid item xs={12} alignItems='center'>
                    <Paper
                        sx={{
                            mb: 2,
                            p: 2,
                            flex: 1,
                            display: 'flex',
                            flexDirection: 'row',
                            alignItems: 'center',
                            color: successColor,
                            bgcolor: `success.${context.pageSettings.themeType}`
                        }}
                    >
                        <CheckCircleOutline fontSize='large' />
                        <Typography variant='body1' sx={{ ml: 1 }}>The details of the &ldquo;{bannerNamespaceName}&rdquo; namespace were updated.</Typography>
                        <IconButton sx={{ ml: 'auto' }} onClick={handleClose}><CloseIcon /></IconButton>
                    </Paper>
                </Grid>
                : null
            }
            <Grid item xs={4} alignItems='center'>
                <UserNamespaceDetailsLogo namespace={props.namespace} onLogoChange={handleLogoChange} />
            </Grid>
            <Grid item xs={8}>
                <Grid container spacing={2}>
                    <Grid item xs={12}>
                        <TextField fullWidth
                            label='Display name'
                            name={INPUT_DISPLAY_NAME}
                            value={newDetails.displayName ?? ''}
                            onChange={handleInputChange} />
                    </Grid>
                    <Grid item xs={12}>
                        <TextField fullWidth
                            multiline
                            rows={3}
                            margin='dense'
                            variant='outlined'
                            label='Description'
                            name={INPUT_DESCRIPTION}
                            value={newDetails.description ?? ''}
                            onChange={handleInputChange} />
                    </Grid>
                </Grid>
            </Grid>
            <Grid item xs={12}>
                <TextField fullWidth
                    label='Website'
                    type='url'
                    name={INPUT_WEBSITE}
                    value={newDetails.website ?? ''}
                    onChange={handleInputChange} />
            </Grid>
            <Grid item xs={12}>
                <TextField fullWidth
                    label='Support link'
                    type='url'
                    name={INPUT_SUPPORT_LINK}
                    value={newDetails.supportLink ?? ''}
                    onChange={handleInputChange} />
            </Grid>
            <Grid item xs={12}>
                <Grid container spacing={2}>
                    <GridIconItem item>
                        <LinkedInIcon titleAccess='LinkedIn profile' />
                    </GridIconItem>
                    <Grid item xs>
                        <TextField fullWidth
                            name={INPUT_LINKEDIN}
                            value={newDetails.socialLinks.linkedin ?? ''}
                            onChange={handleInputChange}
                            InputProps={{
                                startAdornment: <InputAdornment position='start'>
                                    <Select
                                        value={linkedInAccountType}
                                        onChange={handleSelectChange}
                                        sx={{
                                            '& .MuiSelect-select': {
                                                py: 1.75
                                            },
                                            '&.Mui-focused': {
                                                '& .MuiOutlinedInput-notchedOutline': {
                                                    border: 0
                                                }
                                            },
                                            '& .MuiOutlinedInput-notchedOutline': {
                                                border: 0
                                            }
                                        }}
                                    >
                                        <MenuItem value={LINKED_IN_PERSONAL} selected={true}><PersonIcon sx={{ color: 'text.secondary' }} titleAccess='Personal profile' /></MenuItem>
                                        <MenuItem value={LINKED_IN_COMPANY}><BusinessIcon sx={{ color: 'text.secondary' }} titleAccess='Company profile' /></MenuItem>
                                    </Select>
                                    <Typography color='textSecondary'>https://www.linkedin.com/{linkedInAccountType}/</Typography>
                                </InputAdornment>
                            }} />
                    </Grid>
                </Grid>
            </Grid>
            <Grid item xs={12}>
                <Grid container spacing={2}>
                    <GridIconItem item>
                        <GitHubIcon titleAccess='GitHub profile' />
                    </GridIconItem>
                    <Grid item xs>
                        <TextField fullWidth
                            name={INPUT_GITHUB}
                            value={newDetails.socialLinks.github ?? ''}
                            onChange={handleInputChange}
                            InputProps={{ startAdornment: <InputAdornment position='start'>https://github.com/</InputAdornment> }} />
                    </Grid>
                </Grid>
            </Grid>
            <Grid item xs={12}>
                <Grid container spacing={2}>
                    <GridIconItem item>
                        <TwitterIcon titleAccess='Twitter profile' />
                    </GridIconItem>
                    <Grid item xs>
                        <TextField fullWidth
                            name={INPUT_TWITTER}
                            value={newDetails.socialLinks.twitter ?? ''}
                            onChange={handleInputChange}
                            InputProps={{ startAdornment: <InputAdornment position='start'>https://twitter.com/</InputAdornment> }} />
                    </Grid>
                </Grid>
            </Grid>
            <Grid item xs={12} sx={{ display: 'flex', justifyContent: 'flex-end' }}>
                <Button sx={{ ml: { xs: 2, sm: 2, md: 2, lg: 0, xl: 0 } }} variant='outlined' disabled={noChanges} onClick={saveNamespaceDetails}>
                    Save Namespace Details
                </Button>
            </Grid>
        </Grid>
    </>;
};

export interface UserNamespaceDetailsProps {
    namespace: Namespace;
}