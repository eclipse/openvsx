import { Box, styled, Theme, Typography } from "@mui/material";
import { Link as RouteLink } from 'react-router-dom';
import React, { FC } from "react";
import { PageSettings } from "../page-settings";
import { UserSettingsRoutes } from "../pages/user/user-settings";

const link = ({ theme }: { theme: Theme }) => ({
    color: theme.palette.secondary.main,
    textDecoration: 'none',
    '&:hover': {
        textDecoration: 'underline'
    }
});

const EmptyTypography = styled(Typography)(({ theme }: { theme: Theme }) => ({
    [theme.breakpoints.down('sm')]: {
        textAlign: 'center'
    }
}));

const StyledRouteLink = styled(RouteLink)(link);


export const DefaultAgreementNotSignedContent: FC<{ pageSettings: PageSettings }> = ({ pageSettings }) => (
        <Box>
            <EmptyTypography variant='body1'>
                Access tokens cannot be created as you currently do not have a {pageSettings.agreement.name} signed. Please return to
                your <StyledRouteLink to={UserSettingsRoutes.PROFILE}>Profile</StyledRouteLink> page
                to sign the Publisher Agreement.
            </EmptyTypography>
        </Box>
);