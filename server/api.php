<?php
declare(strict_types=1);

const API_KEY = 'asdfjdsl567567sadfsda';
const LOG_FILE = __DIR__ . '/server.log';
const DEVICES_DIR = __DIR__ . '/devices';

/**
 * Send a JSON response and terminate the request.
 */
function respond(int $statusCode, array $payload): void
{
    http_response_code($statusCode);
    header('Content-Type: application/json');
    echo json_encode($payload, JSON_UNESCAPED_SLASHES | JSON_UNESCAPED_UNICODE);
    exit;
}

/**
 * Write a structured event to the log file.
 */
function writeLog(string $level, array $context): bool
{
    $now = new DateTimeImmutable('now', new DateTimeZone('UTC'));
    $ip = $_SERVER['REMOTE_ADDR'] ?? 'unknown';
    $contextJson = json_encode($context, JSON_UNESCAPED_SLASHES | JSON_UNESCAPED_UNICODE);
    if ($contextJson === false) {
        $contextJson = json_encode(['event' => 'log_encode_failure'], JSON_UNESCAPED_SLASHES | JSON_UNESCAPED_UNICODE);
    }

    $logEntry = sprintf(
        '[%s] %s %s %s',
        $now->format(DateTimeInterface::ATOM),
        $ip,
        strtoupper($level),
        $contextJson
    );

    return file_put_contents(LOG_FILE, $logEntry . PHP_EOL, FILE_APPEND | LOCK_EX) !== false;
}

/**
 * Ensure the event is written to the log or halt the request.
 */
function ensureLog(string $level, array $context): void
{
    if (!writeLog($level, $context)) {
        respond(500, ['error' => 'Failed to write log entry.']);
    }
}

$method = $_SERVER['REQUEST_METHOD'] ?? 'UNKNOWN';
$rawBody = file_get_contents('php://input');
$maskedQuery = $_GET ?? [];
if (isset($maskedQuery['api-key'])) {
    $maskedQuery['api-key'] = '[REDACTED]';
}

ensureLog('info', [
    'event' => 'request_received',
    'method' => $method,
    'query' => $maskedQuery,
    'rawBody' => $rawBody === false ? null : $rawBody,
]);

if ($method !== 'POST') {
    ensureLog('error', [
        'event' => 'invalid_method',
        'method' => $method,
    ]);
    respond(405, ['error' => 'Method not allowed. Use POST.']);
}

$providedKey = $_GET['api-key'] ?? '';
if ($providedKey !== API_KEY) {
    ensureLog('error', [
        'event' => 'invalid_api_key',
        'provided' => $providedKey === '' ? null : '[PRESENT]',
    ]);
    respond(401, ['error' => 'Invalid API key.']);
}

if ($rawBody === false || trim($rawBody) === '') {
    ensureLog('error', [
        'event' => 'empty_body',
    ]);
    respond(400, ['error' => 'Empty request body.']);
}

$decoded = json_decode($rawBody, true);
if (!is_array($decoded)) {
    ensureLog('error', [
        'event' => 'invalid_json',
        'rawBody' => $rawBody,
        'jsonError' => json_last_error_msg(),
    ]);
    respond(400, ['error' => 'Invalid JSON payload.', 'details' => json_last_error_msg()]);
}

$deviceId = $decoded['deviceid'] ?? $decoded['deviceId'] ?? null;
if (!is_string($deviceId) || trim($deviceId) === '') {
    ensureLog('error', [
        'event' => 'missing_device_id',
        'payload' => $decoded,
    ]);
    respond(400, ['error' => 'Missing deviceid field in JSON payload.']);
}

$deviceId = trim($deviceId);
$safeDeviceId = preg_replace('/[^A-Za-z0-9_\-]/', '_', $deviceId);
if ($safeDeviceId === '') {
    ensureLog('error', [
        'event' => 'invalid_device_id',
        'deviceId' => $deviceId,
    ]);
    respond(400, ['error' => 'Device ID contains no allowed characters.']);
}

if (!is_dir(DEVICES_DIR) && !mkdir(DEVICES_DIR, 0775, true) && !is_dir(DEVICES_DIR)) {
    ensureLog('error', [
        'event' => 'devices_dir_failure',
        'path' => DEVICES_DIR,
    ]);
    respond(500, ['error' => 'Unable to prepare devices directory.']);
}

$now = new DateTimeImmutable('now', new DateTimeZone('UTC'));
$deviceFilePath = DEVICES_DIR . '/' . $safeDeviceId . '.json';

$formattedPayload = json_encode($decoded, JSON_UNESCAPED_SLASHES | JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);
if ($formattedPayload === false) {
    ensureLog('error', [
        'event' => 'payload_encode_failure',
        'payload' => $decoded,
    ]);
    respond(500, ['error' => 'Failed to encode device data.']);
}

if (file_put_contents($deviceFilePath, $formattedPayload . PHP_EOL, LOCK_EX) === false) {
    ensureLog('error', [
        'event' => 'device_write_failure',
        'deviceId' => $deviceId,
        'path' => $deviceFilePath,
    ]);
    respond(500, ['error' => 'Failed to persist device data.']);
}

ensureLog('info', [
    'event' => 'device_stored',
    'deviceId' => $deviceId,
    'payload' => $decoded,
    'file' => basename($deviceFilePath),
]);

respond(200, [
    'status' => 'ok',
    'deviceId' => $deviceId,
    'storedAt' => $now->format(DateTimeInterface::ATOM),
]);
