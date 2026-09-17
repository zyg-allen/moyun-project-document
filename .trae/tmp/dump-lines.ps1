$c = Get-Content 'd:\zyg_new_work\moyun-project-document\.trae\tmp\clean-version-comments.ps1'
$i = 238
while ($i -lt 254) {
    Write-Host ("{0}: [{1}]" -f ($i + 1), $c[$i])
    $i++
}
Write-Host ('Total lines: ' + $c.Count)
