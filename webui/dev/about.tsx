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
import { Box, Typography, Container } from '@material-ui/core';

const About = () => <Container maxWidth='md'>
    <Box mt={4}>
        <Typography variant='h4'>About This Service</Typography>
        <Box mt={2}>
            <Typography variant='body1'>
                This instance of Open VSX is meant only for development.
            </Typography>
        </Box>
    </Box>
</Container>;

export default About;
