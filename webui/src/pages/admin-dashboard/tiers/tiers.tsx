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

import React, { FC, useState, useEffect, useRef, useMemo } from "react";
import {
  Box,
  Button,
  Paper,
  Typography,
  CircularProgress,
  Alert,
  IconButton
} from "@mui/material";
import { DataGrid, GridColDef, GridRenderCellParams } from "@mui/x-data-grid";
import EditIcon from "@mui/icons-material/Edit";
import DeleteIcon from "@mui/icons-material/Delete";
import AddIcon from "@mui/icons-material/Add";
import { MainContext } from "../../../context";
import type { Tier } from "../../../extension-registry-types";
import { TierFormDialog } from "./tier-form-dialog";
import { DeleteTierDialog } from "./delete-tier-dialog";
import { handleError } from "../../../utils";
import { createMultiSelectFilterOperators } from "../components";

export const Tiers: FC = () => {
  const abortController = useRef<AbortController>(new AbortController());
  const { service } = React.useContext(MainContext);
  const [tiers, setTiers] = useState<readonly Tier[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [formDialogOpen, setFormDialogOpen] = useState(false);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [selectedTier, setSelectedTier] = useState<Tier | undefined>();

  // load all tiers
  const loadTiers = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await service.admin.getTiers(abortController.current);
      setTiers(data.tiers);
    } catch (err: any) {
      setError(handleError(err));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadTiers();
    return () => abortController.current.abort();
  }, []);

  const handleCreateClick = () => {
    setSelectedTier(undefined);
    setFormDialogOpen(true);
  };

  const handleEditClick = (tier: Tier) => {
    setSelectedTier(tier);
    setFormDialogOpen(true);
  };

  const handleDeleteClick = (tier: Tier) => {
    setSelectedTier(tier);
    setDeleteDialogOpen(true);
  };

  const handleFormSubmit = async (formData: Tier) => {
    if (selectedTier) {
      // update existing tier
      await service.admin.updateTier(abortController.current, selectedTier.name, formData);
    } else {
      // create new tier
      await service.admin.createTier(abortController.current, formData);
    }
    await loadTiers();
  };

  const handleDeleteConfirm = async () => {
    if (selectedTier) {
      await service.admin.deleteTier(abortController.current, selectedTier.name);
      await loadTiers();
    }
  };

  const handleFormDialogClose = () => {
    setFormDialogOpen(false);
    setSelectedTier(undefined);
  };

  const handleDeleteDialogClose = () => {
    setDeleteDialogOpen(false);
    setSelectedTier(undefined);
  };

  // Extract unique values for filter dropdowns
  const refillStrategyOptions = useMemo(() =>
    [...new Set(tiers.map(t => t.refillStrategy).filter(Boolean))],
    [tiers]
  );

  const columns: GridColDef[] = [
    {
      field: 'name',
      headerName: 'Name',
      flex: 1,
      minWidth: 150
    },
    {
      field: 'description',
      headerName: 'Description',
      flex: 2,
      minWidth: 200,
      valueGetter: (value: string) => value || '-'
    },
    {
      field: 'capacity',
      headerName: 'Capacity',
      type: 'number',
      width: 120,
      valueFormatter: (value: number) => value.toLocaleString()
    },
    {
      field: 'duration',
      headerName: 'Duration (s)',
      type: 'number',
      width: 130,
      valueFormatter: (value: number) => value.toLocaleString()
    },
    {
      field: 'refillStrategy',
      headerName: 'Refill Strategy',
      width: 150,
      filterOperators: createMultiSelectFilterOperators(refillStrategyOptions)
    },
    {
      field: 'actions',
      headerName: 'Actions',
      width: 120,
      sortable: false,
      filterable: false,
      renderCell: (params: GridRenderCellParams<Tier>) => (
        <>
          <IconButton
            size='small'
            onClick={() => handleEditClick(params.row)}
            title='Edit tier'
            color='primary'
          >
            <EditIcon />
          </IconButton>
          <IconButton
            size='small'
            onClick={() => handleDeleteClick(params.row)}
            title='Delete tier'
            color='error'
          >
            <DeleteIcon />
          </IconButton>
        </>
      ),
    },
  ];

  return (
    <Box sx={{ p: 3, height: '100%', display: 'flex', flexDirection: 'column' }}>
      <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "center", mb: 3 }}>
        <Typography variant='h4' component='h1'>
          Tier Management
        </Typography>
        <Button
          variant='contained'
          startIcon={<AddIcon />}
          onClick={handleCreateClick}
          disabled={loading}
        >
          Create Tier
        </Button>
      </Box>

      { error &&
        <Alert severity='error' sx={{ mb: 2 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      }

      { loading &&
        <Box sx={{ display: "flex", justifyContent: "center", p: 4 }}>
          <CircularProgress />
        </Box>
      }

      { !loading && tiers.length === 0 &&
        <Paper elevation={0} sx={{ p: 3, textAlign: "center" }}>
          <Typography color='textSecondary' gutterBottom>
            No tiers found. Create one to get started.
          </Typography>
        </Paper>
      }

      { !loading && tiers.length > 0 &&
        <Paper elevation={0} sx={{ flex: 1, minHeight: 400, width: '100%', display: 'flex', flexDirection: 'column' }}>
          <DataGrid
            rows={tiers as Tier[]}
            columns={columns}
            getRowId={(row) => row.name}
            pageSizeOptions={[20, 35, 50]}
            initialState={{
              pagination: { paginationModel: { pageSize: 20 } },
            }}
            disableRowSelectionOnClick
            sx={{ flex: 1 }}
          />
        </Paper>
      }

      <TierFormDialog
        open={formDialogOpen}
        tier={selectedTier}
        onClose={handleFormDialogClose}
        onSubmit={handleFormSubmit}
      />

      <DeleteTierDialog
        open={deleteDialogOpen}
        tier={selectedTier}
        onClose={handleDeleteDialogClose}
        onConfirm={handleDeleteConfirm}
      />
    </Box>
  );
};
