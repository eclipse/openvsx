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
  const abortController = useRef<AbortController>();
  const { service } = React.useContext(MainContext);
  const [customers, setCustomers] = useState<Customer[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [formDialogOpen, setFormDialogOpen] = useState(false);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [selectedCustomer, setSelectedCustomer] = useState<Customer | undefined>();

  useEffect(() => {
    abortController.current = new AbortController();
    return () => abortController.current?.abort();
  }, []);

  // Load all customers
  const loadCustomers = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await service.admin.getCustomers();
      setCustomers(data as Customer[]);
    } catch (err: any) {
      setError(err.message || "Failed to load customers");
    } finally {
      setLoading(false);
    }
  }, [service]);

  useEffect(() => {
    loadCustomers();
  }, [loadCustomers]);

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

  const handleFormSubmit = async (formData: Customer) => {
    try {
      if (selectedCustomer) {
        // Update existing customer
        await service.admin.updateCustomer(selectedCustomer.name, formData);
      } else {
        // Create new customer
        await service.admin.createCustomer(formData);
      }
      await loadCustomers();
    } catch (err: any) {
      throw new Error(err.message || "Failed to save customer");
    }
  };

  const handleDeleteConfirm = async () => {
    try {
      if (selectedCustomer) {
        await service.admin.deleteCustomer(selectedCustomer.name);
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
          Customers Management
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

      {error && (
        <Alert severity='error' sx={{ mb: 2 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      {loading && (
        <Box sx={{ display: "flex", justifyContent: "center", p: 4 }}>
          <CircularProgress />
        </Box>
      )}

      {!loading && customers.length === 0 && (
        <Paper sx={{ p: 3, textAlign: "center" }}>
          <Typography color='textSecondary' gutterBottom>
            No customers found. Create one to get started.
          </Typography>
        </Paper>
      )}

      {!loading && customers.length > 0 && (
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
                  <TableCell>{customer.tierId || "-"}</TableCell>
                  <TableCell>
                    <Chip
                      label={customer.state}
                      size='small'
                      variant='outlined'
                    />
                  </TableCell>
                  <TableCell>{customer.cidrBlocks || "-"}</TableCell>
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
