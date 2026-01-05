<?php
declare(strict_types=1);

$devicesDir = __DIR__ . '/devices';
$deviceRecords = [];
$columnSet = [];

if (is_dir($devicesDir)) {
    foreach (glob($devicesDir . '/*.json') as $deviceFile) {
        $contents = file_get_contents($deviceFile);
        if ($contents === false) {
            continue;
        }
        $data = json_decode($contents, true);
        if (!is_array($data)) {
            continue;
        }
        $deviceRecords[] = [
            'file' => basename($deviceFile),
            'data' => $data,
        ];
        foreach ($data as $key => $_) {
            $columnSet[$key] = true;
        }
    }
}

$columns = array_keys($columnSet);

usort($columns, static function (string $a, string $b): int {
    $priority = static function (string $key): int {
        $normalized = strtolower($key);
        return match ($normalized) {
            'deviceid', 'device_id' => 0,
            default => 1,
        };
    };
    $pa = $priority($a);
    $pb = $priority($b);
    if ($pa === $pb) {
        return strnatcasecmp($a, $b);
    }
    return $pa <=> $pb;
});

function formatValue(mixed $value): array
{
    if (is_array($value)) {
        return ['isHtml' => true, 'content' => renderJsonBlock($value)];
    }

    if (is_string($value)) {
        $decoded = decodeJsonStructure($value);
        if ($decoded !== null) {
            return ['isHtml' => true, 'content' => renderJsonBlock($decoded)];
        }
    }

    if (is_bool($value)) {
        return ['isHtml' => false, 'content' => $value ? 'true' : 'false'];
    }

    if ($value === null) {
        return ['isHtml' => false, 'content' => 'null'];
    }

    if (is_scalar($value)) {
        return ['isHtml' => false, 'content' => (string) $value];
    }

    $encoded = json_encode($value, JSON_UNESCAPED_SLASHES | JSON_UNESCAPED_UNICODE);

    if (is_string($encoded)) {
        return ['isHtml' => false, 'content' => $encoded];
    }

    return ['isHtml' => false, 'content' => ''];
}

function decodeJsonStructure(string $value): ?array
{
    $trimmed = trim($value);
    if ($trimmed === '' || ($trimmed[0] !== '{' && $trimmed[0] !== '[')) {
        return null;
    }

    $decoded = json_decode($value, true);

    if (json_last_error() !== JSON_ERROR_NONE || !is_array($decoded)) {
        return null;
    }

    return $decoded;
}

function renderJsonBlock(mixed $value): string
{
    return '<pre class="json-block">' . renderJsonMarkup($value, 0) . '</pre>';
}

function renderJsonMarkup(mixed $value, int $depth): string
{
    if (is_array($value)) {
        $isAssoc = array_keys($value) !== range(0, count($value) - 1);
        $indent = str_repeat('    ', $depth);
        $innerIndent = str_repeat('    ', $depth + 1);
        $open = $isAssoc ? '{' : '[';
        $close = $isAssoc ? '}' : ']';

        if ($value === []) {
            return '<span class="json-brace">' . $open . $close . '</span>';
        }

        $lines = [];
        $total = count($value);
        $index = 0;

        foreach ($value as $key => $item) {
            $linePrefix = $innerIndent;
            if ($isAssoc) {
                $linePrefix .= '<span class="json-key">"' . htmlspecialchars((string) $key, ENT_QUOTES, 'UTF-8') . '"</span>';
                $linePrefix .= '<span class="json-punct">: </span>';
            }

            $lineContent = renderJsonMarkup($item, $depth + 1);
            $index++;
            $comma = $index < $total ? '<span class="json-punct">,</span>' : '';
            $lines[] = $linePrefix . $lineContent . $comma;
        }

        return '<span class="json-brace">' . $open . '</span>'
            . "\n" . implode("\n", $lines) . "\n"
            . $indent . '<span class="json-brace">' . $close . '</span>';
    }

    if (is_string($value)) {
        return '<span class="json-string">"' . htmlspecialchars($value, ENT_QUOTES, 'UTF-8') . '"</span>';
    }

    if (is_bool($value)) {
        return '<span class="json-boolean">' . ($value ? 'true' : 'false') . '</span>';
    }

    if ($value === null) {
        return '<span class="json-null">null</span>';
    }

    if (is_numeric($value)) {
        return '<span class="json-number">' . htmlspecialchars((string) $value, ENT_QUOTES, 'UTF-8') . '</span>';
    }

    return '<span class="json-unknown">' . htmlspecialchars((string) $value, ENT_QUOTES, 'UTF-8') . '</span>';
}

