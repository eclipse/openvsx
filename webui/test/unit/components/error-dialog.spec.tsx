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
 ********************************************************************************/

import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { ErrorDialog } from '../../../src/components/error-dialog';

const open = (errorCode?: string) =>
    render(
        <ErrorDialog
            errorMessage='Something failed.'
            errorCode={errorCode}
            isErrorDialogOpen
            handleCloseDialog={vi.fn()}
        />
    );

describe('ErrorDialog', () => {
    it('shows the error message', () => {
        open();

        expect(screen.getByText('Something failed.')).toBeInTheDocument();
    });

    it('points a failed publisher agreement to the administrators, without a contact address', () => {
        open('publisher-agreement-problem');

        expect(screen.getByText(/contact the administrators of this registry/i)).toBeInTheDocument();
        expect(screen.queryByRole('link')).not.toBeInTheDocument();
    });

    it('adds nothing to the message of a code it has no help for', () => {
        open('eclipse-mismatch-github-id');

        expect(screen.getByText('Something failed.')).toBeInTheDocument();
        expect(screen.queryByText(/eclipse/i)).not.toBeInTheDocument();
        expect(screen.queryByRole('link')).not.toBeInTheDocument();
    });
});
