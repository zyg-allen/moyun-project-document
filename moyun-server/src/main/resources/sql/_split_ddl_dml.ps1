$ErrorActionPreference = 'Stop'

$base = Split-Path -Parent $MyInvocation.MyCommand.Path
$src    = Join-Path $base 'moyun-db-ddl-moyun-db-202608201435.sql'
$ddlOut = Join-Path $base 'moyun-db-ddl-202608201435.sql'
$dmlOut = Join-Path $base 'moyun-db-dml-202608201435.sql'

$raw = [System.IO.File]::ReadAllText($src, [System.Text.Encoding]::UTF8)
$lines = $raw -split "`r?`n"

function Get-Module([string]$name) {
    switch -Regex ($name) {
        '^ai_'     { return 'ai' }
        '^gen_'    { return 'gen' }
        '^portal_' { return 'portal' }
        '^qrtz_'   { return 'qrtz' }
        '^sys_'    { return 'sys' }
        default    { return 'other' }
    }
}

# --- collect header (before first DROP TABLE) ---
$header = New-Object System.Text.StringBuilder
$startIdx = 0
for ($i = 0; $i -lt $lines.Count; $i++) {
    if ($lines[$i] -match '^DROP TABLE IF EXISTS') { $startIdx = $i; break }
    [void]$header.Append($lines[$i])
    [void]$header.Append("`r`n")
}

# --- state machine ---
$names = New-Object System.Collections.Generic.List[string]
$ddl = @{}
$dml = @{}

function Ensure([string]$name) {
    if (-not $ddl.ContainsKey($name)) {
        [void]$names.Add($name)
        $ddl[$name] = New-Object System.Text.StringBuilder
        $dml[$name] = New-Object System.Text.StringBuilder
    }
}

$mode = 'none'   # none / ddl / dml
$cur  = $null

for ($i = $startIdx; $i -lt $lines.Count; $i++) {
    $L = $lines[$i]

    if ($L -match '^INSERT INTO `[^`]+`\.([^\s(]+)') {
        $cur = $Matches[1]; Ensure $cur; $mode = 'dml'
        [void]$dml[$cur].Append($L + "`r`n")
        continue
    }
    if ($L -match '^INSERT INTO `([^`]+)`') {
        $cur = $Matches[1]; Ensure $cur; $mode = 'dml'
        [void]$dml[$cur].Append($L + "`r`n")
        continue
    }
    if ($L -match '^DROP TABLE IF EXISTS `([^`]+)`') {
        $cur = $Matches[1]; Ensure $cur; $mode = 'ddl'
        [void]$ddl[$cur].Append($L + "`r`n")
        continue
    }
    if ($L -match '^CREATE TABLE `([^`]+)`') {
        $cur = $Matches[1]; Ensure $cur; $mode = 'ddl'
        [void]$ddl[$cur].Append($L + "`r`n")
        continue
    }
    if ($L -match '^LOCK TABLES `([^`]+)`') {
        $cur = $Matches[1]; Ensure $cur; $mode = 'dml'
        [void]$dml[$cur].Append($L + "`r`n")
        continue
    }
    if ($L -match '^\s*UNLOCK TABLES;') {
        if ($cur -ne $null -and $mode -eq 'dml') { [void]$dml[$cur].Append($L + "`r`n") }
        $mode = 'none'
        continue
    }
    if ($L -match '(?i)^\s*select\b') {   # stray junk (e.g. select * from ai_agent_tool / SELECT * FROM sys_user_role)
        $mode = 'none'
        continue
    }

    if ($mode -eq 'ddl') {
        [void]$ddl[$cur].Append($L + "`r`n")
    } elseif ($mode -eq 'dml') {
        [void]$dml[$cur].Append($L + "`r`n")
    }
    # else: comments/blank between blocks -> skip
}

$enc = New-Object System.Text.UTF8Encoding($false)

