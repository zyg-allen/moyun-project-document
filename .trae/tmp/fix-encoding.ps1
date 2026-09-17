$p = 'd:\zyg_new_work\moyun-project-document\.trae\tmp\clean-version-comments.ps1'
$content = [System.IO.File]::ReadAllText($p, [System.Text.Encoding]::UTF8)
$utf8Bom = New-Object System.Text.UTF8Encoding($true)
[System.IO.File]::WriteAllText($p, $content, $utf8Bom)
Write-Host 'Converted to UTF-8 with BOM'

$tokens = $null
$err = $null
[System.Management.Automation.Language.Parser]::ParseFile($p, [ref]$tokens, [ref]$err) | Out-Null
if ($err.Count -eq 0) { Write-Host 'SYNTAX OK' }
else { $err | ForEach-Object { Write-Host ($_.Extent.StartLineNumber.ToString() + ':' + $_.Message) } }
