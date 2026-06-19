import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { CartProvider } from './context/CartContext';
import Layout from './components/Layout';
import ProductsPage from './pages/ProductsPage';
import StorePage from './pages/StorePage';
import CheckoutPage from './pages/CheckoutPage';

function App() {
  return (
    <BrowserRouter>
      <CartProvider>
        <Routes>
          <Route path="/" element={<Layout />}>
            <Route index element={<Navigate to="/products" replace />} />
            <Route path="products" element={<ProductsPage />} />
            <Route path="store" element={<StorePage />} />
            <Route path="checkout" element={<CheckoutPage />} />
          </Route>
        </Routes>
      </CartProvider>
    </BrowserRouter>
  );
}

export default App;
