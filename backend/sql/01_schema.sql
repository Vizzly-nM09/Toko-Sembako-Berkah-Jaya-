USE db_toko;

CREATE TABLE IF NOT EXISTS categories (
  id INT UNSIGNED NOT NULL AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_categories_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS suppliers (
  id INT UNSIGNED NOT NULL AUTO_INCREMENT,
  name VARCHAR(120) NOT NULL,
  phone VARCHAR(30) NULL,
  address VARCHAR(255) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_suppliers_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS products (
  id INT UNSIGNED NOT NULL AUTO_INCREMENT,
  name VARCHAR(150) NOT NULL,
  category_id INT UNSIGNED NULL,
  supplier_id INT UNSIGNED NULL,
  stock INT NOT NULL DEFAULT 0,
  min_stock INT NOT NULL DEFAULT 5,
  price DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  image_url VARCHAR(255) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_products_name (name),
  KEY idx_products_stock (stock),
  KEY idx_products_category (category_id),
  KEY idx_products_supplier (supplier_id),
  CONSTRAINT chk_products_stock CHECK (stock >= 0),
  CONSTRAINT chk_products_min_stock CHECK (min_stock >= 0),
  CONSTRAINT chk_products_price CHECK (price >= 0),
  CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE SET NULL,
  CONSTRAINT fk_products_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO categories (name) VALUES
  ('Makanan'),
  ('Minuman'),
  ('Perlengkapan');

INSERT IGNORE INTO suppliers (name, phone, address) VALUES
  ('Supplier Utama', '0812-1111-2222', 'Jl. Merdeka No. 1'),
  ('Supplier Berkah', '0813-3333-4444', 'Jl. Pasar Baru No. 8');

SET @product_count = (SELECT COUNT(*) FROM products);

INSERT INTO products (name, category_id, supplier_id, stock, min_stock, price)
SELECT seed.name, seed.category_id, seed.supplier_id, seed.stock, seed.min_stock, seed.price
FROM (
  SELECT 'Indomie Goreng' AS name, c.id AS category_id, s.id AS supplier_id, 20 AS stock, 5 AS min_stock, 3500.00 AS price
  FROM categories c JOIN suppliers s ON c.name = 'Makanan' AND s.name = 'Supplier Utama'
  UNION ALL
  SELECT 'Air Mineral', c.id, s.id, 4, 5, 3000.00
  FROM categories c JOIN suppliers s ON c.name = 'Minuman' AND s.name = 'Supplier Berkah'
  UNION ALL
  SELECT 'Sabun Mandi', c.id, s.id, 12, 5, 5000.00
  FROM categories c JOIN suppliers s ON c.name = 'Perlengkapan' AND s.name = 'Supplier Utama'
) AS seed
WHERE @product_count = 0;
