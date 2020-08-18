/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import * as React from 'react';
import { Link, Typography, Container, useTheme, makeStyles } from '@material-ui/core';

const About: React.FunctionComponent = () => {
    const theme = useTheme();
    const classes = makeStyles({
        heading: {
            marginTop: theme.spacing(4)
        },
        paragraph: {
            marginTop: theme.spacing(2)
        }
    })();
    return <Container maxWidth='md'>
        <Typography variant='h4' className={classes.heading}>About This Service</Typography>
        <Typography variant='body1' className={classes.paragraph}>
            Open VSX is an open-source registry for VS Code extensions.
            It can be used by any development environment that supports such extensions.
            See <Link color='secondary' href='https://www.eclipse.org/community/eclipse_newsletter/2020/march/1.php'>this article</Link> for
            more information.
        </Typography>
        <Typography variant='body1' className={classes.paragraph}>
            This instance of Open VSX uses the default UI from
            the <Link color='secondary' href='https://www.npmjs.com/package/openvsx-webui'>openvsx-webui npm package</Link>,
            which is also included in
            the <Link color='secondary' href='https://github.com/eclipse/openvsx/packages/324117'>openvsx-webui Docker image</Link>.
        </Typography>
    </Container>;
};

export default About;
