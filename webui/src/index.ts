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

export * from './components/kbd-key';
export * from './components/openvsx-mark';

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
// Leaf hook modules keep their helpers private, so `export *` exposes only the
// public hook plus its types (e.g. useSearch + SearchFilter).
export * from './hooks/use-search';
export { useRegisterPageSearchBar } from './context/search/page-search-bar-context';

// Keyboard shortcuts: register shortcuts from custom pages/components. The hook
// only takes effect below a KeyboardShortcutsProvider — the built-in AppLayout
// mounts one, so custom layouts need to mount their own.
export { useShortcut, type UseShortcutOptions } from './hooks/use-shortcut';
export {
    KeyboardShortcutsProvider,
    useKeyboardShortcuts,
    type ShortcutInfo
} from './context/keyboard-shortcuts-context';

// Signal: lightweight cross-component coordination primitive. Create one with
// useSignal, subscribe with useSignalEffect.
export * from './hooks/use-signal';
export * from './hooks/use-signal-effect';
