$frontend = [System.IO.File]::ReadAllText("$env:TEMP\frontend_all.txt")
$endpoints = Import-Csv "$env:TEMP\endpoints.csv"
$candidates = @()
$used = 0
foreach ($e in $endpoints) {
    $full = ($e.ClassPath + "/" + $e.MethodPath) -replace '//+', '/'
    if ($full -notmatch '^/') { $full = "/" + $full }
    $segs = $full.Split('/') | Where-Object { $_ -ne '' }
    $staticSegs = @()
    foreach ($s in $segs) {
        if ($s -match '^\{') { break }
        $staticSegs += $s
    }
    $staticFull = '/' + ($staticSegs -join '/')
    $matched = $false
    if ($staticFull.Length -gt 3 -and $frontend.Contains($staticFull)) { $matched = $true }
    if (-not $matched) {
        $clsSegs = ($e.ClassPath.Split('/') | Where-Object { $_ -ne '' })
        if ($clsSegs.Count -ge 2) {
            $clsTail = ($clsSegs | Select-Object -Last 2) -join '/'
            $alt = '/' + $clsTail + $staticFull
            if ($frontend.Contains($alt)) { $matched = $true }
        }
    }
    if (-not $matched) {
        $mSegs = ($e.MethodPath.Split('/') | Where-Object { $_ -ne '' })
        $mStatic = @()
        foreach ($s in $mSegs) {
            if ($s -match '^\{') { break }
            $mStatic += $s
        }
        if ($mStatic.Count -gt 0) {
            $mp = ($mStatic -join '/')
            if ($mp.Length -ge 6 -and $frontend.Contains($mp)) { $matched = $true }
        }
    }
    if ($matched) { $used++ } else { $candidates += $e }
}
Write-Host "Matched: $used / $($endpoints.Count), Candidates: $($candidates.Count)"
$candidates | Export-Csv "$env:TEMP\candidates.csv" -NoTypeInformation -Encoding UTF8
