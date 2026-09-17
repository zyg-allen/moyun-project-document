$ErrorActionPreference = 'Stop'
$root = 'd:\zyg_new_work\moyun-project-document\moyun-server\src\main\java\com\moyun\ext'
$files = Get-ChildItem -Path "$root\ai", "$root\ai2" -Recurse -Filter *.java

# 版本 token（带小数点，如 v11.57 / V10.0 / v1.1.2），排除书名号后的文档版本（如《…》V2.0）
$verTok = '(?<!》)[vV]\d+(?:\.\d+)+[a-zA-Z0-9.\-]*(?:\s*任务\s*\d+(?:\.\d+)?)?[：:\s]*'
# 任务编号 token（如 P0-3c：），排除书名号后的文档条目引用（如《…》P1-3：）
$pTok = '(?<!》)[Pp]\d+-\d+[a-z]?[：:\s]*'

# 判断括号内容是否"仅含版本沿革说明"（以版本号开头、剔除 token 后剩余极短且非纯方案代号）
function Test-Paren([string]$inner) {
    if ($inner -notmatch '^\s*[vV]\d') { return $false }
    $rest = $inner -replace '[vV]\d+(?:\.\d+)+[a-zA-Z0-9.\-]*', ''
    $rest = $rest -replace '[Pp]\d+-\d+[a-z]?', ''
    $rest = $rest -replace '任务\s*\d+(?:\.\d+)?', ''
    $rest = $rest -replace '[\s：:，、；;．.()（）\-—·×+／/]', ''
    if ($rest -match '^[Vv]\d+$') { return $false }  # 纯方案代号（如 V4）保留
    return ($rest.Length -le 6)
}

$script:changedFiles = 0
$script:changedLines = 0
$script:deletedLines = 0
$script:report = New-Object System.Collections.Generic.List[string]

foreach ($f in $files) {
    $path = $f.FullName
    $text = [System.IO.File]::ReadAllText($path)
    if ($text -notmatch '[vV]\d+\.\d' -and $text -notmatch '[Pp]\d+-\d+') { continue }

    # 按行切分并保留每行原有换行符（兼容 CRLF/LF/混合）
    $parts = [regex]::Split($text, '(\r\n|\n)')
    $lineChanged = 0
    $lineDeleted = 0

    for ($i = 0; $i -lt $parts.Length; $i += 2) {
        $line = $parts[$i]
        if ($line -eq '') { continue }
        $trimmed = $line.TrimStart()
        # 仅处理注释行：// 、* 、/* 开头
        if ($trimmed -notmatch '^(//|\*|/\*)') { continue }

        $orig = $line

        # 1. 整体删除"纯版本沿革"括号（如（v11.38）（v11.58 P0-3c 收口）（v11.53 任务边界））
        $toRemove = @()
        foreach ($m in [regex]::Matches($line, '（[^（）]*）')) {
            if (Test-Paren $m.Groups[1].Value) { $toRemove += $m.Value }
        }
        $new = $line
        foreach ($p in $toRemove) { $new = $new.Replace($p, '') }

        # 2. 删除版本 token（尾随冒号/空白一并吞掉）
        $new = [regex]::Replace($new, $verTok, '')
        # 3. 删除 P 任务编号 token
        $new = [regex]::Replace($new, $pTok, '')

        # 4. 清理残留
        $new = [regex]::Replace($new, '（\s*）', '')                       # 空括号
        $new = [regex]::Replace($new, '[，、；;]\s*）', '）')              # 括号内尾部遗留标点
        $new = [regex]::Replace($new, '（\s*[：:]+', '（')                 # 括号内开头遗留冒号
        $new = [regex]::Replace($new, '（\s*修复[：:]\s*', '（')           # （v11.49.1 修复：…）→（…）
        $new = [regex]::Replace($new, '(@deprecated\s*)废弃[：:]\s*', '$1') # @deprecated v11.95 废弃：→ @deprecated
        $new = [regex]::Replace($new, '^(\s*(?://|/\*{1,2}|\*+)\s*)[：:]\s*', '$1')  # 注释头后遗留冒号

        if ($new -ceq $orig) { continue }

        # 5. 变成空注释行则整行删除（仅限因本次替换变空的行）
        if ($new -match '^\s*//\s*$' -or $new -match '^\s*\*+\s*$') {
            $parts[$i] = $null
            if (($i + 1) -lt $parts.Length) { $parts[$i + 1] = $null }
            $lineDeleted++
        } else {
            $new = [regex]::Replace($new, '[ \t]+$', '')   # 行尾空白
            $parts[$i] = $new
            $lineChanged++
        }
    }

    if ($lineChanged -gt 0 -or $lineDeleted -gt 0) {
        $out = ($parts | Where-Object { $null -ne $_ }) -join ''
        [System.IO.File]::WriteAllText($path, $out, (New-Object System.Text.UTF8Encoding($false)))
        $script:changedFiles++
        $script:changedLines += $lineChanged
        $script:deletedLines += $lineDeleted
        $script:report.Add(("{0}: mod={1} del={2}" -f $f.Name, $lineChanged, $lineDeleted))
    }
}

Write-Output ("files={0} modifiedLines={1} deletedLines={2}" -f $script:changedFiles, $script:changedLines, $script:deletedLines)
$script:report | ForEach-Object { Write-Output $_ }
