import { useState, useEffect, useCallback, useRef } from 'react';
import {
  Box, Typography, TextField, Button, Snackbar, Alert, InputAdornment, Pagination
} from '@mui/material';
import { Add, Search, UploadFile } from '@mui/icons-material';
import ProductTable from '../components/products/ProductTable';
import ProductModal from '../components/products/ProductModal';
import CsvImportModal from '../components/products/CsvImportModal';
import {
  getProducts, searchProducts, createProduct, updateProduct, deleteProduct
} from '../api/apiClient';

export default function ProductsPage() {
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [modalOpen, setModalOpen] = useState(false);
  const [importModalOpen, setImportModalOpen] = useState(false);
  const [selectedProduct, setSelectedProduct] = useState(null);
  const [snackbar, setSnackbar] = useState({ open: false, message: '', severity: 'success' });
  const debounceRef = useRef(null);

  const fetchProducts = useCallback(async (query = '', p = 0) => {
    setLoading(true);
    try {
      const response = query
        ? await searchProducts(query, p, 10)
        : await getProducts(p, 10);
      setProducts(response.data.content);
      setTotalPages(response.data.totalPages);
      setTotalElements(response.data.totalElements);
    } catch {
      showSnackbar('Failed to load products', 'error');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchProducts();
  }, [fetchProducts]);

  useEffect(() => {
    if (debounceRef.current) clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(() => {
      setPage(0);
      fetchProducts(searchQuery, 0);
    }, 300);
    return () => clearTimeout(debounceRef.current);
  }, [searchQuery, fetchProducts]);

  useEffect(() => {
    fetchProducts(searchQuery, page);
  }, [page, fetchProducts, searchQuery]);

  const showSnackbar = (message, severity = 'success') => {
    setSnackbar({ open: true, message, severity });
  };

  const handleOpenCreate = () => {
    setSelectedProduct(null);
    setModalOpen(true);
  };

  const handleOpenEdit = (product) => {
    setSelectedProduct(product);
    setModalOpen(true);
  };

  const handleSave = async (data) => {
    if (selectedProduct) {
      await updateProduct(selectedProduct.id, data);
      showSnackbar('Product updated successfully');
    } else {
      await createProduct(data);
      showSnackbar('Product created successfully');
    }
    setModalOpen(false);
    fetchProducts(searchQuery, page);
  };

  const handleDelete = async (id) => {
    try {
      await deleteProduct(id);
      showSnackbar('Product deleted successfully');
      fetchProducts(searchQuery, page);
    } catch {
      showSnackbar('Failed to delete product', 'error');
    }
  };

  const handleImportComplete = () => {
    setImportModalOpen(false);
    fetchProducts(searchQuery, page);
    showSnackbar('CSV import completed');
  };

  return (
    <Box>
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 3, flexWrap: 'wrap' }}>
        <Typography variant="h5" sx={{ flexGrow: 1 }}>
          Product Management
        </Typography>
        <TextField
          size="small"
          placeholder="Search products..."
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          InputProps={{
            startAdornment: (
              <InputAdornment position="start">
                <Search />
              </InputAdornment>
            )
          }}
        />
        <Button
          variant="outlined"
          startIcon={<UploadFile />}
          onClick={() => setImportModalOpen(true)}
        >
          Import CSV
        </Button>
        <Button
          variant="contained"
          startIcon={<Add />}
          onClick={handleOpenCreate}
        >
          Add Product
        </Button>
      </Box>

      <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
        {totalElements} products found
      </Typography>

      <ProductTable
        products={products}
        onEdit={handleOpenEdit}
        onDelete={handleDelete}
        loading={loading}
      />

      <Pagination
        count={totalPages}
        page={page + 1}
        onChange={(e, value) => setPage(value - 1)}
        color="primary"
        sx={{ mt: 2, display: 'flex', justifyContent: 'center' }}
      />

      <ProductModal
        open={modalOpen}
        onClose={() => setModalOpen(false)}
        onSave={handleSave}
        product={selectedProduct}
      />

      <CsvImportModal
        open={importModalOpen}
        onClose={() => setImportModalOpen(false)}
        onImportComplete={handleImportComplete}
      />

      <Snackbar
        open={snackbar.open}
        autoHideDuration={4000}
        onClose={() => setSnackbar((s) => ({ ...s, open: false }))}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
      >
        <Alert
          severity={snackbar.severity}
          onClose={() => setSnackbar((s) => ({ ...s, open: false }))}
          variant="filled"
        >
          {snackbar.message}
        </Alert>
      </Snackbar>
    </Box>
  );
}
