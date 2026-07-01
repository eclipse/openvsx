/******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/

import { FunctionComponent } from 'react';
import { SvgIconProps } from '@mui/material';
import DataObjectIcon from '@mui/icons-material/DataObject';
import AutoAwesomeIcon from '@mui/icons-material/AutoAwesome';
import SpellcheckIcon from '@mui/icons-material/Spellcheck';
import FormatAlignLeftIcon from '@mui/icons-material/FormatAlignLeft';
import ContentCopyIcon from '@mui/icons-material/ContentCopy';
import PaletteIcon from '@mui/icons-material/Palette';
import BugReportIcon from '@mui/icons-material/BugReport';
import ScienceIcon from '@mui/icons-material/Science';
import AccountTreeIcon from '@mui/icons-material/AccountTree';
import KeyboardIcon from '@mui/icons-material/Keyboard';
import ExtensionIcon from '@mui/icons-material/Extension';
import BarChartIcon from '@mui/icons-material/BarChart';
import TranslateIcon from '@mui/icons-material/Translate';
import SchoolIcon from '@mui/icons-material/School';
import InsightsIcon from '@mui/icons-material/Insights';
import GridViewIcon from '@mui/icons-material/GridView';

export interface Category {
    label: string;
    icon: FunctionComponent<SvgIconProps>;
}

/** Edit this array to add, remove, or reorder categories. */
export const CATEGORIES: Category[] = [
    { label: 'Programming Languages', icon: DataObjectIcon },
    { label: 'AI', icon: AutoAwesomeIcon },
    { label: 'Linters', icon: SpellcheckIcon },
    { label: 'Formatters', icon: FormatAlignLeftIcon },
    { label: 'Snippets', icon: ContentCopyIcon },
    { label: 'Themes', icon: PaletteIcon },
    { label: 'Debuggers', icon: BugReportIcon },
    { label: 'Testing', icon: ScienceIcon },
    { label: 'SCM Providers', icon: AccountTreeIcon },
    { label: 'Keymaps', icon: KeyboardIcon },
    { label: 'Extension Packs', icon: ExtensionIcon },
    { label: 'Visualization', icon: BarChartIcon },
    { label: 'Language Packs', icon: TranslateIcon },
    { label: 'Education', icon: SchoolIcon },
    { label: 'Data Science', icon: InsightsIcon },
    { label: 'Other', icon: GridViewIcon }
];

export const DefaultCategoryIcon: FunctionComponent<SvgIconProps> = GridViewIcon;

export const CATEGORY_ICONS: Record<string, FunctionComponent<SvgIconProps>> = Object.fromEntries(
    CATEGORIES.map(c => [c.label, c.icon])
);
