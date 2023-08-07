/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import React, { FunctionComponent } from 'react';
import { Link, Typography, Container } from '@mui/material';
import { styled, Theme } from '@mui/material/styles';

const Heading = styled(Typography)(({ theme }: { theme: Theme }) => ({ marginTop: theme.spacing(4) }));
const Paragraph = styled(Typography)(({ theme }: { theme: Theme }) => ({ margingTop: theme.spacing(2) }));

const About: FunctionComponent = () => {
    return <Container maxWidth='md'>
        <Heading variant='h4'>About This Service</Heading>
        <Paragraph variant='body1'>
            Open VSX is an open-source registry for VS Code extensions.
            It can be used by any development environment that supports such extensions.
            See <Link color='secondary' underline='hover' href='https://www.eclipse.org/community/eclipse_newsletter/2020/march/1.php'>this article</Link> for
            more information.
        </Paragraph>
        <Paragraph variant='body1'>
            This instance of Open VSX uses the default UI from
            the <Link color='secondary' underline='hover' href='https://www.npmjs.com/package/openvsx-webui'>openvsx-webui npm package</Link>,
            which is also included in
            the <Link color='secondary' underline='hover' href='https://github.com/eclipse/openvsx/pkgs/container/openvsx-webui'>openvsx-webui Docker image</Link>.
        </Paragraph>
    </Container>;
};

export default About;
