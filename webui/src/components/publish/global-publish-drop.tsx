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

import { FunctionComponent, useContext, useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router';
import { Box, Typography } from '@mui/material';
import { alpha, styled } from '@mui/material/styles';
import FileUploadOutlinedIcon from '@mui/icons-material/FileUploadOutlined';
import { MainContext } from '../../context';
import { usePublishQueue } from '../../context/publish-queue-context';
import { PublishRoutes } from '../../pages/publish/publish-routes';

const Overlay = styled(Box)(({ theme }) => ({
    position: 'fixed',
    inset: '1.25rem',
    zIndex: 1400,
    borderRadius: theme.shape.borderRadiusCard,
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    gap: '0.75rem',
    // Not interactive: the window listener owns the drop, this only says what will happen.
    pointerEvents: 'none',
    backgroundColor: alpha(theme.palette.background.default, 0.85),
    backdropFilter: 'blur(2px)',
    border: `2px dashed ${theme.palette.secondary.main}`,
    color: theme.palette.text.primary
}));

/** Whether a drag is carrying files, as opposed to text or a link. */
const draggingFiles = (event: DragEvent): boolean => Array.from(event.dataTransfer?.types ?? []).includes('Files');

/**
 * Makes the whole app a drop target for `.vsix` packages: dropping anywhere queues
 * the packages and moves to the publish page to watch them, so publishing never
 * requires finding a page first. Armed only for a logged-in user.
 */
export const GlobalPublishDrop: FunctionComponent = () => {
    const { user } = useContext(MainContext);
    const { publish } = usePublishQueue();
    const navigate = useNavigate();
    const [dragging, setDragging] = useState(false);
    // dragenter/dragleave also fire when crossing child elements, so count depth instead.
    const depth = useRef(0);

    useEffect(() => {
        if (!user) {
            return;
        }
        const onDragEnter = (event: DragEvent) => {
            if (!draggingFiles(event)) {
                return;
            }
            depth.current += 1;
            setDragging(true);
        };
        const onDragOver = (event: DragEvent) => {
            // Without this the browser navigates to the dropped file instead.
            if (draggingFiles(event)) {
                event.preventDefault();
            }
        };
        const onDragLeave = () => {
            depth.current = Math.max(0, depth.current - 1);
            if (depth.current === 0) {
                setDragging(false);
            }
        };
        const onDrop = (event: DragEvent) => {
            if (!draggingFiles(event)) {
                return;
            }
            event.preventDefault();
            depth.current = 0;
            setDragging(false);
            publish(Array.from(event.dataTransfer?.files ?? []));
            // The queue lives on the publish page, so follow the packages there.
            navigate(PublishRoutes.ROOT);
        };

        window.addEventListener('dragenter', onDragEnter);
        window.addEventListener('dragover', onDragOver);
        window.addEventListener('dragleave', onDragLeave);
        window.addEventListener('drop', onDrop);
        return () => {
            window.removeEventListener('dragenter', onDragEnter);
            window.removeEventListener('dragover', onDragOver);
            window.removeEventListener('dragleave', onDragLeave);
            window.removeEventListener('drop', onDrop);
        };
    }, [user, publish, navigate]);

    if (!dragging) {
        return null;
    }
    return (
        <Overlay>
            <FileUploadOutlinedIcon sx={{ fontSize: '2.5rem', color: 'secondary.main' }} />
            <Typography sx={{ fontSize: '1.125rem', fontWeight: 700 }}>Drop to publish</Typography>
            <Typography sx={{ fontSize: '0.875rem', color: 'text.secondary' }}>
                Every <code>.vsix</code> package you drop is uploaded straight away.
            </Typography>
        </Overlay>
    );
};
