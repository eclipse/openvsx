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

import { FunctionComponent } from 'react';
import { Link as RouteLink, useNavigate } from 'react-router';
import { Button, ButtonProps } from '@mui/material';
import { alpha, styled, SxProps, Theme } from '@mui/material/styles';
import FileUploadOutlinedIcon from '@mui/icons-material/FileUploadOutlined';
import { KbdKey } from '../kbd-key';
import { useShortcut } from '../../hooks/use-shortcut';
import { PublishRoutes } from '../../pages/publish/publish-routes';
import { usePublishDrop } from './use-publish-drop';

const Root = styled(Button, {
    shouldForwardProp: prop => prop !== 'dragging' && prop !== 'over'
})<ButtonProps<typeof RouteLink> & { dragging?: boolean; over?: boolean }>(({ theme, dragging, over }) => ({
    margin: '0 0.25rem',
    padding: '0.5rem 1.125rem',
    fontWeight: 600,
    fontSize: '0.8125rem',
    borderRadius: `${theme.shape.borderRadius}px`,
    display: 'inline-flex',
    alignItems: 'center',
    gap: '0.4375rem',
    // Always outlined, transparent until a drag: the frame can then fade in, and drawing it inside
    // the box keeps the nav around it from shifting.
    outline: '2px dashed transparent',
    outlineOffset: '-2px',
    transition: 'background-color 0.15s, outline-color 0.15s',
    '&:hover': { backgroundColor: alpha(theme.palette.secondary.main, 0.08) },
    ...(dragging && {
        outlineColor: alpha(theme.palette.secondary.main, over ? 1 : 0.45),
        backgroundColor: alpha(theme.palette.secondary.main, over ? 0.16 : 0.06)
    })
}));

export interface PublishButtonProps {
    /** Placement and colour tweaks for a nav that is not the built-in one; the rest is the button's own. */
    sx?: SxProps<Theme>;
    className?: string;
}

/**
 * The nav's link to the publish page, doubling as the app's drop target: a file drag anywhere in
 * the window turns it into a drop area, and it fills in once the drag is over it.
 *
 * The keyboard shortcut is registered here alongside the keycap that advertises it and the link
 * that clicking follows, so the three cannot drift apart. A deployment with its own menu should
 * render this rather than its own button, or publishing by drag and drop has nowhere to land.
 */
export const PublishButton: FunctionComponent<PublishButtonProps> = ({ sx, className }) => {
    const navigate = useNavigate();
    const { dragging, over, dropProps } = usePublishDrop();
    useShortcut({ key: 'p', label: 'Publish', order: 3, callback: () => navigate(PublishRoutes.ROOT) });

    return (
        <Root
            variant='text'
            color='secondary'
            component={RouteLink}
            to={PublishRoutes.ROOT}
            dragging={dragging}
            over={over}
            {...dropProps}
            sx={sx}
            className={className}>
            {dragging ? (
                <>
                    <FileUploadOutlinedIcon sx={{ fontSize: '1rem' }} />
                    Drop to publish
                </>
            ) : (
                <>
                    Publish
                    <KbdKey>p</KbdKey>
                </>
            )}
        </Root>
    );
};
