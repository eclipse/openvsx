import typescriptEslint from '@typescript-eslint/eslint-plugin';
import react from 'eslint-plugin-react';
import tsParser from '@typescript-eslint/parser';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import js from '@eslint/js';
import { FlatCompat } from '@eslint/eslintrc';
import { reactRefresh } from 'eslint-plugin-react-refresh';
import prettierRecommended from 'eslint-plugin-prettier/recommended';
import * as parserPlain from 'eslint-parser-plain';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const compat = new FlatCompat({
    baseDirectory: __dirname,
    recommendedConfig: js.configs.recommended,
    allConfig: js.configs.all
});

export default [
    { ignores: ['node_modules/**', 'lib/**', 'dist/**', 'playwright-report/**', 'test-results/**', '.yarn/**'] },
    { settings: { react: { version: 'detect' } } },
    ...compat.extends(
        'eslint:recommended',
        'plugin:@typescript-eslint/eslint-recommended',
        'plugin:@typescript-eslint/recommended',
        'plugin:react/recommended'
    ),
    reactRefresh.configs.vite(),
    {
        // JS / JSX / TS / TSX / Flow — tsParser handles all of them
        files: ['**/*.{js,jsx,mjs,cjs,ts,tsx,mts,cts}'],
        plugins: {
            '@typescript-eslint': typescriptEslint,
            react
        },

        languageOptions: {
            parser: tsParser
        },

        rules: {
            '@typescript-eslint/ban-types': 'off',
            '@typescript-eslint/no-empty-function': 'off',
            '@typescript-eslint/no-empty-interface': 'off',
            '@typescript-eslint/no-explicit-any': 'off',
            '@typescript-eslint/no-inferrable-types': 'off',
            '@typescript-eslint/no-namespace': 'off',
            '@typescript-eslint/no-non-null-assertion': 'off',
            '@typescript-eslint/no-unused-vars': 'off',
            '@typescript-eslint/no-var-requires': 'off',
            'react/prop-types': 'off',
            'react/react-in-jsx-scope': ['off']
        }
    },
    {
        // JSON / CSS-Less-SCSS / YAML / GraphQL / Vue / Handlebars (Angular
        // templates inherit via .html below). Prettier infers the right
        // sub-parser from the extension.
        files: ['**/*.{json,json5,jsonc,css,scss,less,yaml,yml,graphql,gql,vue,hbs}'],
        languageOptions: { parser: parserPlain }
    },
    prettierRecommended,
    {
        // eslint-plugin-prettier forces a JS parser on .md/.mdx/.html unless
        // the ESLint parser is named eslint-mdx / @html-eslint/parser. Bypass
        // that by passing the Prettier parser explicitly via the rule option.
        files: ['**/*.{md,markdown}'],
        languageOptions: { parser: parserPlain },
        rules: { 'prettier/prettier': ['error', { parser: 'markdown' }] }
    },
    {
        files: ['**/*.mdx'],
        languageOptions: { parser: parserPlain },
        rules: { 'prettier/prettier': ['error', { parser: 'mdx' }] }
    },
    {
        files: ['**/*.{html,htm}'],
        languageOptions: { parser: parserPlain },
        rules: { 'prettier/prettier': ['error', { parser: 'html' }] }
    }
];
