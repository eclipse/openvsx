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

import React, { FunctionComponent, useMemo } from 'react';
import { Dialog, DialogTitle, DialogContent, DialogActions, Button, Typography, List, ListItem, ListItemText, Box, Tooltip } from '@mui/material';
import { Info as InfoIcon } from '@mui/icons-material';
import { useDialogs } from '../../../hooks/scan-admin';
import { useTheme } from '@mui/material/styles';
import { ScanResult } from '../../../context/scan-admin';

/**
 * Get only the enforced threats from a scan.
 * Only enforced threats should have file decisions made on them.
 */
const getEnforcedThreats = (scan: ScanResult) => {
    return scan.threats?.filter(threat => threat.enforcedFlag) || [];
};

/**
 * Get only the unenforced threats from a scan.
 */
const getUnenforcedThreats = (scan: ScanResult) => {
    return scan.threats?.filter(threat => !threat.enforcedFlag) || [];
};

/**
 * Confirmation dialog for extension-level allow/block actions.
 * Uses the useDialogs hook to consume context.
 *
 * Only files from ENFORCED threats are actionable.
 * Unenforced threats are informational only and
 * should not have allow/block decisions made on them.
 */
export const QuarantineDialog: FunctionComponent = () => {
    const theme = useTheme();
    const { confirmDialog } = useDialogs();

    // Check if any extension has unenforced threats (to show info message)
    const hasAnyUnenforcedThreats = useMemo(() => {
        return confirmDialog.selectedExtensions.some((scan: ScanResult) => getUnenforcedThreats(scan).length > 0);
    }, [confirmDialog.selectedExtensions]);

    // Calculate total enforced files across all selected extensions
    const totalEnforcedFiles = useMemo(() => {
        return confirmDialog.selectedExtensions.reduce(
            (total: number, scan: ScanResult) => total + getEnforcedThreats(scan).length,
            0
        );
    }, [confirmDialog.selectedExtensions]);

    return (
        <Dialog
            open={confirmDialog.isOpen}
            onClose={confirmDialog.close}
            maxWidth='md'
            fullWidth
        >
            <DialogTitle>
                {confirmDialog.action === 'allow' ? 'Confirm Allow' : 'Confirm Block'}
            </DialogTitle>
            <DialogContent>
                <Typography variant='body1' sx={{ mb: 2 }}>
                    Are you sure you want to {confirmDialog.action} {totalEnforcedFiles} enforced file{totalEnforcedFiles !== 1 ? 's' : ''} from {confirmDialog.selectedExtensions.length !== 1 ? 'these' : 'this'} {confirmDialog.selectedExtensions.length} extension{confirmDialog.selectedExtensions.length !== 1 ? 's' : ''}?
                </Typography>

                {/* Info message about unenforced threats */}
                {hasAnyUnenforcedThreats && (
                    <Box sx={{
                        display: 'flex',
                        alignItems: 'flex-start',
                        gap: 1,
                        mb: 2,
                        p: 1.5,
                        backgroundColor: theme.palette.info.dark + '20',
                        borderRadius: 1,
                        border: `1px solid ${theme.palette.info.dark}40`,
                    }}>
                        <InfoIcon sx={{ fontSize: 18, color: theme.palette.info.main, mt: 0.25 }} />
                        <Typography variant='body2' color='text.secondary'>
                            Some extensions have unenforced threats. These are informational only and will not be added to the {confirmDialog.action === 'allow' ? 'allow' : 'block'} list.
                        </Typography>
                    </Box>
                )}

                <List sx={{
                    maxHeight: '400px',
                    overflow: 'auto',
                    border: `1px solid ${theme.palette.scanBackground.default}`,
                    borderRadius: 1,
                }}>
                    {confirmDialog.selectedExtensions.map((scan: ScanResult) => {
                        const enforcedThreats = getEnforcedThreats(scan);
                        const unenforcedThreats = getUnenforcedThreats(scan);

                        return (
                            <ListItem
                                key={scan.id}
                                sx={{
                                    borderBottom: `1px solid ${theme.palette.scanBackground.dark}`,
                                    '&:last-child': { borderBottom: 'none' },
                                    display: 'flex',
                                    alignItems: 'flex-start',
                                    gap: 2,
                                }}
                            >
                                <ListItemText
                                    sx={{ flex: 1, minWidth: 0 }}
                                    primary={
                                        <Typography
                                            variant='body1'
                                            sx={{
                                                fontWeight: 500,
                                                overflow: 'hidden',
                                                textOverflow: 'ellipsis',
                                                whiteSpace: 'nowrap',
                                            }}
                                        >
                                            {scan.displayName}
                                        </Typography>
                                    }
                                    secondary={
                                        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 0.25 }}>
                                            <Typography
                                                variant='body2'
                                                color='text.secondary'
                                                sx={{
                                                    overflow: 'hidden',
                                                    textOverflow: 'ellipsis',
                                                    whiteSpace: 'nowrap',
                                                }}
                                            >
                                                {scan.namespace}.{scan.extensionName}
                                            </Typography>
                                            <Typography
                                                variant='body2'
                                                color='text.secondary'
                                                sx={{
                                                    overflow: 'hidden',
                                                    textOverflow: 'ellipsis',
                                                    whiteSpace: 'nowrap',
                                                }}
                                            >
                                                Publisher: {scan.publisher}
                                            </Typography>
                                            <Typography
                                                variant='body2'
                                                color='text.secondary'
                                                sx={{
                                                    overflow: 'hidden',
                                                    textOverflow: 'ellipsis',
                                                    whiteSpace: 'nowrap',
                                                }}
                                            >
                                                Version: {scan.version}
                                            </Typography>
                                        </Box>
                                    }
                                />
                                <Tooltip
                                    title={
                                        enforcedThreats.length > 0 ? (
                                            <Box>
                                                <Typography variant='caption' sx={{ fontWeight: 600 }}>
                                                    Files to {confirmDialog.action}:
                                                </Typography>
                                                <Box sx={{ whiteSpace: 'pre-line', mt: 0.5 }}>
                                                    {enforcedThreats.map((threat, index) => (
                                                        <div key={index}>{threat.fileName}</div>
                                                    ))}
                                                </Box>
                                                {unenforcedThreats.length > 0 && (
                                                    <>
                                                        <Typography variant='caption' sx={{ fontWeight: 600, mt: 1, display: 'block' }}>
                                                            Unenforced (not included):
                                                        </Typography>
                                                        <Box sx={{ whiteSpace: 'pre-line', mt: 0.5, opacity: 0.7 }}>
                                                            {unenforcedThreats.map((threat, index) => (
                                                                <div key={index}>{threat.fileName}</div>
                                                            ))}
                                                        </Box>
                                                    </>
                                                )}
                                            </Box>
                                        ) : (
                                            'No enforced threats to action'
                                        )
                                    }
                                    arrow
                                >
                                    <Box sx={{
                                        flexShrink: 0,
                                        alignSelf: 'flex-start',
                                        mt: 0.5,
                                        cursor: 'pointer',
                                        textAlign: 'right',
                                    }}>
                                        <Typography variant='body2' color='text.secondary'>
                                            {enforcedThreats.length} file{enforcedThreats.length !== 1 ? 's' : ''}
                                        </Typography>
                                        {unenforcedThreats.length > 0 && (
                                            <Typography variant='caption' sx={{ color: 'text.disabled', display: 'block' }}>
                                                +{unenforcedThreats.length} unenforced
                                            </Typography>
                                        )}
                                    </Box>
                                </Tooltip>
                            </ListItem>
                        );
                    })}
                </List>
            </DialogContent>
            <DialogActions sx={{ px: 3, pb: 2 }}>
                <Button onClick={confirmDialog.close} color='inherit'>
                    Cancel
                </Button>
                <Button
                    onClick={confirmDialog.execute}
                    variant='contained'
                    color={confirmDialog.action === 'allow' ? 'success' : 'error'}
                >
                    Confirm {confirmDialog.action === 'allow' ? 'Allow' : 'Block'}
                </Button>
            </DialogActions>
        </Dialog>
    );
};
