import { AppBar, Toolbar, Typography, Button, Box, Container, IconButton, Badge } from '@mui/material';
import { ShoppingCart } from '@mui/icons-material';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import { useCart } from '../context/CartContext';
import CartDrawer from './cart/CartDrawer';

export default function Layout() {
  const navigate = useNavigate();
  const location = useLocation();
  const { cartCount, drawerOpen, setDrawerOpen } = useCart();

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', minHeight: '100vh' }}>
      <AppBar position="static">
        <Toolbar>
          <Typography variant="h6" sx={{ flexGrow: 1 }}>
            LoanPro E-Commerce
          </Typography>
          <Button
            color="inherit"
            onClick={() => navigate('/products')}
            sx={{
              borderBottom: location.pathname === '/products' ? '2px solid white' : 'none'
            }}
          >
            Products
          </Button>
          <Button
            color="inherit"
            onClick={() => navigate('/store')}
            sx={{
              borderBottom: location.pathname === '/store' ? '2px solid white' : 'none'
            }}
          >
            Store
          </Button>
          <IconButton color="inherit" onClick={() => setDrawerOpen(true)} sx={{ ml: 1 }}>
            <Badge badgeContent={cartCount} color="error">
              <ShoppingCart />
            </Badge>
          </IconButton>
        </Toolbar>
      </AppBar>
      <Container maxWidth="lg" sx={{ mt: 3, mb: 3, flexGrow: 1 }}>
        <Outlet />
      </Container>
      <CartDrawer
        open={drawerOpen}
        onClose={() => setDrawerOpen(false)}
        onCheckout={() => navigate('/checkout')}
      />
    </Box>
  );
}
