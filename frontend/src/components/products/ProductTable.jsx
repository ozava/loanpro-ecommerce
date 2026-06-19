import { useState } from 'react';
import {
  Table, TableBody, TableCell, TableContainer, TableHead, TableRow,
  Paper, IconButton, Dialog, DialogTitle, DialogContent, DialogContentText,
  DialogActions, Button, CircularProgress, Box, Typography
} from '@mui/material';
import { Edit, Delete } from '@mui/icons-material';

export default function ProductTable({ products, onEdit, onDelete, loading }) {
  const [deleteDialog, setDeleteDialog] = useState({ open: false, product: null });

  const handleDeleteClick = (product) => {
    setDeleteDialog({ open: true, product });
  };

  const handleDeleteConfirm = () => {
    onDelete(deleteDialog.product.id);
    setDeleteDialog({ open: false, product: null });
  };

  const handleDeleteCancel = () => {
    setDeleteDialog({ open: false, product: null });
  };

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', py: 6 }}>
        <CircularProgress />
      </Box>
    );
  }

  if (products.length === 0) {
    return (
      <Box sx={{ textAlign: 'center', py: 6 }}>
        <Typography color="text.secondary">No products found</Typography>
      </Box>
    );
  }

  return (
    <>
      <TableContainer component={Paper} sx={{ overflowX: 'auto' }}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>SKU</TableCell>
              <TableCell>Name</TableCell>
              <TableCell>Category</TableCell>
              <TableCell align="right">Price</TableCell>
              <TableCell align="right">Stock</TableCell>
              <TableCell align="right">Weight (kg)</TableCell>
              <TableCell align="center">Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {products.map((product) => (
              <TableRow key={product.id} hover>
                <TableCell>{product.sku}</TableCell>
                <TableCell>{product.name}</TableCell>
                <TableCell>{product.categoryName}</TableCell>
                <TableCell align="right">
                  ${Number(product.price).toFixed(2)}
                </TableCell>
                <TableCell align="right">{product.stock}</TableCell>
                <TableCell align="right">
                  {product.weightKg != null ? Number(product.weightKg).toFixed(3) : '—'}
                </TableCell>
                <TableCell align="center">
                  <IconButton size="small" onClick={() => onEdit(product)}>
                    <Edit fontSize="small" />
                  </IconButton>
                  <IconButton size="small" color="error" onClick={() => handleDeleteClick(product)}>
                    <Delete fontSize="small" />
                  </IconButton>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>

      <Dialog open={deleteDialog.open} onClose={handleDeleteCancel}>
        <DialogTitle>Confirm Delete</DialogTitle>
        <DialogContent>
          <DialogContentText>
            Are you sure you want to delete {deleteDialog.product?.name}?
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleDeleteCancel}>Cancel</Button>
          <Button onClick={handleDeleteConfirm} color="error" variant="contained">
            Delete
          </Button>
        </DialogActions>
      </Dialog>
    </>
  );
}
