/********************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import { FunctionComponent } from 'react';
import { SvgIconProps } from '@mui/material';
import DataObjectIcon from '@mui/icons-material/DataObject';
import AutoAwesomeIcon from '@mui/icons-material/AutoAwesome';
import SpellcheckIcon from '@mui/icons-material/Spellcheck';
import FormatAlignLeftIcon from '@mui/icons-material/FormatAlignLeft';
import ContentCopyIcon from '@mui/icons-material/ContentCopy';
import PaletteIcon from '@mui/icons-material/Palette';
import BugReportIcon from '@mui/icons-material/BugReport';
import AccountTreeIcon from '@mui/icons-material/AccountTree';
import KeyboardIcon from '@mui/icons-material/Keyboard';
import ExtensionIcon from '@mui/icons-material/Extension';
import BarChartIcon from '@mui/icons-material/BarChart';
import TranslateIcon from '@mui/icons-material/Translate';
import InsightsIcon from '@mui/icons-material/Insights';
import ModelTrainingIcon from '@mui/icons-material/ModelTraining';
import MenuBookIcon from '@mui/icons-material/MenuBook';
import GridViewIcon from '@mui/icons-material/GridView';
import { CATEGORIES, ExtensionCategory } from '../extension-registry-types';

export const DefaultCategoryIcon: FunctionComponent<SvgIconProps> = GridViewIcon;

// The category list itself lives in extension-registry-types; this record is
// compile-checked to stay exhaustive when categories are added or removed.
export const CATEGORY_ICONS: Record<ExtensionCategory, FunctionComponent<SvgIconProps>> = {
    AI: AutoAwesomeIcon,
    'Programming Languages': DataObjectIcon,
    Snippets: ContentCopyIcon,
    Linters: SpellcheckIcon,
    Themes: PaletteIcon,
    Debuggers: BugReportIcon,
    Formatters: FormatAlignLeftIcon,
    Keymaps: KeyboardIcon,
    'SCM Providers': AccountTreeIcon,
    Other: GridViewIcon,
    'Extension Packs': ExtensionIcon,
    'Language Packs': TranslateIcon,
    'Data Science': InsightsIcon,
    'Machine Learning': ModelTrainingIcon,
    Visualization: BarChartIcon,
    Notebooks: MenuBookIcon
};

const SORTED_CATEGORIES: ExtensionCategory[] = [...CATEGORIES].sort((a, b) => {
    if (a === 'Other') return 1;
    if (b === 'Other') return -1;
    return a.localeCompare(b);
});

export function useCategories(): ExtensionCategory[] {
    return SORTED_CATEGORIES;
}
