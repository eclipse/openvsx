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

import { DragEvent as ReactDragEvent, useCallback, useContext, useEffect, useRef, useState } from 'react';
import { useLocation, useNavigate } from 'react-router';
import { MainContext } from '../../context';
import { usePublishQueue } from '../../context/publish-queue-context';
import { PublishRoutes } from '../../pages/publish/publish-routes';

/** Whether a drag carries files, as opposed to text or a link. */
const draggingFiles = (transfer: DataTransfer | null): boolean => Array.from(transfer?.types ?? []).includes('Files');

export interface PublishDrop {
    /** Files are being dragged somewhere in the window, so the target should offer itself. */
    dragging: boolean;
    /** The drag is over the target itself, so releasing now publishes. */
    over: boolean;
    /** Spread onto the element that takes the drop. */
    dropProps: {
        onDragEnter: (event: ReactDragEvent) => void;
        onDragOver: (event: ReactDragEvent) => void;
        onDragLeave: (event: ReactDragEvent) => void;
        onDrop: (event: ReactDragEvent) => void;
    };
}

/**
 * Makes one element the drop target for `.vsix` packages: everything dropped on it is published
 * straight away and the queue is followed to the publish page.
 *
 * The window is watched only to notice that a drag has started, so the target can offer itself
 * while the packages are still in the air, and to swallow a drop that misses it — the browser
 * answers an unhandled file drop by leaving the app for the file. Armed for a logged-in user only.
 */
export const usePublishDrop = (): PublishDrop => {
    const { user } = useContext(MainContext);
    const { publish } = usePublishQueue();
    const navigate = useNavigate();
    const { pathname } = useLocation();
    const [dragging, setDragging] = useState(false);
    const [over, setOver] = useState(false);
    // dragenter/dragleave also fire when crossing child elements, so count depth instead.
    const depth = useRef(0);

    useEffect(() => {
        if (!user) {
            return;
        }
        const onDragEnter = (event: DragEvent) => {
            if (draggingFiles(event.dataTransfer)) {
                depth.current += 1;
                setDragging(true);
            }
        };
        const onDragLeave = () => {
            depth.current = Math.max(0, depth.current - 1);
            if (depth.current === 0) {
                setDragging(false);
            }
        };
        // Anything that gets this far missed the target. Claiming it leaves the drag with nowhere
        // to land, which is the point: the alternative is the browser navigating away to the file.
        const claim = (event: DragEvent) => {
            if (draggingFiles(event.dataTransfer)) {
                event.preventDefault();
            }
        };
        const onDrop = (event: DragEvent) => {
            claim(event);
            depth.current = 0;
            setDragging(false);
            setOver(false);
        };

        window.addEventListener('dragenter', onDragEnter);
        window.addEventListener('dragleave', onDragLeave);
        window.addEventListener('dragover', claim);
        window.addEventListener('drop', onDrop);
        return () => {
            window.removeEventListener('dragenter', onDragEnter);
            window.removeEventListener('dragleave', onDragLeave);
            window.removeEventListener('dragover', claim);
            window.removeEventListener('drop', onDrop);
        };
    }, [user]);

    const onDragOver = useCallback(
        (event: ReactDragEvent) => {
            if (!user || !draggingFiles(event.dataTransfer)) {
                return;
            }
            // Marks the element a valid target; without it the browser never delivers the drop.
            event.preventDefault();
            setOver(true);
        },
        [user]
    );

    const onDragLeave = useCallback((event: ReactDragEvent) => {
        // Crossing into the target's own children is not leaving it.
        if (!event.currentTarget.contains(event.relatedTarget as Node | null)) {
            setOver(false);
        }
    }, []);

    const onDrop = useCallback(
        (event: ReactDragEvent) => {
            if (!user || !draggingFiles(event.dataTransfer)) {
                return;
            }
            event.preventDefault();
            setOver(false);
            publish(Array.from(event.dataTransfer.files));
            // The queue lives on the publish page, so follow the packages there. Navigating to the
            // page one is already on would only stack up history entries.
            if (pathname !== PublishRoutes.ROOT) {
                navigate(PublishRoutes.ROOT);
            }
        },
        [user, publish, navigate, pathname]
    );

    return { dragging, over, dropProps: { onDragEnter: onDragOver, onDragOver, onDragLeave, onDrop } };
};
