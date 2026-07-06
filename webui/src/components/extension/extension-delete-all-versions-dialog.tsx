/******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/

import { FunctionComponent, useContext, useState } from 'react';
import { Button, Dialog, DialogActions, DialogContent, DialogContentText, DialogTitle } from '@mui/material';
import { ButtonWithProgress } from '../button-with-progress';
import { MainContext } from '../../context';
import { Extension, VersionTargetPlatforms } from '../../extension-registry-types';
import { VersionDeleteTarget } from './extension-version-delete-dialog';

export const DeleteAllVersionsDialog: FunctionComponent<DeleteAllVersionsDialogProps> = props => {
    const { handleError } = useContext(MainContext);
    const [working, setWorking] = useState(false);

    const handleRemove = async () => {
        try {
            setWorking(true);
            const targets: VersionDeleteTarget[] = props.versions.flatMap(v =>
                v.targetPlatforms.map(({ targetPlatform }) => ({ version: v.version, targetPlatform }))
            );
            await props.onRemove(targets);
            props.onDeleted();
            props.onClose();
        } catch (err) {
            handleError(err);
        } finally {
            setWorking(false);
        }
    };

    return (
        <Dialog open={props.open} onClose={props.onClose} maxWidth='xs' fullWidth>
            <DialogTitle>Delete all versions of {props.extension.displayName ?? props.extension.name}?</DialogTitle>
            <DialogContent>
                <DialogContentText>
                    This will permanently remove {props.versions.length} version
                    {props.versions.length === 1 ? '' : 's'} of this extension across all target platforms. This action
                    cannot be undone.
                </DialogContentText>
            </DialogContent>
            <DialogActions>
                <Button variant='contained' onClick={props.onClose}>
                    Cancel
                </Button>
                <ButtonWithProgress sx={{ ml: 1 }} color='error' working={working} onClick={handleRemove}>
                    Delete All Versions
                </ButtonWithProgress>
            </DialogActions>
        </Dialog>
    );
};

export interface DeleteAllVersionsDialogProps {
    open: boolean;
    onClose: () => void;
    extension: Extension;
    versions: VersionTargetPlatforms[];
    onRemove: (targets: VersionDeleteTarget[]) => Promise<unknown>;
    onDeleted: () => void;
}
