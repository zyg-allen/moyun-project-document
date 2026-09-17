$path = 'd:\zyg_new_work\moyun-project-document\.trae\tmp\clean-version-comments.ps1'
$tokens = $null
$err = $null
[System.Management.Automation.Language.Parser]::ParseFile($path, [ref]$tokens, [ref]$err) | Out-Null
if ($err.Count -eq 0) { Write-Host 'SYNTAX OK' }
else { $err | ForEach-Object { Write-Host ($_.Extent.StartLineNumber.ToString() + ':' + $_.Extent.StartColumnNumber.ToString() + ' ' + $_.Message + ' >>> ' + $_.Extent.Text) } }
