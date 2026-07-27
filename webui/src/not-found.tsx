import { FunctionComponent } from 'react';
import { Box, Typography } from '@mui/material';
import BrokenImageIcon from '@mui/icons-material/BrokenImage';
import { PageContainer } from './components/page-container';

export const NotFound: FunctionComponent = () => {
    return (
        <PageContainer flushTop>
            <Box height='30vh' display='flex' flexWrap='wrap' justifyContent='center' alignItems='center'>
                <Typography variant='h3'>Oooups...this is a 404 page.</Typography>
                <BrokenImageIcon sx={{ fontSize: '4rem', flexBasis: '100%' }} />
            </Box>
        </PageContainer>
    );
};
