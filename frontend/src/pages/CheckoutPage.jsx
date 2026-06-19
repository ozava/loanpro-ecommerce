import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useCart } from '../context/CartContext';
import CheckoutComponent from '../components/cart/CheckoutPage';

export default function CheckoutPage() {
  const { cart } = useCart();
  const navigate = useNavigate();

  useEffect(() => {
    if (cart.length === 0) {
      navigate('/store', { replace: true });
    }
  }, []);

  return <CheckoutComponent />;
}
