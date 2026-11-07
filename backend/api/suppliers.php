<?php
require __DIR__ . '/bootstrap.php';

require_method('GET', 'POST', 'PUT', 'DELETE');

$method = $_SERVER['REQUEST_METHOD'];

if ($method === 'GET') {
    $stmt = $pdo->query(
        'SELECT s.id, s.name, s.phone, s.address, COUNT(p.id) AS product_count
         FROM suppliers s
         LEFT JOIN products p ON p.supplier_id = s.id
         GROUP BY s.id, s.name, s.phone, s.address
         ORDER BY s.name ASC'
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
    $phone = payload_nullable_string($data, 'phone');
    $address = payload_nullable_string($data, 'address');

    if (strlen($name) > 120) {
        json_response(['error' => 'Nama supplier maksimal 120 karakter'], 422);
    }
    if ($phone !== null && strlen($phone) > 30) {
        json_response(['error' => 'Nomor telepon maksimal 30 karakter'], 422);
    }
    if ($address !== null && strlen($address) > 255) {
        json_response(['error' => 'Alamat maksimal 255 karakter'], 422);
    }

    try {
        $stmt = $pdo->prepare(
            'INSERT INTO suppliers (name, phone, address) VALUES (?, ?, ?)'
        );
        $stmt->execute([$name, $phone, $address]);
    } catch (PDOException $e) {
        if ($e->getCode() === '23000') {
            json_response(['error' => 'Nama supplier sudah digunakan'], 409);
        }
        throw $e;
    }

    json_response([
        'message' => 'Supplier berhasil ditambahkan',
        'id' => (int) $pdo->lastInsertId(),
    ], 201);
}

$id = require_id();

if ($method === 'PUT') {
    $stmt = $pdo->prepare('SELECT id, name, phone, address FROM suppliers WHERE id = ?');
    $stmt->execute([$id]);
    $existing = $stmt->fetch();

    if (!$existing) {
        json_response(['error' => 'Supplier tidak ditemukan'], 404);
    }

    $data = read_json_body();
    $name = array_key_exists('name', $data) ? payload_string($data, 'name') : $existing['name'];
    $phone = array_key_exists('phone', $data)
        ? payload_nullable_string($data, 'phone')
        : $existing['phone'];
    $address = array_key_exists('address', $data)
        ? payload_nullable_string($data, 'address')
        : $existing['address'];

    if (strlen($name) > 120) {
        json_response(['error' => 'Nama supplier maksimal 120 karakter'], 422);
    }
    if ($phone !== null && strlen($phone) > 30) {
        json_response(['error' => 'Nomor telepon maksimal 30 karakter'], 422);
    }
    if ($address !== null && strlen($address) > 255) {
        json_response(['error' => 'Alamat maksimal 255 karakter'], 422);
    }

    try {
        $stmt = $pdo->prepare(
            'UPDATE suppliers SET name = ?, phone = ?, address = ? WHERE id = ?'
        );
        $stmt->execute([$name, $phone, $address, $id]);
    } catch (PDOException $e) {
        if ($e->getCode() === '23000') {
            json_response(['error' => 'Nama supplier sudah digunakan'], 409);
        }
        throw $e;
    }

    json_response(['message' => 'Supplier berhasil diperbarui']);
}

$stmt = $pdo->prepare('DELETE FROM suppliers WHERE id = ?');
$stmt->execute([$id]);

if ($stmt->rowCount() === 0) {
    json_response(['error' => 'Supplier tidak ditemukan'], 404);
}

json_response(['message' => 'Supplier berhasil dihapus']);
