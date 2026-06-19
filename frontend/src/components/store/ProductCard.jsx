import { useState } from 'react';
import {
  Card, CardContent, CardActions, Typography, Avatar, Chip, Button, IconButton, Box
} from '@mui/material';
import { Add, Remove } from '@mui/icons-material';
import { useCart } from '../../context/CartContext';

const categoryColors = {
  'Electronics': '#1976d2',
  'Clothing': '#9c27b0',
  'Home & Garden': '#2e7d32',
  'Sports': '#ed6c02',
  'Beauty': '#e91e63',
  'Food & Beverage': '#795548',
  'Toys': '#ff9800',
  'Automotive': '#607d8b',
};
const DEFAULT_COLOR = '#455a64';

export default function ProductCard({ product }) {
  const { cart, addToCart, updateQuantity } = useCart();
  const [qty, setQty] = useState(1);
  const cartItem = cart.find((item) => item.product.id === product.id);
  const cartQty = cartItem ? cartItem.quantity : 0;
  const categoryName = product.categoryName || product.category?.name || '';
  const color = categoryColors[categoryName] || DEFAULT_COLOR;

  return (
    <Card sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      <CardContent sx={{ flexGrow: 1 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
          <Avatar sx={{ bgcolor: color, mr: 2 }}>
            {categoryName.charAt(0) || '?'}
          </Avatar>
          <Typography variant="h6" sx={{ lineHeight: 1.2 }}>
            {product.name}
          </Typography>
        </Box>
        <Chip label={categoryName} size="small" sx={{ mb: 1 }} />
        <Typography variant="h6" color="primary" sx={{ fontWeight: 'bold', mb: 1 }}>
          ${Number(product.price).toFixed(2)}
        </Typography>
        <Typography
          variant="body2"
          sx={{ color: product.stock > 0 ? 'success.main' : 'error.main', fontWeight: 500 }}
        >
          {product.stock > 0 ? `In Stock (${product.stock})` : 'Out of Stock'}
        </Typography>
      </CardContent>
      <CardActions sx={{ px: 2, pb: 2, flexDirection: 'column', gap: 1 }}>
        {cartQty === 0 ? (
          <>
            <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'center', width: '100%' }}>
              <IconButton
                size="small"
                onClick={() => setQty((q) => Math.max(1, q - 1))}
                disabled={qty <= 1}
              >
                <Remove />
              </IconButton>
              <Typography sx={{ mx: 2, fontWeight: 'bold' }}>{qty}</Typography>
              <IconButton
                size="small"
                onClick={() => setQty((q) => Math.min(product.stock, q + 1))}
                disabled={qty >= product.stock}
              >
                <Add />
              </IconButton>
            </Box>
            <Button
              variant="contained"
              fullWidth
              disabled={product.stock === 0}
              onClick={() => {
                addToCart(product, qty);
                setQty(1);
              }}
            >
              ADD TO CART
            </Button>
          </>
        ) : (
          <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'center', width: '100%' }}>
            <IconButton
              size="small"
              onClick={() => updateQuantity(product.id, cartQty - 1)}
            >
              <Remove />
            </IconButton>
            <Typography sx={{ mx: 2, fontWeight: 'bold' }}>{cartQty}</Typography>
            <IconButton
              size="small"
              onClick={() => updateQuantity(product.id, cartQty + 1)}
              disabled={cartQty >= product.stock}
            >
              <Add />
            </IconButton>
          </Box>
        )}
      </CardActions>
    </Card>
  );
}
