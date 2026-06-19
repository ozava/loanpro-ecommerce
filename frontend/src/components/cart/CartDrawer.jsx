import {
  Drawer, Box, Typography, IconButton, Divider, Button, Badge
} from '@mui/material';
import { Close, Delete, Add, Remove } from '@mui/icons-material';
import { useCart } from '../../context/CartContext';

export default function CartDrawer({ open, onClose, onCheckout }) {
  const { cart, cartCount, cartTotal, updateQuantity, removeFromCart, clearCart } = useCart();

  return (
    <Drawer anchor="right" open={open} onClose={onClose}>
      <Box sx={{ width: 380, display: 'flex', flexDirection: 'column', height: '100%' }}>
        <Box sx={{ display: 'flex', alignItems: 'center', p: 2 }}>
          <Typography variant="h6" sx={{ flexGrow: 1 }}>
            Your Cart
          </Typography>
          <Badge badgeContent={cartCount} color="primary" sx={{ mr: 2 }}>
            <Box />
          </Badge>
          <IconButton onClick={onClose}>
            <Close />
          </IconButton>
        </Box>
        <Divider />
        <Box sx={{ flexGrow: 1, overflow: 'auto', p: 2 }}>
          {cart.length === 0 ? (
            <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '100%' }}>
              <Typography color="text.secondary">Your cart is empty</Typography>
            </Box>
          ) : (
            cart.map((item) => (
              <Box key={item.product.id} sx={{ mb: 2 }}>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                  <Box>
                    <Typography variant="subtitle1" sx={{ fontWeight: 500 }}>
                      {item.product.name}
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                      SKU: {item.product.sku}
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                      ${Number(item.product.price).toFixed(2)} each
                    </Typography>
                  </Box>
                  <Box sx={{ textAlign: 'right' }}>
                    <IconButton size="small" onClick={() => removeFromCart(item.product.id)}>
                      <Delete fontSize="small" />
                    </IconButton>
                    <Typography variant="subtitle2" sx={{ fontWeight: 'bold' }}>
                      ${(item.quantity * item.product.price).toFixed(2)}
                    </Typography>
                  </Box>
                </Box>
                <Box sx={{ display: 'flex', alignItems: 'center', mt: 1 }}>
                  <IconButton
                    size="small"
                    onClick={() => updateQuantity(item.product.id, item.quantity - 1)}
                  >
                    <Remove fontSize="small" />
                  </IconButton>
                  <Typography sx={{ mx: 1 }}>{item.quantity}</Typography>
                  <IconButton
                    size="small"
                    onClick={() => updateQuantity(item.product.id, item.quantity + 1)}
                    disabled={item.quantity >= item.product.stock}
                  >
                    <Add fontSize="small" />
                  </IconButton>
                </Box>
                <Divider sx={{ mt: 2 }} />
              </Box>
            ))
          )}
        </Box>
        <Divider />
        <Box sx={{ p: 2 }}>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
            <Typography variant="h6" sx={{ fontWeight: 'bold' }}>Total</Typography>
            <Typography variant="h6" sx={{ fontWeight: 'bold' }}>
              ${cartTotal.toFixed(2)}
            </Typography>
          </Box>
          <Button
            variant="contained"
            fullWidth
            disabled={cart.length === 0}
            onClick={() => {
              onCheckout();
              onClose();
            }}
            sx={{ mb: 1 }}
          >
            CHECKOUT
          </Button>
          <Button
            fullWidth
            onClick={clearCart}
            disabled={cart.length === 0}
          >
            CLEAR CART
          </Button>
        </Box>
      </Box>
    </Drawer>
  );
}
