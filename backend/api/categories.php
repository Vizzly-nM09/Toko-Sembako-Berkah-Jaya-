<?php
require __DIR__ . '/bootstrap.php';

require_method('GET', 'POST', 'PUT', 'DELETE');

$method = $_SERVER['REQUEST_METHOD'];

if ($method === 'GET') {
    $stmt = $pdo->query(
        'SELECT c.id, c.name, COUNT(p.id) AS product_count
         FROM categories c
         LEFT JOIN products p ON p.category_id = c.id
         GROUP BY c.id, c.name
         ORDER BY c.name ASC'
    );

    $rows = $stmt->fetchAll();
    foreach ($rows as &$row) {
        $row['id'] = (int) $row['id'];
        $row['product_count'] = (int) $row['product_count'];
    }
    unset($row);

    json_response($rows);
}

if ($method === 'POST') {
    $data = read_json_body();
    $name = payload_string($data, 'name');

    if (strlen($name) > 100) {
        json_response(['error' => 'Nama kategori maksimal 100 karakter'], 422);
    }

    try {
        $stmt = $pdo->prepare('INSERT INTO categories (name) VALUES (?)');
        $stmt->execute([$name]);
    } catch (PDOException $e) {
        if ($e->getCode() === '23000') {
            json_response(['error' => 'Nama kategori sudah digunakan'], 409);
        }
        throw $e;
    }

    json_response([
        'message' => 'Kategori berhasil ditambahkan',
        'id' => (int) $pdo->lastInsertId(),
    ], 201);
}

$id = require_id();

if ($method === 'PUT') {
    if (!record_exists($pdo, 'categories', $id)) {
        json_response(['error' => 'Kategori tidak ditemukan'], 404);
    }

    $data = read_json_body();
    $name = payload_string($data, 'name');

    if (strlen($name) > 100) {
        json_response(['error' => 'Nama kategori maksimal 100 karakter'], 422);
    }

    try {
        $stmt = $pdo->prepare('UPDATE categories SET name = ? WHERE id = ?');
        $stmt->execute([$name, $id]);
    } catch (PDOException $e) {
        if ($e->getCode() === '23000') {
            json_response(['error' => 'Nama kategori sudah digunakan'], 409);
        }
        throw $e;
    }

    json_response(['message' => 'Kategori berhasil diperbarui']);
}

$stmt = $pdo->prepare('DELETE FROM categories WHERE id = ?');
$stmt->execute([$id]);

if ($stmt->rowCount() === 0) {
    json_response(['error' => 'Kategori tidak ditemukan'], 404);
}

json_response(['message' => 'Kategori berhasil dihapus']);
