ALTER TABLE orders
    ADD COLUMN payment_method VARCHAR(50) NOT NULL DEFAULT 'stripe';
