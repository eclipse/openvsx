/********************************************************************************
 * Copyright (c) 2022 Precies. Software and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import { FunctionComponent, ReactNode, useContext, useEffect, useState, useRef } from 'react';
import { Typography, Box, Link, Divider } from '@mui/material';
import GitHubIcon from '@mui/icons-material/GitHub';
import LinkedInIcon from '@mui/icons-material/LinkedIn';
import TwitterIcon from '@mui/icons-material/Twitter';
import { useParams } from 'react-router-dom';
import { ExtensionCard } from '../../components/extension-card';
import { MainContext } from '../../context';
import { DelayedLoadIndicator } from '../../components/delayed-load-indicator';
import { Section } from '../../components/page-primitives';
import { NamespaceDetails, isError, UrlString } from '../../extension-registry-types';

export const NamespaceDetail: FunctionComponent = () => {
    const [loading, setLoading] = useState(true);
    const [truncateReadMore, setTruncateReadMore] = useState(true);
    const [showReadMore, setShowReadMore] = useState(false);
    const [namespaceDetails, setNamespaceDetails] = useState<Readonly<NamespaceDetails>>();
    const [notFoundError, setNotFoundError] = useState('');

    const { name } = useParams();
    const { pageSettings, service, handleError } = useContext(MainContext);

    const abortController = useRef<AbortController>(new AbortController());
    useEffect(() => {
        updateNamespaceDetails(name as string);
        return () => {
            abortController.current.abort();
        };
    }, []);

    useEffect(() => {
        setNamespaceDetails(undefined);
        setLoading(true);
        updateNamespaceDetails(name as string);
    }, [name]);

    const updateNamespaceDetails = async (name: string): Promise<void> => {
        try {
            const namespaceDetails = await service.getNamespaceDetails(abortController.current, name);
            if (isError(namespaceDetails)) {
                throw namespaceDetails;
            }

            setNamespaceDetails(namespaceDetails);
            setLoading(false);
            setTruncateReadMore(true);
        } catch (err) {
            if (err && err.status === 404) {
                setNotFoundError(`Namespace Not Found: ${name}`);
            } else {
                handleError(err);
            }

            setLoading(false);
        }
    };

    const readMore = () => {
        setTruncateReadMore(false);
    };

    const displayLink = (link: UrlString) => {
        return link.replace(/https?:\/\//, '');
    };

    const renderHeaderTags = (name: string, namespaceDetails?: NamespaceDetails): ReactNode => {
        const { namespaceHeadTags: NamespaceHeadTagsComponent } = pageSettings.elements;
        return (
            <>
                {NamespaceHeadTagsComponent ? (
                    <NamespaceHeadTagsComponent
                        namespaceDetails={namespaceDetails}
                        name={name}
                        pageSettings={pageSettings}
                    />
                ) : null}
            </>
        );
    };

    const renderNotFound = (): ReactNode => {
        return (
            <>
                {notFoundError ? (
                    <Box p={4}>
                        <Typography variant='h5'>{notFoundError}</Typography>
                    </Box>
                ) : null}
            </>
        );
    };

    const calculateShowReadMore = (el: HTMLElement) => {
        const showReadMore =
            truncateReadMore && el && (el.scrollHeight > el.offsetHeight || el.scrollWidth > el.offsetWidth);
        setShowReadMore(showReadMore);
    };

    const renderNamespaceDetails = (namespaceDetails: NamespaceDetails, truncateReadMore: boolean): ReactNode => {
        const { website, supportLink, socialLinks } = namespaceDetails;
        return (
            <>
                <Box sx={{ borderBottom: '1px solid', borderColor: 'divider' }}>
                    <Section sx={{ py: { xs: '2rem', sm: '3rem' } }}>
                        <Box
                            sx={{
                                display: 'flex',
                                flexDirection: { xs: 'column', sm: 'row' },
                                alignItems: { xs: 'flex-start', sm: 'center' },
                                gap: { xs: '1.5rem', sm: '2rem' }
                            }}>
                            <Box
                                component='img'
                                src={namespaceDetails.logo ?? pageSettings.urls.extensionDefaultIcon}
                                alt={namespaceDetails.displayName}
                                sx={{ width: '7.5rem', height: '7.5rem', objectFit: 'contain', flexShrink: 0 }}
                            />
                            <Box sx={{ minWidth: 0, flex: 1 }}>
                                <Typography variant='h5' sx={{ fontWeight: 700 }}>
                                    {namespaceDetails.displayName}
                                </Typography>
                                {namespaceDetails.description ? (
                                    <Box sx={{ mt: '0.5rem' }}>
                                        <Typography
                                            color='text.secondary'
                                            ref={calculateShowReadMore}
                                            sx={
                                                truncateReadMore
                                                    ? {
                                                          overflow: 'hidden',
                                                          textOverflow: 'ellipsis',
                                                          display: '-webkit-box',
                                                          WebkitLineClamp: '2',
                                                          WebkitBoxOrient: 'vertical'
                                                      }
                                                    : {}
                                            }>
                                            {namespaceDetails.description}
                                        </Typography>
                                        {showReadMore ? (
                                            <Link
                                                color='secondary'
                                                underline='hover'
                                                component='button'
                                                onClick={readMore}>
                                                Read more
                                            </Link>
                                        ) : null}
                                    </Box>
                                ) : null}
                                {website || supportLink ? (
                                    <Box
                                        sx={{
                                            display: 'flex',
                                            alignItems: 'center',
                                            flexWrap: 'wrap',
                                            gap: '0.75rem',
                                            mt: '1rem'
                                        }}>
                                        {website ? (
                                            <Link color='secondary' underline='hover' target='_blank' href={website}>
                                                {displayLink(website)}
                                            </Link>
                                        ) : null}
                                        {website && supportLink ? <Divider orientation='vertical' flexItem /> : null}
                                        {supportLink ? (
                                            <Link
                                                color='secondary'
                                                underline='hover'
                                                target='_blank'
                                                href={supportLink}>
                                                {displayLink(supportLink)}
                                            </Link>
                                        ) : null}
                                    </Box>
                                ) : null}
                                {socialLinks.linkedin || socialLinks.github || socialLinks.twitter ? (
                                    <Box sx={{ display: 'flex', alignItems: 'center', gap: '1rem', mt: '1rem' }}>
                                        {socialLinks.linkedin ? (
                                            <Link target='_blank' color='text.primary' href={socialLinks.linkedin}>
                                                <LinkedInIcon />
                                            </Link>
                                        ) : null}
                                        {socialLinks.github ? (
                                            <Link target='_blank' color='text.primary' href={socialLinks.github}>
                                                <GitHubIcon />
                                            </Link>
                                        ) : null}
                                        {socialLinks.twitter ? (
                                            <Link target='_blank' color='text.primary' href={socialLinks.twitter}>
                                                <TwitterIcon />
                                            </Link>
                                        ) : null}
                                    </Box>
                                ) : null}
                            </Box>
                        </Box>
                    </Section>
                </Box>
                {namespaceDetails.extensions ? (
                    <Section sx={{ py: { xs: '2rem', sm: '3rem' } }}>
                        <Box
                            sx={{
                                display: 'grid',
                                gridTemplateColumns: {
                                    xs: 'repeat(2, minmax(0, 1fr))',
                                    sm: 'repeat(auto-fill, minmax(190px, 1fr))'
                                },
                                gap: '1rem'
                            }}>
                            {namespaceDetails.extensions.map((ext, idx) => (
                                <ExtensionCard
                                    extension={ext}
                                    fadeDelayMs={(idx % 10) * 200}
                                    key={`${ext.namespace}.${ext.name}`}
                                />
                            ))}
                        </Box>
                    </Section>
                ) : null}
            </>
        );
    };

    return (
        <>
            {renderHeaderTags(name as string, namespaceDetails)}
            <DelayedLoadIndicator loading={loading} />
            {namespaceDetails ? renderNamespaceDetails(namespaceDetails, truncateReadMore) : renderNotFound()}
        </>
    );
};
