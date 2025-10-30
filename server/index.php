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

function formatValue(mixed $value): string
{
    if (is_bool($value)) {
        return $value ? 'true' : 'false';
    }
    if ($value === null) {
        return 'null';
    }
    if (is_scalar($value)) {
        return (string) $value;
    }
    return json_encode($value, JSON_UNESCAPED_SLASHES | JSON_UNESCAPED_UNICODE) ?: '';
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
                <td><?php echo htmlspecialchars(formatValue($value), ENT_QUOTES, 'UTF-8'); ?></td>
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
