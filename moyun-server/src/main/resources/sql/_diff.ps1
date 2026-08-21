$ErrorActionPreference = 'Stop'
$d = [System.IO.File]::ReadAllText('d:\zyg_new_work\moyun-project-document\moyun-server\src\main\resources\sql\moyun-db-dml-202608201435.sql', [System.Text.Encoding]::UTF8)
# capture unquoted table name after `moyun-db`.
$t1 = [regex]::Matches($d, '(?m)^INSERT INTO `[^`]+`\.([^\s(]+)') | ForEach-Object { $_.Groups[1].Value }
# capture quoted table name
$t2 = [regex]::Matches($d, '(?m)^INSERT INTO `([^`]+)` VALUES') | ForEach-Object { $_.Groups[1].Value }
$all = @($t1) + @($t2)
$uniq = $all | Sort-Object -Unique
Write-Output ('insert_total=' + $all.Count)
Write-Output ('unique_tables=' + $uniq.Count)
Write-Output '--- tables ---'
$uniq | ForEach-Object { Write-Output $_ }
Write-Output '--- lock count (raw) ---'
Write-Output ([regex]::Matches($d, '(?m)^LOCK TABLES').Count)
Write-Output '--- unlock count (raw) ---'
Write-Output ([regex]::Matches($d, '(?m)^\s*UNLOCK TABLES').Count)