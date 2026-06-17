CREATE TEMPORARY TABLE staging_raw (
    name       TEXT,
    sku        TEXT,
    description TEXT,
    category   TEXT,
    price      TEXT,
    stock      TEXT,
    weight_kg  TEXT
);

COPY staging_raw
    FROM '/flyway/data/Initial_data.csv'
    DELIMITER ','
    CSV HEADER;

CREATE TEMPORARY TABLE staging_clean AS
SELECT DISTINCT ON (TRIM(sku))
    TRIM(name)        AS name,
    TRIM(sku)         AS sku,
    TRIM(description) AS description,
    TRIM(category)    AS category,
    REPLACE(TRIM(price), '$', '')::NUMERIC(10,2) AS price,
    TRIM(stock)::INTEGER                          AS stock,
    TRIM(weight_kg)::NUMERIC(10,2)                AS weight_kg
FROM staging_raw
WHERE TRUE
  AND TRIM(COALESCE(name, ''))        <> ''
  AND TRIM(COALESCE(sku, ''))         <> ''
  AND TRIM(COALESCE(description, '')) <> ''
  AND TRIM(COALESCE(category, ''))    <> ''
  AND TRIM(COALESCE(price, ''))       <> ''
  AND TRIM(COALESCE(stock, ''))       <> ''
  AND TRIM(COALESCE(weight_kg, ''))   <> ''

  AND REPLACE(TRIM(price), '$', '') ~ '^\d+(\.\d+)?$'

  AND TRIM(stock) ~ '^\d+$'

  AND TRIM(weight_kg) ~ '^\d+(\.\d+)?$'
ORDER BY TRIM(sku), TRIM(name);

INSERT INTO categories (name)
SELECT DISTINCT category FROM staging_clean
    ON CONFLICT (name) DO NOTHING;

INSERT INTO products (sku, name, description, category_id, price, stock, weight_kg)
SELECT
    sc.sku,
    sc.name,
    sc.description,
    c.id,
    sc.price,
    sc.stock,
    sc.weight_kg
FROM staging_clean sc
         JOIN categories c ON c.name = sc.category
    ON CONFLICT (sku) DO NOTHING;

DROP TABLE IF EXISTS staging_raw;
DROP TABLE IF EXISTS staging_clean;