$moduleOrder = @('ai','gen','portal','qrtz','sys','other')
$moduleLabel = @{}
$moduleLabel['ai']     = 'AI Agent / Knowledge Base (ai_*)'
$moduleLabel['gen']    = 'Code Generator (gen_*)'
$moduleLabel['portal'] = 'Portal Community (portal_*)'
$moduleLabel['qrtz']   = 'Quartz Scheduler (qrtz_*)'
$moduleLabel['sys']    = 'System Administration (sys_*)'
$moduleLabel['other']  = 'Others'

# ---- DDL output ----
$ddlSb = New-Object System.Text.StringBuilder
[void]$ddlSb.Append($header.ToString())
[void]$ddlSb.Append("`r`n-- =============================================================`r`n")
[void]$ddlSb.Append("-- DDL (Data Definition Language): table structures, grouped by module`r`n")
[void]$ddlSb.Append("-- Source: moyun-db-ddl-moyun-db-202608201435.sql`r`n")
[void]$ddlSb.Append("-- For future schema changes, keep the original DDL below and append incremental ALTER TABLE statements to the matching module`r`n")
[void]$ddlSb.Append("-- =============================================================`r`n`r`n")

foreach ($mod in $moduleOrder) {
    $list = @($names | Where-Object { (Get-Module $_) -eq $mod })
    if ($list.Count -eq 0) { continue }
    [void]$ddlSb.Append("-- ------------------------------------------------------------`r`n")
    [void]$ddlSb.Append("-- Module: " + $moduleLabel[$mod] + "`r`n")
    [void]$ddlSb.Append("-- ------------------------------------------------------------`r`n`r`n")
    foreach ($n in $list) {
        [void]$ddlSb.Append($ddl[$n].ToString())
        [void]$ddlSb.Append("`r`n")
    }
}
[System.IO.File]::WriteAllText($ddlOut, $ddlSb.ToString(), $enc)

# ---- DML output ----
$dmlSb = New-Object System.Text.StringBuilder
[void]$dmlSb.Append($header.ToString())
[void]$dmlSb.Append("`r`n-- =============================================================`r`n")
[void]$dmlSb.Append("-- DML (Data Manipulation Language): table data, grouped by module`r`n")
[void]$dmlSb.Append("-- Only tables with actual data (INSERT) are included; empty tables are omitted`r`n")
[void]$dmlSb.Append("-- Idempotent: each table is cleared with DELETE FROM before re-inserting its data`r`n")
[void]$dmlSb.Append("-- Source: moyun-db-ddl-moyun-db-202608201435.sql`r`n")
[void]$dmlSb.Append("-- =============================================================`r`n`r`n")

foreach ($mod in $moduleOrder) {
    $list = @($names | Where-Object { (Get-Module $_) -eq $mod })
    $withData = @($list | Where-Object { $dml[$_].ToString() -match 'INSERT\s+INTO' })
    if ($withData.Count -eq 0) { continue }
    [void]$dmlSb.Append("-- ------------------------------------------------------------`r`n")
    [void]$dmlSb.Append("-- Module: " + $moduleLabel[$mod] + "`r`n")
    [void]$dmlSb.Append("-- ------------------------------------------------------------`r`n`r`n")
    foreach ($n in $withData) {
        [void]$dmlSb.Append('DELETE FROM `' + $n + '`;' + "`r`n")
        [void]$dmlSb.Append($dml[$n].ToString())
        [void]$dmlSb.Append("`r`n")
    }
}
[System.IO.File]::WriteAllText($dmlOut, $dmlSb.ToString(), $enc)

Write-Output ("total_tables=" + $names.Count)
Write-Output ("ddl_tables=" + (@($names | Where-Object { $ddl[$_].ToString() -match 'CREATE TABLE' }).Count))
Write-Output ("dml_tables=" + (@($names | Where-Object { $dml[$_].ToString() -match 'INSERT\s+INTO' }).Count))
Write-Output ("ddl_bytes=" + (Get-Item $ddlOut).Length)
Write-Output ("dml_bytes=" + (Get-Item $dmlOut).Length)