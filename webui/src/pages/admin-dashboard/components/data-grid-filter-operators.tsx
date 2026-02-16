/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import React, { FC } from 'react';
import { Autocomplete, TextField } from '@mui/material';
import { GridFilterOperator, GridFilterInputValueProps } from '@mui/x-data-grid';

/**
 * Custom multi-select filter input component for DataGrid columns.
 * Renders an Autocomplete with multiple selection support.
 */
export const MultiSelectFilterInput: FC<GridFilterInputValueProps & { options: string[] }> = ({
  item,
  applyValue,
  options
}) => {
  const handleChange = (_event: React.SyntheticEvent, newValue: string[]) => {
    applyValue({ ...item, value: newValue });
  };

  return (
    <Autocomplete
      multiple
      size='small'
      options={options}
      value={(item.value as string[]) || []}
      onChange={handleChange}
      renderInput={(params) => (
        <TextField {...params} variant='standard' placeholder='Filter...' />
      )}
      sx={{ minWidth: 150, mt: 'auto' }}
    />
  );
};

/**
 * Creates filter operators for single-value columns with multi-select capability.
 * Includes "is any of" and "is none of" operators.
 *
 * @param options - Array of possible values to select from
 * @returns Array of GridFilterOperator for use in column definition
 *
 */
export const createMultiSelectFilterOperators = (options: string[]): GridFilterOperator[] => [
  {
    label: 'is any of',
    value: 'isAnyOf',
    getApplyFilterFn: (filterItem) => {
      if (!filterItem.value || (filterItem.value as string[]).length === 0) {
        return null;
      }
      const filterValues = filterItem.value as string[];

      return (value) => filterValues.indexOf(value as string) !== -1;
    },
    InputComponent: (props: GridFilterInputValueProps) => (
      <MultiSelectFilterInput {...props} options={options} />
    ),
  },
  {
    label: 'is none of',
    value: 'isNoneOf',
    getApplyFilterFn: (filterItem) => {
      if (!filterItem.value || (filterItem.value as string[]).length === 0) {
        return null;
      }
      const filterValues = filterItem.value as string[];

      return (value) => filterValues.indexOf(value as string) === -1;
    },
    InputComponent: (props: GridFilterInputValueProps) => (
      <MultiSelectFilterInput {...props} options={options} />
    ),
  },
];

/**
 * Creates filter operators for array-type columns with multi-select capability.
 * Includes "contains any of" and "contains none of" operators.
 *
 * @param options - Array of possible values to select from
 * @returns Array of GridFilterOperator for use in column definition
 *
 */
export const createArrayContainsFilterOperators = (options: string[]): GridFilterOperator[] => [
  {
    label: 'contains any of',
    value: 'containsAnyOf',
    getApplyFilterFn: (filterItem) => {
      if (!filterItem.value || (filterItem.value as string[]).length === 0) {
        return null;
      }
      const filterValues = filterItem.value as string[];

      return (value) => filterValues.some(fv => (value as string[]).indexOf(fv) !== -1);
    },
    InputComponent: (props: GridFilterInputValueProps) => (
      <MultiSelectFilterInput {...props} options={options} />
    ),
  },
  {
    label: 'contains none of',
    value: 'containsNoneOf',
    getApplyFilterFn: (filterItem) => {
      if (!filterItem.value || (filterItem.value as string[]).length === 0) {
        return null;
      }
      const filterValues = filterItem.value as string[];

      return (value) => !filterValues.some(fv => (value as string[]).indexOf(fv) !== -1);
    },
    InputComponent: (props: GridFilterInputValueProps) => (
      <MultiSelectFilterInput {...props} options={options} />
    ),
  },
];
