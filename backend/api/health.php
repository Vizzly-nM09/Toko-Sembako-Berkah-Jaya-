<?php
require __DIR__ . '/bootstrap.php';

$pdo->query('SELECT 1');

json_response(['status' => 'ok', 'database' => 'connected']);
