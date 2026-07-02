/******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/

import { FunctionComponent, lazy, Suspense, useContext, useEffect, useState } from 'react';
import { Routes, Route, useNavigate } from 'react-router-dom';
import { Box } from '@mui/material';
import { styled } from '@mui/material/styles';
import { Banner } from '../components/banner';
import { ShortcutsModal } from '../components/shortcuts-modal';
import { MainContext } from '../context';
import { SearchProvider } from '../context/search/search-context';
import { SearchFocusProvider } from '../context/search/search-focus-context';
import { KeyboardShortcutsProvider } from '../keyboard-shortcuts-context';
import { useShortcut } from '../use-shortcut';
import { getCookieValueByKey, setCookie } from '../utils';
import { UserData } from '../extension-registry-types';
import { ExtensionListRoutes } from '../pages/extension-list/extension-list-routes';
import { UserSettingsRoutes } from '../pages/user/user-settings-routes';
import { NamespaceDetailRoutes } from '../pages/namespace-detail/namespace-detail-routes';
import { ExtensionDetailRoutes } from '../pages/extension-detail/extension-detail-routes';
import { ExtensionDetail } from '../pages/extension-detail/extension-detail';
import { HomePage } from '../pages/home/home-page';
import { SearchPage } from '../pages/search/search-page';
import { NamespaceDetail } from '../pages/namespace-detail/namespace-detail';
import { NotFound } from '../not-found';
import { AppNavbar } from './app-navbar';
import { AppFooter } from './app-footer';

const UserSettings = lazy(() => import('../pages/user/user-settings').then(m => ({ default: m.UserSettings })));

const Wrapper = styled(Box)({
    display: 'flex',
    flexDirection: 'column',
    position: 'relative',
    minHeight: '100vh'
});

const AppLayoutContent: FunctionComponent<AppLayoutProps> = props => {
    const { pageSettings, loginProviders } = useContext(MainContext);
    const { additionalRoutes: AdditionalRoutes, banner: BannerComponent } = pageSettings.elements;

    const navigate = useNavigate();
    const [isBannerOpen, setIsBannerOpen] = useState(false);
    const [shortcutsOpen, setShortcutsOpen] = useState(false);

    useEffect(() => {
        const banner = pageSettings.elements.banner;
        if (banner) {
            let open = true;
            if (banner.cookie) {
                const bannerClosedCookie = getCookieValueByKey(banner.cookie.key);
                if (bannerClosedCookie === banner.cookie.value) open = false;
            }
            setIsBannerOpen(open);
        }
    }, []);

    useShortcut({ key: '?', label: 'Show keyboard shortcuts', order: 0, callback: () => setShortcutsOpen(true) });
    useShortcut({
        key: 'd',
        label: 'Go to documentation',
        order: 2,
        callback: () => window.open('https://github.com/eclipse/openvsx/wiki', '_blank')
    });
    useShortcut({
        key: 'p',
        label: 'Publish extension',
        order: 3,
        callback: () => navigate('/user-settings/extensions'),
        enabled: !!loginProviders
    });
    useShortcut({
        key: 'h',
        label: 'Go to home',
        order: 4,
        callback: () => navigate('/')
    });
    useShortcut({
        key: 's',
        label: 'Go to search',
        order: 5,
        callback: () => navigate('/search')
    });

    const onDismissBannerButtonClick = () => {
        const onClose = pageSettings.elements.banner?.props?.onClose;
        if (onClose) onClose();
        const cookie = pageSettings.elements.banner?.cookie;
        if (cookie) setCookie(cookie);
        setIsBannerOpen(false);
    };

    return (
        <Wrapper>
            <AppNavbar />
            {BannerComponent ? (
                <Banner
                    open={isBannerOpen}
                    showDismissButton={BannerComponent.props?.dismissButton?.show}
                    dismissButtonLabel={BannerComponent.props?.dismissButton?.label}
                    dismissButtonOnClick={onDismissBannerButtonClick}
                    color={BannerComponent.props?.color}
                    theme={pageSettings.themeType}>
                    <BannerComponent.content />
                </Banner>
            ) : null}
            <Box>
                <Suspense fallback={null}>
                    <Routes>
                        <Route path={ExtensionListRoutes.MAIN} element={<HomePage />} />
                        <Route path={ExtensionListRoutes.SEARCH} element={<SearchPage />} />
                        <Route
                            path={UserSettingsRoutes.MAIN}
                            element={<UserSettings userLoading={props.userLoading} />}
                        />
                        <Route
                            path={UserSettingsRoutes.DELETE_EXTENSION}
                            element={<UserSettings userLoading={props.userLoading} />}
                        />
                        <Route path={NamespaceDetailRoutes.MAIN} element={<NamespaceDetail />} />
                        <Route path={ExtensionDetailRoutes.MAIN} element={<ExtensionDetail />} />
                        <Route path={ExtensionDetailRoutes.MAIN_TARGET} element={<ExtensionDetail />} />
                        {AdditionalRoutes ?? null}
                        <Route path='*' element={<NotFound />} />
                    </Routes>
                </Suspense>
            </Box>
            <AppFooter onOpenShortcuts={() => setShortcutsOpen(true)} />
            <ShortcutsModal open={shortcutsOpen} onClose={() => setShortcutsOpen(false)} />
        </Wrapper>
    );
};

export const AppLayout: FunctionComponent<AppLayoutProps> = props => (
    <KeyboardShortcutsProvider>
        <SearchProvider>
            <SearchFocusProvider>
                <AppLayoutContent {...props} />
            </SearchFocusProvider>
        </SearchProvider>
    </KeyboardShortcutsProvider>
);

export interface AppLayoutProps {
    user?: UserData;
    userLoading: boolean;
}
