import { AppBar, Toolbar, Typography, Button, Box, Container } from '@mui/material';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';

export default function Layout() {
  const navigate = useNavigate();
  const location = useLocation();

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
          <Button color="inherit" disabled>
            Store
          </Button>
        </Toolbar>
      </AppBar>
      <Container maxWidth="lg" sx={{ mt: 3, mb: 3, flexGrow: 1 }}>
        <Outlet />
      </Container>
    </Box>
  );
}
