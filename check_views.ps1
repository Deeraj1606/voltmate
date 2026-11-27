$lines = Get-Content "C:\Users\DRJ\.gemini\antigravity-ide\brain\0a2c729d-122a-4dcc-97ee-5287a17f6d33\.system_generated\logs\transcript_full.jsonl" -Encoding utf8

foreach ($l in $lines) {
    try {
        $j = ConvertFrom-Json $l -ErrorAction SilentlyContinue
        if ($j -and $j.tool_calls) {
            foreach ($tc in $j.tool_calls) {
                if ($tc.name -eq "write_to_file") {
                    if ($tc.args.TargetFile -like "*DriverDashboardScreen.kt*" -or $tc.args.TargetFile -like "*OwnerDashboardScreen.kt*") {
                        Write-Host "Step $($j.step_index): write_to_file $($tc.args.TargetFile) ($($tc.args.CodeContent.Length) chars, $(($tc.args.CodeContent -split '`n').Count) lines)"
                    }
                }
            }
        }
    } catch {}
}
