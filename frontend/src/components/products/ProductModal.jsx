import { useState, useEffect } from 'react';
import {
  Dialog, DialogTitle, DialogContent, DialogActions,
  TextField, Button, Alert, Box, CircularProgress
} from '@mui/material';

const emptyForm = {
  name: '',
  sku: '',
  description: '',
  categoryName: '',
  price: '',
  stock: '',
  weightKg: ''
};

function parseFieldErrors(data) {
  if (data.errors && typeof data.errors === 'object') {
    return { ...data.errors };
  }
  if (Array.isArray(data.fieldErrors)) {
    const mapped = {};
    data.fieldErrors.forEach(({ field, message }) => {
      mapped[field] = message;
    });
    return mapped;
  }
  return null;
}

export default function ProductModal({ open, onClose, onSave, product }) {
  const [form, setForm] = useState(emptyForm);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [fieldErrors, setFieldErrors] = useState({});

  useEffect(() => {
    if (product) {
      setForm({
        name: product.name || '',
        sku: product.sku || '',
        description: product.description || '',
        categoryName: product.categoryName || '',
        price: product.price != null ? String(product.price) : '',
        stock: product.stock != null ? String(product.stock) : '',
        weightKg: product.weightKg != null ? String(product.weightKg) : ''
      });
    } else {
      setForm(emptyForm);
    }
    setError('');
    setFieldErrors({});
  }, [product, open]);

  const handleChange = (field) => (e) => {
    setForm((prev) => ({ ...prev, [field]: e.target.value }));
    setFieldErrors((prev) => ({ ...prev, [field]: undefined }));
  };

  const handleSave = async () => {
    setSaving(true);
    setError('');
    setFieldErrors({});
    try {
      const data = {
        name: form.name,
        sku: form.sku,
        description: form.description || null,
        categoryName: form.categoryName || null,
        price: parseFloat(form.price),
        stock: parseInt(form.stock, 10),
        weightKg: form.weightKg ? parseFloat(form.weightKg) : null
      };
      await onSave(data);
    } catch (err) {
      const resp = err.response;
      if (resp && resp.status === 400 && resp.data) {
        const parsed = typeof resp.data === 'object' ? parseFieldErrors(resp.data) : null;
        if (parsed && Object.keys(parsed).length > 0) {
          setFieldErrors(parsed);
        } else if (typeof resp.data === 'object' && resp.data.message) {
          setError(resp.data.message);
        } else if (typeof resp.data === 'string') {
          setError(resp.data);
        } else {
          setError('Validation failed. Please check your input.');
        }
      } else if (resp && resp.status === 409) {
        setError(resp.data?.message || 'A product with this SKU already exists.');
      } else {
        setError('An unexpected error occurred.');
      }
    } finally {
      setSaving(false);
    }
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>{product ? 'Edit Product' : 'Add Product'}</DialogTitle>
      <DialogContent>
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, mt: 1 }}>
          {error && <Alert severity="error">{error}</Alert>}
          <TextField
            label="Name"
            value={form.name}
            onChange={handleChange('name')}
            required
            fullWidth
            error={!!fieldErrors.name}
            helperText={fieldErrors.name || ''}
          />
          <TextField
            label="SKU"
            value={form.sku}
            onChange={handleChange('sku')}
            required
            fullWidth
            error={!!fieldErrors.sku}
            helperText={fieldErrors.sku || ''}
          />
          <TextField
            label="Description"
            value={form.description}
            onChange={handleChange('description')}
            multiline
            rows={3}
            fullWidth
            error={!!fieldErrors.description}
            helperText={fieldErrors.description || ''}
          />
          <TextField
            label="Category"
            value={form.categoryName}
            onChange={handleChange('categoryName')}
            required
            fullWidth
            error={!!fieldErrors.categoryName}
            helperText={fieldErrors.categoryName || ''}
          />
          <TextField
            label="Price"
            value={form.price}
            onChange={handleChange('price')}
            type="number"
            required
            fullWidth
            inputProps={{ min: 0, step: '0.01' }}
            error={!!fieldErrors.price}
            helperText={fieldErrors.price || ''}
          />
          <TextField
            label="Stock"
            value={form.stock}
            onChange={handleChange('stock')}
            type="number"
            required
            fullWidth
            inputProps={{ min: 0, step: 1 }}
            error={!!fieldErrors.stock}
            helperText={fieldErrors.stock || ''}
          />
          <TextField
            label="Weight (kg)"
            value={form.weightKg}
            onChange={handleChange('weightKg')}
            type="number"
            fullWidth
            inputProps={{ min: 0, step: '0.001' }}
            error={!!fieldErrors.weightKg}
            helperText={fieldErrors.weightKg || ''}
          />
        </Box>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} disabled={saving}>Cancel</Button>
        <Button
          onClick={handleSave}
          variant="contained"
          disabled={saving}
          startIcon={saving ? <CircularProgress size={16} /> : null}
        >
          Save
        </Button>
      </DialogActions>
    </Dialog>
  );
}
