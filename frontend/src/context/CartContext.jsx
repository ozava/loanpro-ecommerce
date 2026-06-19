import { createContext, useContext, useState, useEffect, useCallback, useMemo } from 'react';

const CartContext = createContext();

export function CartProvider({ children }) {
  const [cart, setCart] = useState(() => {
    const saved = localStorage.getItem('cart');
    return saved ? JSON.parse(saved) : [];
  });
  const [drawerOpen, setDrawerOpen] = useState(false);

  useEffect(() => {
    localStorage.setItem('cart', JSON.stringify(cart));
  }, [cart]);

  const addToCart = useCallback((product, quantity = 1) => {
    setCart((prev) => {
      const existing = prev.find((item) => item.product.id === product.id);
      if (existing) {
        const newQty = Math.min(existing.quantity + quantity, product.stock);
        if (newQty === existing.quantity) return prev;
        return prev.map((item) =>
          item.product.id === product.id
            ? { ...item, quantity: newQty }
            : item
        );
      }
      if (product.stock <= 0) return prev;
      return [...prev, { product, quantity: Math.min(quantity, product.stock) }];
    });
  }, []);

  const removeFromCart = useCallback((productId) => {
    setCart((prev) => prev.filter((item) => item.product.id !== productId));
  }, []);

  const updateQuantity = useCallback((productId, quantity) => {
    setCart((prev) => {
      if (quantity <= 0) return prev.filter((item) => item.product.id !== productId);
      return prev.map((item) => {
        if (item.product.id !== productId) return item;
        const capped = Math.min(quantity, item.product.stock);
        return { ...item, quantity: capped };
      });
    });
  }, []);

  const clearCart = useCallback(() => {
    setCart([]);
    localStorage.removeItem('cart');
  }, []);

  const cartCount = useMemo(
    () => cart.reduce((sum, item) => sum + item.quantity, 0),
    [cart]
  );

  const cartTotal = useMemo(
    () => Number(cart.reduce((sum, item) => sum + item.quantity * item.product.price, 0).toFixed(2)),
    [cart]
  );

  const value = useMemo(() => ({
    cart,
    addToCart,
    removeFromCart,
    updateQuantity,
    clearCart,
    cartCount,
    cartTotal,
    drawerOpen,
    setDrawerOpen
  }), [cart, addToCart, removeFromCart, updateQuantity, clearCart, cartCount, cartTotal, drawerOpen]);

  return (
    <CartContext.Provider value={value}>
      {children}
    </CartContext.Provider>
  );
}

export function useCart() {
  const context = useContext(CartContext);
  if (!context) throw new Error('useCart must be used within a CartProvider');
  return context;
}
