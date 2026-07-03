/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

export * from './main';
export * from './page-settings';
export * from './extension-registry-service';
export * from './extension-registry-types';
export * from './pages/extension-detail/extension-detail';
export * from './components/extension-list';
export * from './pages/namespace-detail/namespace-detail';
export * from './pages/user/user-settings';

// Building blocks of the built-in home page, for composing custom home pages.
export { HeroSearch, type HeroSearchProps } from './pages/home/hero-search';
export { BrowseCategories, type BrowseCategoriesProps } from './pages/home/browse-categories';
export { CuratedSections, type CuratedSectionsProps } from './pages/home/curated-sections';
export { GetInvolved, type GetInvolvedProps } from './pages/home/get-involved';
export {
    useHomeCategories,
    useCuratedRows,
    type CuratedRow,
    DEFAULT_CURATED_SECTIONS
} from './pages/home/use-home-data';
export { ExtensionCard, type ExtensionCardProps } from './components/extension-card';
export * from './components/page-primitives';
export { useSearch } from './hooks/use-search';
export { useRegisterPageSearchBar } from './context/search/search-focus-context';
