/** ******************************************************************************
 * Copyright (c) 2024 Precies. Software OU and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */

import React, { FunctionComponent, PropsWithChildren } from 'react';
import { Button } from '@mui/material';
import { styled } from '@mui/material/styles';
import { isError, CsrfTokenJson } from '../../extension-registry-types';
import { logoutUrl, useGetCsrfTokenQuery } from '../../store/api';

const LogoutButton = styled(Button)({
    cursor: 'pointer',
    textDecoration: 'none',
    border: 'none',
    background: 'none',
    padding: 0
});

export const LogoutForm: FunctionComponent<PropsWithChildren> = ({ children }) => {
    const { data } = useGetCsrfTokenQuery();

    const csrf = data && !isError(data) ? (data as CsrfTokenJson).value : undefined;
    return <form method='post' action={logoutUrl}>
        {csrf && <input name='_csrf' type='hidden' value={csrf} /> }
        <LogoutButton type='submit'>
            {children}
        </LogoutButton>
    </form>;
};