<?php
require __DIR__ . '/bootstrap.php';

require_method('GET', 'POST', 'PUT', 'DELETE');

$method = $_SERVER['REQUEST_METHOD'];

function validate_product_values(
    PDO $pdo,
    string $name,
    ?int $categoryId,
    ?int $supplierId,
    int $stock,
    int $minStock,
    float $price,
    ?string $imageUrl
): void {
    if (strlen($name) > 150) {
        json_response(['error' => 'Nama produk maksimal 150 karakter'], 422);
    }
    if ($stock < 0) {
        json_response(['error' => 'Stok tidak boleh negatif'], 422);
    }
    if ($minStock < 0) {
        json_response(['error' => 'Stok minimum tidak boleh negatif'], 422);
    }
    if ($price < 0) {
        json_response(['error' => 'Harga tidak boleh negatif'], 422);
    }
    if ($imageUrl !== null && strlen($imageUrl) > 255) {
        json_response(['error' => 'URL gambar maksimal 255 karakter'], 422);
    }
    if ($categoryId !== null && !record_exists($pdo, 'categories', $categoryId)) {
        json_response(['error' => 'Kategori tidak ditemukan'], 422);
    }
    if ($supplierId !== null && !record_exists($pdo, 'suppliers', $supplierId)) {
        json_response(['error' => 'Supplier tidak ditemukan'], 422);
    }
}

function map_product(array $row): array
{
    return [
        'id' => (int) $row['id'],
        'name' => $row['name'],
        'category_id' => $row['category_id'] !== null ? (int) $row['category_id'] : null,
        'category_name' => $row['category_name'],
        'supplier_id' => $row['supplier_id'] !== null ? (int) $row['supplier_id'] : null,
        'supplier_name' => $row['supplier_name'],
        'stock' => (int) $row['stock'],
        'min_stock' => (int) $row['min_stock'],
        'price' => (float) $row['price'],
        'image_url' => $row['image_url'],
        'is_low_stock' => (int) $row['stock'] <= (int) $row['min_stock'],
    ];
}

if ($method === 'GET') {
    $where = [];
    $params = [];

    $searchParam = $_GET['search'] ?? '';
    $search = is_scalar($searchParam) ? trim((string) $searchParam) : '';
    if ($search !== '') {
        $where[] = 'p.name LIKE :search';
        $params[':search'] = '%' . $search . '%';
    }

    $categoryId = query_int('category_id');
    if ($categoryId !== null) {
        $where[] = 'p.category_id = :category_id';
        $params[':category_id'] = $categoryId;
    }

    $supplierId = query_int('supplier_id');
    if ($supplierId !== null) {
        $where[] = 'p.supplier_id = :supplier_id';
        $params[':supplier_id'] = $supplierId;
    }

    $stockMin = query_int('stock_min');
    if ($stockMin !== null) {
        $where[] = 'p.stock >= :stock_min';
        $params[':stock_min'] = $stockMin;
    }

    $stockMax = query_int('stock_max');
    if ($stockMax !== null) {
        $where[] = 'p.stock <= :stock_max';
        $params[':stock_max'] = $stockMax;
    }

    $lowStock = $_GET['low_stock'] ?? null;
    if (is_scalar($lowStock) && ($lowStock === '1' || $lowStock === 'true')) {
        $where[] = 'p.stock <= p.min_stock';
    }

    $sql = 'SELECT p.id, p.name, p.category_id, c.name AS category_name,
                   p.supplier_id, s.name AS supplier_name,
                   p.stock, p.min_stock, p.price, p.image_url
            FROM products p
            LEFT JOIN categories c ON c.id = p.category_id
            LEFT JOIN suppliers s ON s.id = p.supplier_id';

    if ($where !== []) {
        $sql .= ' WHERE ' . implode(' AND ', $where);
    }

    $sql .= ' ORDER BY p.name ASC';

    $stmt = $pdo->prepare($sql);
    $stmt->execute($params);

    json_response(array_map('map_product', $stmt->fetchAll()));
}

if ($method === 'POST') {
    $data = read_json_body();

    $name = payload_string($data, 'name');
    $categoryId = payload_nullable_int($data, 'category_id');
    $supplierId = payload_nullable_int($data, 'supplier_id');
    $stock = payload_int($data, 'stock', 0);
    $minStock = payload_int($data, 'min_stock', 5);
    $price = payload_float($data, 'price', 0.0);
    $imageUrl = payload_nullable_string($data, 'image_url');

    validate_product_values(
        $pdo,
        $name,
        $categoryId,
        $supplierId,
        $stock,
        $minStock,
        $price,
        $imageUrl
    );

    $stmt = $pdo->prepare(
        'INSERT INTO products (name, category_id, supplier_id, stock, min_stock, price, image_url)
         VALUES (?, ?, ?, ?, ?, ?, ?)'
    );
    $stmt->execute([$name, $categoryId, $supplierId, $stock, $minStock, $price, $imageUrl]);

    json_response([
        'message' => 'Produk berhasil ditambahkan',
        'id' => (int) $pdo->lastInsertId(),
    ], 201);
}

$id = require_id();

if ($method === 'PUT') {
    $stmt = $pdo->prepare(
        'SELECT id, name, category_id, supplier_id, stock, min_stock, price, image_url
         FROM products WHERE id = ?'
    );
    $stmt->execute([$id]);
    $existing = $stmt->fetch();

    if (!$existing) {
        json_response(['error' => 'Produk tidak ditemukan'], 404);
    }

    $data = read_json_body();

    $name = array_key_exists('name', $data)
        ? payload_string($data, 'name')
        : $existing['name'];
    $categoryId = array_key_exists('category_id', $data)
        ? payload_nullable_int($data, 'category_id')
        : ($existing['category_id'] !== null ? (int) $existing['category_id'] : null);
    $supplierId = array_key_exists('supplier_id', $data)
        ? payload_nullable_int($data, 'supplier_id')
        : ($existing['supplier_id'] !== null ? (int) $existing['supplier_id'] : null);
    $stock = array_key_exists('stock', $data)
        ? payload_int($data, 'stock', 0)
        : (int) $existing['stock'];
    $minStock = array_key_exists('min_stock', $data)
        ? payload_int($data, 'min_stock', 5)
        : (int) $existing['min_stock'];
    $price = array_key_exists('price', $data)
        ? payload_float($data, 'price', 0.0)
        : (float) $existing['price'];
    $imageUrl = array_key_exists('image_url', $data)
        ? payload_nullable_string($data, 'image_url')
        : $existing['image_url'];

    validate_product_values(
        $pdo,
        $name,
        $categoryId,
        $supplierId,
        $stock,
        $minStock,
        $price,
        $imageUrl
    );

    $stmt = $pdo->prepare(
        'UPDATE products
         SET name = ?, category_id = ?, supplier_id = ?, stock = ?, min_stock = ?, price = ?, image_url = ?
         WHERE id = ?'
    );
    $stmt->execute([
        $name,
        $categoryId,
        $supplierId,
        $stock,
        $minStock,
        $price,
        $imageUrl,
        $id,
    ]);

    json_response(['message' => 'Produk berhasil diperbarui']);
}

$stmt = $pdo->prepare('DELETE FROM products WHERE id = ?');
$stmt->execute([$id]);

if ($stmt->rowCount() === 0) {
    json_response(['error' => 'Produk tidak ditemukan'], 404);
}

json_response(['message' => 'Produk berhasil dihapus']);
