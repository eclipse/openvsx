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

import React, { FC, useState, useEffect, useRef, useCallback } from "react";
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
  Stack,
  Chip
} from "@mui/material";
import EditIcon from "@mui/icons-material/Edit";
import DeleteIcon from "@mui/icons-material/Delete";
import AddIcon from "@mui/icons-material/Add";
import { MainContext } from "../../../context";
import type { Customer } from "../../../extension-registry-types";
import { CustomerFormDialog } from "./customer-form-dialog";
import { DeleteCustomerDialog } from "./delete-customer-dialog";

export const Customers: FC = () => {
  const abortController = useRef<AbortController>(new AbortController());
  const { service } = React.useContext(MainContext);
  const [customers, setCustomers] = useState<Customer[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [formDialogOpen, setFormDialogOpen] = useState(false);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [selectedCustomer, setSelectedCustomer] = useState<Customer | undefined>();

  // Load all customers
  const loadCustomers = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await service.admin.getCustomers(abortController.current);
      setCustomers(data.customers);
    } catch (err: any) {
      setError(err.message || "Failed to load customers");
    } finally {
      setLoading(false);
    }
  }, [service]);

  useEffect(() => {
    loadCustomers();
    return () => abortController.current.abort();
  }, []);

  const handleCreateClick = () => {
    setSelectedCustomer(undefined);
    setFormDialogOpen(true);
  };

  const handleEditClick = (customer: Customer) => {
    setSelectedCustomer(customer);
    setFormDialogOpen(true);
  };

  const handleDeleteClick = (customer: Customer) => {
    setSelectedCustomer(customer);
    setDeleteDialogOpen(true);
  };

  const handleFormSubmit = async (customer: Customer) => {
    try {
      if (selectedCustomer) {
        // update existing customer
        await service.admin.updateCustomer(abortController.current, selectedCustomer.name, customer);
      } else {
        // create new customer
        await service.admin.createCustomer(abortController.current, customer);
      }
      await loadCustomers();
    } catch (err: any) {
      throw new Error(err.message || "Failed to save customer");
    }
  };

  const handleDeleteConfirm = async () => {
    try {
      if (selectedCustomer) {
        await service.admin.deleteCustomer(abortController.current, selectedCustomer.name);
        await loadCustomers();
      }
    } catch (err: any) {
      throw new Error(err.message || "Failed to delete customer");
    }
  };

  const handleFormDialogClose = () => {
    setFormDialogOpen(false);
    setSelectedCustomer(undefined);
  };

  const handleDeleteDialogClose = () => {
    setDeleteDialogOpen(false);
    setSelectedCustomer(undefined);
  };

  return (
    <Box sx={{ p: 3 }}>
      <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "center", mb: 3 }}>
        <Typography variant='h4' component='h1'>
          Customer Management
        </Typography>
        <Button
          variant='contained'
          startIcon={<AddIcon />}
          onClick={handleCreateClick}
          disabled={loading}
        >
          Create Customer
        </Button>
      </Box>

      { error && (
        <Alert severity='error' sx={{ mb: 2 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      { loading && (
        <Box sx={{ display: "flex", justifyContent: "center", p: 4 }}>
          <CircularProgress />
        </Box>
      )}

      { !loading && customers.length === 0 && (
        <Paper sx={{ p: 3, textAlign: "center" }}>
          <Typography color='textSecondary' gutterBottom>
            No customers found. Create one to get started.
          </Typography>
        </Paper>
      )}

      { !loading && customers.length > 0 && (
        <TableContainer component={Paper}>
          <Table>
            <TableHead>
              <TableRow sx={{ backgroundColor: "#f5f5f5" }}>
                <TableCell sx={{ fontWeight: "bold" }}>Name</TableCell>
                <TableCell sx={{ fontWeight: "bold" }}>Tier</TableCell>
                <TableCell sx={{ fontWeight: "bold" }}>State</TableCell>
                <TableCell sx={{ fontWeight: "bold" }}>CIDR Blocks</TableCell>
                <TableCell align='center' sx={{ fontWeight: "bold" }}>
                  Actions
                </TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {customers.map(customer => (
                <TableRow key={customer.name} hover>
                  <TableCell>{customer.name}</TableCell>
                  <TableCell>{customer.tier?.name || "-"}</TableCell>
                  <TableCell>
                    <Chip
                      label={customer.state}
                      size='small'
                      variant='outlined'
                    />
                  </TableCell>
                  <TableCell>
                    {customer.cidrBlocks.length > 0
                        ? customer.cidrBlocks.map((value) => (
                            <tr>
                              <td>{value}</td>
                            </tr>
                        ))
                        : "-"
                    }
                  </TableCell>
                  <TableCell align='center'>
                    <Stack direction='row' spacing={0.5} justifyContent='center'>
                      <IconButton
                        size='small'
                        onClick={() => handleEditClick(customer)}
                        title='Edit customer'
                        color='primary'
                      >
                        <EditIcon fontSize='small' />
                      </IconButton>
                      <IconButton
                        size='small'
                        onClick={() => handleDeleteClick(customer)}
                        title='Delete customer'
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
      )}

      <CustomerFormDialog
        open={formDialogOpen}
        customer={selectedCustomer}
        onClose={handleFormDialogClose}
        onSubmit={handleFormSubmit}
      />

      <DeleteCustomerDialog
        open={deleteDialogOpen}
        customer={selectedCustomer}
        onClose={handleDeleteDialogClose}
        onConfirm={handleDeleteConfirm}
      />
    </Box>
  );
};
