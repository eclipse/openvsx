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

import React, { FC, useState, useEffect, useRef } from "react";
import {
  Box,
  Button,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography,
  CircularProgress,
  Alert,
  IconButton,
  Stack
} from "@mui/material";
import EditIcon from "@mui/icons-material/Edit";
import DeleteIcon from "@mui/icons-material/Delete";
import AddIcon from "@mui/icons-material/Add";
import { MainContext } from "../../../context";
import type { Tier } from "../../../extension-registry-types";
import { TierFormDialog } from "./tier-form-dialog";
import { DeleteTierDialog } from "./delete-tier-dialog";

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
      setError(err.message || "Failed to load tiers");
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
    try {
      if (selectedTier) {
        // update existing tier
        await service.admin.updateTier(abortController.current, selectedTier.name, formData);
      } else {
        // create new tier
        await service.admin.createTier(abortController.current, formData);
      }
      await loadTiers();
    } catch (err: any) {
      throw new Error(err.message || "Failed to save tier");
    }
  };

  const handleDeleteConfirm = async () => {
    try {
      if (selectedTier) {
        await service.admin.deleteTier(abortController.current, selectedTier.name);
        await loadTiers();
      }
    } catch (err: any) {
      throw new Error(err.message || "Failed to delete tier");
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

  return (
    <Box sx={{ p: 3 }}>
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
        <Paper sx={{ p: 3, textAlign: "center" }}>
          <Typography color='textSecondary' gutterBottom>
            No tiers found. Create one to get started.
          </Typography>
        </Paper>
      }

      { !loading && tiers.length > 0 &&
        <TableContainer component={Paper}>
          <Table>
            <TableHead>
              <TableRow sx={{ backgroundColor: "#f5f5f5" }}>
                <TableCell sx={{ fontWeight: "bold" }}>Name</TableCell>
                <TableCell sx={{ fontWeight: "bold" }}>Description</TableCell>
                <TableCell align='right' sx={{ fontWeight: "bold" }}>
                  Capacity
                </TableCell>
                <TableCell align='right' sx={{ fontWeight: "bold" }}>
                  Duration (s)
                </TableCell>
                <TableCell sx={{ fontWeight: "bold" }}>Refill Strategy</TableCell>
                <TableCell align='center' sx={{ fontWeight: "bold" }}>
                  Actions
                </TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {tiers.map(tier => (
                <TableRow key={tier.name} hover>
                  <TableCell>{tier.name}</TableCell>
                  <TableCell>{tier.description || "-"}</TableCell>
                  <TableCell align='right'>{tier.capacity.toLocaleString()}</TableCell>
                  <TableCell align='right'>{tier.duration.toLocaleString()}</TableCell>
                  <TableCell>{tier.refillStrategy}</TableCell>
                  <TableCell align='center'>
                    <Stack direction='row' spacing={0.5} justifyContent='center'>
                      <IconButton
                        size='small'
                        onClick={() => handleEditClick(tier)}
                        title='Edit tier'
                        color='primary'
                      >
                        <EditIcon fontSize='small' />
                      </IconButton>
                      <IconButton
                        size='small'
                        onClick={() => handleDeleteClick(tier)}
                        title='Delete tier'
                        color='error'
                      >
                        <DeleteIcon fontSize='small' />
                      </IconButton>
                    </Stack>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
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
