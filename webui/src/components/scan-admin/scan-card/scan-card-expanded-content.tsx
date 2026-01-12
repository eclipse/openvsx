/********************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import React from 'react';
import { Box, Typography, Collapse } from '@mui/material';
import { useTheme } from '@mui/material/styles';
import { ScanResult, Threat, ValidationFailure } from '../../../context/scan-admin';
import { ScanDetailCard } from './scan-detail-card';
import { formatDateTime } from '../common';

interface ScanCardExpandedContentProps {
    scan: ScanResult;
    expanded: boolean;
    onCollapseComplete?: () => void;
}

interface ThreatItemProps {
    threat: Threat;
}

interface ValidationFailureItemProps {
    failure: ValidationFailure;
}

/**
 * A single threat item in the expanded content.
 */
const ThreatItem: React.FC<ThreatItemProps> = ({ threat }) => {
    const theme = useTheme();

    return (
        <ScanDetailCard
            accentColor={theme.palette.quarantined.dark as string}
            isUnenforced={!threat.enforcedFlag}
            description={threat.reason}
            descriptionColor='warning.dark'
            details={[
                { label: 'File', value: threat.fileName },
                { label: 'Hash', value: threat.fileHash },
                { label: 'Scanner', value: threat.type },
                { label: 'Rule Name', value: threat.ruleName },
                { label: 'Severity', value: threat.severity },
                { label: 'Detected at', value: threat.dateDetected ? formatDateTime(threat.dateDetected) : undefined },
            ]}
        />
    );
};

/**
 * A single validation failure item in the expanded content.
 */
const ValidationFailureItem: React.FC<ValidationFailureItemProps> = ({ failure }) => {
    const theme = useTheme();
    const isUnenforced = !failure.enforcedFlag;

    return (
        <ScanDetailCard
            accentColor={theme.palette.rejected.dark as string}
            isUnenforced={isUnenforced}
            chip={{
                label: failure.type,
                color: theme.palette.rejected.dark as string,
                textColor: theme.palette.rejected.light as string,
            }}
            description={failure.reason}
            details={[
                { label: 'Rule Name', value: failure.ruleName },
                { label: 'Detected at', value: failure.dateDetected ? formatDateTime(failure.dateDetected) : undefined },
            ]}
        />
    );
};

/**
 * The expanded content section showing threats and validation failures.
 * Each item's enforcedFlag controls its individual striping effect.
 */
export const ScanCardExpandedContent: React.FC<ScanCardExpandedContentProps> = ({ scan, expanded, onCollapseComplete }) => {
    const theme = useTheme();
    const hasThreats = scan.threats.length > 0;
    const hasValidationFailures = scan.validationFailures.length > 0;
    const hasErrorMessage = scan.status === 'ERROR' && scan.errorMessage;

    return (
        <Collapse in={expanded} timeout='auto' unmountOnExit onExited={onCollapseComplete}>
            <Box sx={{
                px: 4,
                pb: 3,
                pt: 2,
                borderBottomRightRadius: 8,
            }}>
                {/* Error Message */}
                {hasErrorMessage && (
                    <Box sx={{ mb: (hasThreats || hasValidationFailures) ? 2 : 0 }}>
                        <ScanDetailCard
                            accentColor={theme.palette.errorStatus.dark as string}
                            chip={{
                                label: 'ERROR',
                                color: theme.palette.errorStatus.dark as string,
                                textColor: theme.palette.errorStatus.light as string,
                            }}
                            description={scan.errorMessage ?? undefined}
                            descriptionColor='text.secondary'
                            details={[]}
                        />
                    </Box>
                )}

                {/* Threats */}
                {hasThreats && (
                    <Box>
                        {hasValidationFailures && (
                            <Typography variant='subtitle2' sx={{ mb: 1.5, color: 'text.secondary' }}>
                                Threats
                            </Typography>
                        )}
                        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.5 }}>
                            {scan.threats.map((threat, index) => (
                                <ThreatItem key={index} threat={threat} />
                            ))}
                        </Box>
                    </Box>
                )}

                {/* Validation Failures */}
                {hasValidationFailures && (
                    <Box sx={{ mt: hasThreats ? 2 : 0 }}>
                        {hasThreats && (
                            <Typography variant='subtitle2' sx={{ mb: 1.5, color: 'text.secondary' }}>
                                Validation Failures
                            </Typography>
                        )}
                        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.5 }}>
                            {scan.validationFailures.map((failure, index) => (
                                <ValidationFailureItem key={index} failure={failure} />
                            ))}
                        </Box>
                    </Box>
                )}
            </Box>
        </Collapse>
    );
};
