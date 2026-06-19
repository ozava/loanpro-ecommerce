CREATE TABLE categories (
                            id BIGSERIAL PRIMARY KEY,
                            name VARCHAR(100) UNIQUE NOT NULL
);

CREATE TABLE products (
                          id BIGSERIAL PRIMARY KEY,
                          sku VARCHAR(50) UNIQUE NOT NULL,
                          name VARCHAR(200) NOT NULL,
                          description TEXT,
                          category_id BIGINT REFERENCES categories(id),
                          price NUMERIC(10,2) NOT NULL CHECK (price >= 0),
                          stock INTEGER NOT NULL DEFAULT 0 CHECK (stock >= 0),
                          weight_kg NUMERIC(8,3),
                          created_at TIMESTAMP DEFAULT NOW(),
                          updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE orders (
                        id BIGSERIAL PRIMARY KEY,
                        status VARCHAR(20) NOT NULL DEFAULT 'completed',
                        total_amount NUMERIC(12,2) NOT NULL,
                        created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE order_items (
                             id BIGSERIAL PRIMARY KEY,
                             order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
                             product_id BIGINT NOT NULL REFERENCES products(id),
                             quantity INTEGER NOT NULL CHECK (quantity > 0),
                             unit_price NUMERIC(10,2) NOT NULL,
                             subtotal NUMERIC(12,2) NOT NULL
);

CREATE INDEX idx_products_search ON products
    USING gin(to_tsvector('english', name || ' ' || COALESCE(description, '')));
CREATE INDEX idx_products_sku ON products(sku);
CREATE INDEX idx_products_category ON products(category_id);