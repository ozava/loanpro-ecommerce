import { useState, useEffect, useCallback, useRef } from 'react';
import {
  Box, Typography, TextField, Grid, InputAdornment, Fab, Badge, Skeleton, Card,
  CardContent, CardActions, Snackbar, Alert, Pagination
} from '@mui/material';
import { Search, ShoppingCart } from '@mui/icons-material';
import ProductCard from '../components/store/ProductCard';
import CartDrawer from '../components/cart/CartDrawer';
import { useCart } from '../context/CartContext';
import { getProducts, searchProducts } from '../api/apiClient';
import { useNavigate } from 'react-router-dom';

function SkeletonCard() {
  return (
    <Card sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      <CardContent sx={{ flexGrow: 1 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
          <Skeleton variant="circular" width={40} height={40} sx={{ mr: 2 }} />
          <Skeleton variant="text" width="60%" height={32} />
        </Box>
        <Skeleton variant="rectangular" width={80} height={24} sx={{ mb: 1, borderRadius: 1 }} />
        <Skeleton variant="text" width="40%" height={32} />
        <Skeleton variant="text" width="50%" />
      </CardContent>
      <CardActions sx={{ px: 2, pb: 2 }}>
        <Skeleton variant="rectangular" width="100%" height={36} sx={{ borderRadius: 1 }} />
      </CardActions>
    </Card>
  );
}

export default function StorePage() {
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [snackbar, setSnackbar] = useState({ open: false, message: '', severity: 'error' });
  const { cartCount, drawerOpen, setDrawerOpen } = useCart();
  const navigate = useNavigate();
  const debounceRef = useRef(null);

  const fetchProducts = useCallback(async (query = '', p = 0) => {
    setLoading(true);
    try {
      const response = query
        ? await searchProducts(query, p, 12)
        : await getProducts(p, 12);
      setProducts(response.data.content);
      setTotalPages(response.data.totalPages);
      setTotalElements(response.data.totalElements);
    } catch {
      setSnackbar({ open: true, message: 'Failed to load products', severity: 'error' });
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

  return (
    <Box>
      <Box sx={{ mb: 3 }}>
        <TextField
          fullWidth
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
      </Box>

      <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
        {totalElements} products available
      </Typography>

      {loading ? (
        <Grid container spacing={3}>
          {Array.from({ length: 8 }).map((_, i) => (
            <Grid item xs={12} sm={6} md={4} lg={3} key={i}>
              <SkeletonCard />
            </Grid>
          ))}
        </Grid>
      ) : products.length === 0 ? (
        <Box sx={{ textAlign: 'center', py: 6 }}>
          <Typography variant="h6" color="text.secondary">No products found</Typography>
        </Box>
      ) : (
        <Grid container spacing={3}>
          {products.map((product) => (
            <Grid item xs={12} sm={6} md={4} lg={3} key={product.id}>
              <ProductCard product={product} />
            </Grid>
          ))}
        </Grid>
      )}

      <Pagination
        count={totalPages}
        page={page + 1}
        onChange={(e, value) => {
          setPage(value - 1);
          window.scrollTo({ top: 0, behavior: 'smooth' });
        }}
        color="primary"
        sx={{ mt: 4, display: 'flex', justifyContent: 'center' }}
      />

      <Fab
        color="primary"
        sx={{ position: 'fixed', bottom: 24, right: 24 }}
        onClick={() => setDrawerOpen(true)}
      >
        <Badge badgeContent={cartCount} color="error">
          <ShoppingCart />
        </Badge>
      </Fab>

      <CartDrawer
        open={drawerOpen}
        onClose={() => setDrawerOpen(false)}
        onCheckout={() => navigate('/checkout')}
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
