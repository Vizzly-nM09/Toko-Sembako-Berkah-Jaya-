<?php
declare(strict_types=1);

ini_set('display_errors', '0');
error_reporting(E_ALL);

$configPath = __DIR__ . '/config.php';
if (!is_file($configPath)) {
    http_response_code(500);
    header('Content-Type: application/json; charset=utf-8');
    echo json_encode(['error' => 'config.php belum dibuat. Salin config.example.php menjadi config.php.']);
    exit;
}

$config = require $configPath;

header('Content-Type: application/json; charset=utf-8');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

if (($_SERVER['REQUEST_METHOD'] ?? 'GET') === 'OPTIONS') {
    http_response_code(204);
    exit;
}

function json_response(array|object $data, int $status = 200): never
{
    http_response_code($status);
    echo json_encode(
        $data,
        JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES | JSON_PRESERVE_ZERO_FRACTION
    );
    exit;
}

function read_json_body(): array
{
    $raw = file_get_contents('php://input');
    if ($raw === false || trim($raw) === '') {
        json_response(['error' => 'Body JSON wajib diisi'], 400);
    }

    $data = json_decode($raw, true);
    if (!is_array($data) || json_last_error() !== JSON_ERROR_NONE) {
        json_response(['error' => 'Body JSON tidak valid'], 400);
    }

    return $data;
}

function require_method(string ...$methods): void
{
    $current = $_SERVER['REQUEST_METHOD'] ?? 'GET';
    if (!in_array($current, $methods, true)) {
        header('Allow: ' . implode(', ', $methods));
        json_response(['error' => 'Method tidak diizinkan'], 405);
    }
}

function query_int(string $name, ?int $default = null): ?int
{
    if (!isset($_GET[$name]) || $_GET[$name] === '') {
        return $default;
    }

    if (!is_scalar($_GET[$name])) {
        json_response(['error' => "Parameter $name harus berupa bilangan bulat"], 422);
    }

    $value = filter_var($_GET[$name], FILTER_VALIDATE_INT);
    if ($value === false) {
        json_response(['error' => "Parameter $name harus berupa bilangan bulat"], 422);
    }

    return (int) $value;
}

function require_id(): int
{
    $id = query_int('id');
    if ($id === null || $id < 1) {
        json_response(['error' => 'Parameter id wajib diisi'], 422);
    }

    return $id;
}

function record_exists(PDO $pdo, string $table, int $id): bool
{
    $allowed = ['categories', 'suppliers'];
    if (!in_array($table, $allowed, true)) {
        throw new InvalidArgumentException('Tabel tidak diizinkan');
    }

    $stmt = $pdo->prepare("SELECT 1 FROM $table WHERE id = ?");
    $stmt->execute([$id]);

    return (bool) $stmt->fetchColumn();
}

function payload_string(array $data, string $key): string
{
    if (!array_key_exists($key, $data) || !is_scalar($data[$key])) {
        json_response(['error' => "$key wajib diisi"], 422);
    }

    $value = trim((string) $data[$key]);
    if ($value === '') {
        json_response(['error' => "$key wajib diisi"], 422);
    }

    return $value;
}

function payload_int(array $data, string $key, int $default = 0): int
{
    if (!array_key_exists($key, $data) || $data[$key] === null || $data[$key] === '') {
        return $default;
    }

    $value = filter_var($data[$key], FILTER_VALIDATE_INT);
    if ($value === false) {
        json_response(['error' => "$key harus berupa bilangan bulat"], 422);
    }

    return (int) $value;
}

function payload_float(array $data, string $key, float $default = 0.0): float
{
    if (!array_key_exists($key, $data) || $data[$key] === null || $data[$key] === '') {
        return $default;
    }

    if (!is_numeric($data[$key])) {
        json_response(['error' => "$key harus berupa angka"], 422);
    }

    return (float) $data[$key];
}

function payload_nullable_int(array $data, string $key): ?int
{
    if (!array_key_exists($key, $data) || $data[$key] === null || $data[$key] === '') {
        return null;
    }

    $value = filter_var($data[$key], FILTER_VALIDATE_INT);
    if ($value === false) {
        json_response(['error' => "$key harus berupa bilangan bulat"], 422);
    }

    return (int) $value;
}

function payload_nullable_string(array $data, string $key, ?string $default = null): ?string
{
    if (!array_key_exists($key, $data)) {
        return $default;
    }

    if ($data[$key] === null || $data[$key] === '') {
        return null;
    }

    if (!is_scalar($data[$key])) {
        json_response(['error' => "$key harus berupa teks"], 422);
    }

    $value = trim((string) $data[$key]);

    return $value === '' ? null : $value;
}

set_exception_handler(function (Throwable $e) use ($config): void {
    error_log((string) $e);
    $message = !empty($config['debug']) ? $e->getMessage() : 'Terjadi kesalahan pada server';
    json_response(['error' => $message], 500);
});

$db = $config['db'];
$dsn = sprintf(
    'mysql:host=%s;port=%s;dbname=%s;charset=utf8mb4',
    $db['host'],
    $db['port'],
    $db['name']
);

$pdo = new PDO($dsn, $db['user'], $db['pass'], [
    PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
    PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
    PDO::ATTR_EMULATE_PREPARES => false,
    PDO::ATTR_STRINGIFY_FETCHES => false,
]);
