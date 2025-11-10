/** ******************************************************************************
 * Copyright (c) 2025 Precies. Software OU and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
import { FunctionComponent, ReactNode, useState } from "react";
import { Button, Dialog, DialogContent, DialogTitle, Stack } from "@mui/material";

export const LoginComponent: FunctionComponent<LoginComponentProps> = (props) => {
    const [dialogOpen, setDialogOpen] = useState(false);

    const showLoginDialog = () => setDialogOpen(true);

    const providers = Object.keys(props.loginProviders);
    if (providers.length === 1) {
        return props.renderButton(props.loginProviders[providers[0]]);
    } else {
        return <>
            {props.renderButton(undefined, showLoginDialog)}
            <Dialog
                fullWidth
                open={dialogOpen}
                onClose={() => setDialogOpen(false)}
            >
                <DialogTitle>Log In</DialogTitle>
                <DialogContent>
                    <Stack spacing={2}>
                        {providers.map((provider) => (<Button key={provider} fullWidth variant='contained' color='secondary' href={props.loginProviders[provider]}>{provider}</Button>))}
                    </Stack>
                </DialogContent>
            </Dialog>
        </>;
    }
};

export interface LoginComponentProps {
    loginProviders: Record<string, string>
    renderButton: (href?: string, onClick?: () => void) => ReactNode
}
