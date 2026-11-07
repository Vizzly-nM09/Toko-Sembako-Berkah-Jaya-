<?php
require __DIR__ . '/bootstrap.php';

require_method('GET');

$productStats = $pdo->query(
    'SELECT COUNT(*) AS total_products,
            COALESCE(SUM(stock), 0) AS total_stock,
            COALESCE(SUM(stock * price), 0) AS inventory_value,
            COALESCE(SUM(stock <= min_stock), 0) AS low_stock_count
     FROM products'
)->fetch();

$totalCategories = (int) $pdo->query('SELECT COUNT(*) FROM categories')->fetchColumn();
$totalSuppliers = (int) $pdo->query('SELECT COUNT(*) FROM suppliers')->fetchColumn();

json_response([
    'total_products' => (int) $productStats['total_products'],
    'total_stock' => (int) $productStats['total_stock'],
    'low_stock_count' => (int) $productStats['low_stock_count'],
    'total_categories' => $totalCategories,
    'total_suppliers' => $totalSuppliers,
    'inventory_value' => (float) $productStats['inventory_value'],
]);
