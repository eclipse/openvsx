import React, { FunctionComponent } from 'react';
import { Box, Container, Typography } from '@mui/material';
import BrokenImageIcon from '@mui/icons-material/BrokenImage';

export const NotFound: FunctionComponent = () => {
    return <Container>
        <Box height='30vh' display='flex' flexWrap='wrap' justifyContent='center' alignItems='center'>
            <Typography variant='h3'>Oooups...this is a 404 page.</Typography>
            <BrokenImageIcon sx={{ fontSize: '4rem', flexBasis: '100%' }} />
        </Box>
    </Container>;
};