?><!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Device Overview</title>
    <style>
        body {
            font-family: Arial, Helvetica, sans-serif;
            margin: 2rem;
            background-color: #f7f7f7;
        }
        h1 {
            margin-bottom: 0.5rem;
        }
        table {
            border-collapse: collapse;
            width: 100%;
            background: #fff;
        }
        th, td {
            border: 1px solid #ccc;
            padding: 0.5rem;
            text-align: left;
            vertical-align: top;
        }
        th {
            background: #efefef;
        }
        tr:nth-child(even) {
            background: #fafafa;
        }
        .controls {
            margin: 1rem 0;
        }
        .no-data {
            font-style: italic;
            color: #666;
        }
        .json-block {
            background: #1f2933;
            color: #e5e7eb;
            padding: 0.75rem;
            border-radius: 0.5rem;
            margin: 0;
            white-space: pre;
            font-family: "Fira Code", Consolas, "Liberation Mono", Menlo, Courier, monospace;
            font-size: 0.85rem;
            line-height: 1.4;
            overflow-x: auto;
        }
        .json-key {
            color: #38bdf8;
            font-weight: 600;
        }
        .json-string {
            color: #f472b6;
        }
        .json-number {
            color: #cbd5f5;
        }
        .json-boolean {
            color: #34d399;
            font-weight: 600;
        }
        .json-null {
            color: #f97316;
            font-style: italic;
        }
        .json-brace {
            color: #facc15;
            font-weight: 600;
        }
        .json-punct {
            color: #9ca3af;
        }
        .json-unknown {
            color: #e5e7eb;
        }
    </style>
</head>
<body>
    <h1>Devices dashboard</h1>
    <div class="controls">
        <button type="button" id="compareBtn">Compare selected</button>
        <button type="button" id="resetBtn">Show all</button>
    </div>
<?php if (empty($deviceRecords) || empty($columns)): ?>
    <p class="no-data">No device data available.</p>
<?php else: ?>
    <table id="devicesTable">
        <thead>
            <tr>
                <th>Select</th>
<?php foreach ($columns as $column): ?>
                <th><?php echo htmlspecialchars($column, ENT_QUOTES, 'UTF-8'); ?></th>
<?php endforeach; ?>
            </tr>
        </thead>
        <tbody>
<?php foreach ($deviceRecords as $index => $record): ?>
            <tr data-index="<?php echo $index; ?>">
                <td><input type="checkbox" class="row-select" aria-label="Select row <?php echo $index + 1; ?>"></td>
<?php foreach ($columns as $column): ?>
<?php $value = $record['data'][$column] ?? ''; ?>
<?php $formatted = formatValue($value); ?>
                <td>
<?php if ($formatted['isHtml']): ?>
<?php echo $formatted['content']; ?>
<?php else: ?>
<?php echo htmlspecialchars($formatted['content'], ENT_QUOTES, 'UTF-8'); ?>
<?php endif; ?>
                </td>
<?php endforeach; ?>
            </tr>
<?php endforeach; ?>
        </tbody>
    </table>
<?php endif; ?>

    <script>
        (function () {
            const table = document.getElementById('devicesTable');
            const compareBtn = document.getElementById('compareBtn');
            const resetBtn = document.getElementById('resetBtn');

            if (!table) {
                compareBtn.disabled = true;
                resetBtn.disabled = true;
                return;
            }

            const rows = Array.from(table.querySelectorAll('tbody tr'));

            compareBtn.addEventListener('click', () => {
                const selected = rows.filter(row => row.querySelector('.row-select').checked);
                if (selected.length === 0) {
                    alert('Select at least one device to compare.');
                    return;
                }
                rows.forEach(row => {
                    row.style.display = row.querySelector('.row-select').checked ? '' : 'none';
                });
            });

            resetBtn.addEventListener('click', () => {
                rows.forEach(row => {
                    row.style.display = '';
                    row.querySelector('.row-select').checked = false;
                });
            });
        }());
    </script>
</body>
</html>
