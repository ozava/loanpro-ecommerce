import { useState } from 'react';
import {
  Box, Typography, Button, Table, TableBody, TableCell, TableContainer,
  TableHead, TableRow, Paper, ToggleButtonGroup, ToggleButton, CircularProgress
} from '@mui/material';
import CheckCircleOutlined from '@mui/icons-material/CheckCircleOutlined';
import ErrorOutlined from '@mui/icons-material/ErrorOutlined';
import WarningAmber from '@mui/icons-material/WarningAmber';
import { useNavigate } from 'react-router-dom';
import { useCart } from '../../context/CartContext';
import { createOrder } from '../../api/apiClient';

export default function CheckoutPage() {
  const { cart, cartTotal, clearCart } = useCart();
  const navigate = useNavigate();
  const [paymentMethod, setPaymentMethod] = useState('STRIPE');
  const [loading, setLoading] = useState(false);
  const [orderResult, setOrderResult] = useState(null);
  const [errorState, setErrorState] = useState(null);

  const handlePlaceOrder = async () => {
    setLoading(true);
    setErrorState(null);
    try {
      const orderData = {
        items: cart.map((item) => ({
          productId: item.product.id,
          quantity: item.quantity
        })),
        paymentMethod
      };
      const response = await createOrder(orderData);
      setOrderResult(response.data);
    } catch (err) {
      const status = err.response?.status;
      if (status === 402) {
        setErrorState({
          type: 'failed',
          message: err.response?.data?.message || err.response?.data?.error || 'Payment was declined'
        });
      } else if (status === 503) {
        setErrorState({ type: 'unavailable' });
      } else {
        setErrorState({
          type: 'failed',
          message: err.response?.data?.message || err.response?.data?.error || 'An unexpected error occurred'
        });
      }
    } finally {
      setLoading(false);
    }
  };

  if (orderResult) {
    return (
      <Box sx={{ textAlign: 'center', py: 6 }}>
        <CheckCircleOutlined sx={{ fontSize: 80, color: 'success.main', mb: 2 }} />
        <Typography variant="h4" gutterBottom>Order Placed Successfully!</Typography>
        <Typography variant="h6" color="text.secondary" gutterBottom>
          Order ID: {orderResult.id}
        </Typography>
        <Typography variant="h6" color="text.secondary" gutterBottom>
          Total: ${Number(orderResult.totalAmount).toFixed(2)}
        </Typography>
        <Typography variant="body1" color="text.secondary" gutterBottom>
          Payment: {orderResult.paymentMethod || paymentMethod}
        </Typography>
        <Button
          variant="contained"
          size="large"
          sx={{ mt: 3 }}
          onClick={() => {
            clearCart();
            navigate('/store');
          }}
        >
          CONTINUE SHOPPING
        </Button>
      </Box>
    );
  }

  if (errorState?.type === 'unavailable') {
    return (
      <Box sx={{ textAlign: 'center', py: 6 }}>
        <WarningAmber sx={{ fontSize: 80, color: 'warning.main', mb: 2 }} />
        <Typography variant="h4" gutterBottom>Payment Service Unavailable</Typography>
        <Typography variant="body1" color="text.secondary" gutterBottom>
          The payment service is temporarily unavailable. Please try again later.
        </Typography>
        <Button
          variant="contained"
          size="large"
          sx={{ mt: 3 }}
          onClick={() => setErrorState(null)}
        >
          TRY AGAIN
        </Button>
        <Box sx={{ mt: 2 }}>
          <Button onClick={() => navigate('/store')}>BACK TO STORE</Button>
        </Box>
      </Box>
    );
  }

  if (errorState?.type === 'failed') {
    return (
      <Box sx={{ textAlign: 'center', py: 6 }}>
        <ErrorOutlined sx={{ fontSize: 80, color: 'error.main', mb: 2 }} />
        <Typography variant="h4" gutterBottom>Payment Failed</Typography>
        <Typography variant="body1" color="text.secondary" gutterBottom>
          {errorState.message}
        </Typography>
        <Button
          variant="contained"
          size="large"
          sx={{ mt: 3 }}
          onClick={() => setErrorState(null)}
        >
          TRY AGAIN
        </Button>
        <Box sx={{ mt: 2 }}>
          <Button onClick={() => navigate('/store')}>BACK TO STORE</Button>
        </Box>
      </Box>
    );
  }

  return (
    <Box sx={{ maxWidth: 800, mx: 'auto', py: 3 }}>
      <Typography variant="h4" gutterBottom>Checkout</Typography>
      <TableContainer component={Paper} sx={{ mb: 3 }}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Product</TableCell>
              <TableCell align="center">Quantity</TableCell>
              <TableCell align="right">Unit Price</TableCell>
              <TableCell align="right">Subtotal</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {cart.map((item) => (
              <TableRow key={item.product.id}>
                <TableCell>{item.product.name}</TableCell>
                <TableCell align="center">{item.quantity}</TableCell>
                <TableCell align="right">${Number(item.product.price).toFixed(2)}</TableCell>
                <TableCell align="right">${(item.quantity * item.product.price).toFixed(2)}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>
      <Typography variant="h5" sx={{ fontWeight: 'bold', mb: 3, textAlign: 'right' }}>
        Total: ${cartTotal.toFixed(2)}
      </Typography>
      <Typography variant="h6" gutterBottom>Payment Method</Typography>
      <ToggleButtonGroup
        value={paymentMethod}
        exclusive
        onChange={(e, val) => { if (val) setPaymentMethod(val); }}
        sx={{ mb: 3 }}
        fullWidth
      >
        <ToggleButton value="STRIPE">Stripe</ToggleButton>
        <ToggleButton value="PAYPAL">PayPal</ToggleButton>
        <ToggleButton value="MERCADO_PAGO">MercadoPago</ToggleButton>
      </ToggleButtonGroup>
      <Button
        variant="contained"
        size="large"
        fullWidth
        onClick={handlePlaceOrder}
        disabled={loading || cart.length === 0}
        sx={{ mb: 2 }}
      >
        {loading ? <CircularProgress size={24} color="inherit" /> : 'PLACE ORDER'}
      </Button>
      <Button fullWidth onClick={() => navigate('/store')}>
        BACK TO STORE
      </Button>
    </Box>
  );
}
