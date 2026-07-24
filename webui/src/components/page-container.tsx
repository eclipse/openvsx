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

import { forwardRef } from 'react';
import { Container, ContainerProps } from '@mui/material';

export interface PageContainerProps extends ContainerProps {
    /** Full-bleed: drop the width cap and gutters so nested containers own their centering (e.g. the home). */
    fluid?: boolean;
    /** No top margin — content hugs the nav, or an inner element supplies its own. */
    flushTop?: boolean;
    /** No bottom margin — content meets the footer line, or an inner element supplies its own. */
    flushBottom?: boolean;
}

/**
 * A MUI `Container` plus the standard page margins (a top offset below the nav and
 * a gap above the footer). For page-level content; non-pages or custom spacing
 * should use `Container` directly.
 */
export const PageContainer = forwardRef<HTMLDivElement, PageContainerProps>(function PageContainer(
    { fluid, flushTop, flushBottom, sx, ...props },
    ref
) {
    return (
        <Container
            ref={ref}
            maxWidth={fluid ? false : 'xl'}
            disableGutters={fluid}
            sx={[
                {
                    pt: flushTop ? 0 : { xs: '2.75rem', sm: '4.875rem' },
                    pb: flushBottom ? 0 : { xs: '2.5rem', sm: '4rem' }
                },
                ...(Array.isArray(sx) ? sx : [sx])
            ]}
            {...props}
        />
    );
});
