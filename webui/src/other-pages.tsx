import React, { FunctionComponent, useContext, useEffect, useState } from 'react';
import { Routes, Route } from 'react-router-dom';
import { AppBar, Box, IconButton, Toolbar } from '@mui/material';
import { styled, Theme } from '@mui/material/styles';
import AccountBoxIcon from '@mui/icons-material/AccountBox';
import { Banner } from './components/banner';
import { MainContext } from './context';
import { HeaderMenu } from './header-menu';
import { UserAvatar } from './pages/user/avatar';
import { ExtensionListContainer, ExtensionListRoutes } from './pages/extension-list/extension-list-container';
import { UserSettings, UserSettingsRoutes } from './pages/user/user-settings';
import { NamespaceDetail, NamespaceDetailRoutes } from './pages/namespace-detail/namespace-detail';
import { ExtensionDetail, ExtensionDetailRoutes } from './pages/extension-detail/extension-detail';
import { getCookieValueByKey, setCookie } from './utils';
import { UserData } from './extension-registry-types';
import { NotFound } from './not-found';

const ToolbarItem = styled(Box)({
    display: 'flex',
    alignItems: 'center'
});

const Wrapper = styled(Box)({
    display: 'flex',
    flexDirection: 'column',
    position: 'relative',
    minHeight: '100vh'
});

const Footer = styled('footer')(({ theme }: { theme: Theme }) => ({
    position: 'fixed',
    bottom: 0,
    width: '100%',
    padding: `${theme.spacing(1)} ${theme.spacing(2)}`,
    backgroundColor: theme.palette.background.paper,
    boxShadow: '0px -2px 6px 0px rgba(0, 0, 0, 0.5)',
    backgroundImage: theme.palette.mode == 'dark' ? 'linear-gradient(rgba(255, 255, 255, 0.08), rgba(255, 255, 255, 0.08))' : undefined
}));

export const OtherPages: FunctionComponent<OtherPagesProps> = (props) => {
    const { service, pageSettings } = useContext(MainContext);
    const {
        additionalRoutes: AdditionalRoutes,
        banner: BannerComponent,
        footer: FooterComponent,
        toolbarContent: ToolbarContent
    } = pageSettings.elements;

    const [isBannerOpen, setIsBannerOpen] = useState(false);
    const [isFooterExpanded, setIsFooterExpanded] = useState(false);

    useEffect(() => {
        // Check a cookie to determine whether a banner should be shown
        const banner = pageSettings.elements.banner;
        if (banner) {
            let open = true;
            if (banner.cookie) {
                const bannerClosedCookie = getCookieValueByKey(banner.cookie.key);
                if (bannerClosedCookie === banner.cookie.value) {
                    open = false;
                }
            }
            setIsBannerOpen(open);
        }
    }, []);

    const onDismissBannerButtonClick = () => {
        const onClose = pageSettings.elements.banner?.props?.onClose;
        if (onClose) {
            onClose();
        }
        const cookie = pageSettings.elements.banner?.cookie;
        if (cookie) {
            setCookie(cookie);
        }
        setIsBannerOpen(false);
    };

    const getContentPadding = (): number => {
        const footerHeight = pageSettings.elements.footer?.props.footerHeight;
        return footerHeight ? footerHeight + 24 : 0;
    };

    return <Wrapper>
        <AppBar position='relative' color='inherit' elevation={3}>
            <Toolbar sx={{ justifyContent: 'space-between' }}>
                <ToolbarItem>
                    {ToolbarContent ? <ToolbarContent /> : null}
                </ToolbarItem>
                <ToolbarItem>
                    <HeaderMenu />
                    {
                        props.user ?
                            <UserAvatar />
                            :
                            <IconButton
                                href={service.getLoginUrl()}
                                title='Log In'
                                aria-label='Log In' >
                                <AccountBoxIcon />
                            </IconButton>
                    }
                </ToolbarItem>
            </Toolbar>
        </AppBar>
        {
            BannerComponent ?
                <Banner
                    open={isBannerOpen}
                    showDismissButton={BannerComponent.props?.dismissButton?.show}
                    dismissButtonLabel={BannerComponent.props?.dismissButton?.label}
                    dismissButtonOnClick={onDismissBannerButtonClick}
                    color={BannerComponent.props?.color}
                    theme={pageSettings.themeType}
                >
                    <BannerComponent.content />
                </Banner>
                : null
        }
        <Box pb={`${getContentPadding()}px`}>
            <Routes>
                <Route path={ExtensionListRoutes.MAIN} element={ <ExtensionListContainer /> } />
                <Route path={UserSettingsRoutes.MAIN} element={<UserSettings userLoading={props.userLoading} />} />
                <Route path={NamespaceDetailRoutes.MAIN} element={ <NamespaceDetail /> } />
                <Route path={ExtensionDetailRoutes.MAIN} element={<ExtensionDetail />} />
                <Route path={ExtensionDetailRoutes.MAIN_TARGET} element={<ExtensionDetail />} />
                {AdditionalRoutes ? AdditionalRoutes : null}
                <Route path='*' element={<NotFound />} />
            </Routes>
        </Box>
        {
            FooterComponent ?
                <Footer
                    onMouseEnter={() => setIsFooterExpanded(true)}
                    onMouseLeave={() => setIsFooterExpanded(false)} >
                    <FooterComponent.content expanded={isFooterExpanded} />
                </Footer>
                : null
        }
    </Wrapper>;
};

export interface OtherPagesProps {
    user?: UserData;
    userLoading: boolean;
}