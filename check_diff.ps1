$lines = Get-Content "C:\Users\DRJ\.gemini\antigravity-ide\brain\0a2c729d-122a-4dcc-97ee-5287a17f6d33\.system_generated\logs\transcript_full.jsonl" -Encoding utf8
foreach ($l in $lines) {
    $j = ConvertFrom-Json $l -ErrorAction SilentlyContinue
    if ($j -and ($j.step_index -eq 1200 -or $j.step_index -eq 1212)) {
        Write-Host "=== STEP $($j.step_index) ==="
        Write-Host $j.content
    }
}
