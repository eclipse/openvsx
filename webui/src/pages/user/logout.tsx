/** ******************************************************************************
 * Copyright (c) 2024 Precies. Software OU and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */

import { FunctionComponent, PropsWithChildren, useContext, useEffect, useRef, useState } from 'react';
import { Button } from '@mui/material';
import { styled } from '@mui/material/styles';
import { isError, CsrfTokenJson } from '../../extension-registry-types';
import { MainContext } from '../../context';

const LogoutButton = styled(Button)({
    cursor: 'pointer',
    textDecoration: 'none',
    border: 'none',
    background: 'none',
    padding: 0
});

export const LogoutForm: FunctionComponent<PropsWithChildren> = ({ children }) => {
    const [csrf, setCsrf] = useState<string>();
    const context = useContext(MainContext);

    const abortController = useRef<AbortController>(new AbortController());
    useEffect(() => {
        updateCsrf();
        return () => abortController.current.abort();
    }, []);

    const updateCsrf = async () => {
        try {
            const csrfResponse = await context.service.getCsrfToken(abortController.current);
            if (!isError(csrfResponse)) {
                const csrfToken = csrfResponse as CsrfTokenJson;
                setCsrf(csrfToken.value);
            }
        } catch (err) {
            context.handleError(err);
        }
    };

    return <form method='post' action={context.service.getLogoutUrl()}>
        {csrf ? <input name='_csrf' type='hidden' value={csrf} /> : null}
        <LogoutButton type='submit'>
            {children}
        </LogoutButton>
    </form>;
};