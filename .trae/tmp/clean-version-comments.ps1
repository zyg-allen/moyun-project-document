param(
    [switch]$Apply
)

$ErrorActionPreference = 'Stop'

$csSource = @'
using System;
using System.Collections.Generic;
using System.IO;
using System.Text;
using System.Text.RegularExpressions;

public static class CommentVersionCleaner
{
    // v11.31 / V11.0 / v1.1.2 等（含后随冒号/空白）
    private static readonly Regex VersionToken = new Regex(@"(?<![A-Za-z0-9])[vV]\d+(\.\d+)+[a-zA-Z0-9.\-]*[：:\s]*", RegexOptions.Compiled);
    // P1-5 / p0-3 等任务号
    private static readonly Regex TaskToken = new Regex(@"(?<![A-Za-z0-9])[Pp]\d+-\d+[a-z]?[：:\s]*", RegexOptions.Compiled);
    // 空全角括号（含可选前导空格，避免残留双空格）
    private static readonly Regex EmptyParen = new Regex(@" ?（\s*）", RegexOptions.Compiled);
    // 删除括号内 token 后残留的前导标点：（， / （；
    private static readonly Regex ParenLeftJunk = new Regex(@"（\s*[，；]", RegexOptions.Compiled);
    // 括号闭合前残留标点：，） / ；）
    private static readonly Regex ParenRightJunk = new Regex(@"[，；]\s*）", RegexOptions.Compiled);

    private static string CleanCommentText(string s)
    {
        s = VersionToken.Replace(s, "");
        s = TaskToken.Replace(s, "");
        s = EmptyParen.Replace(s, "");
        s = ParenLeftJunk.Replace(s, "（");
        s = ParenRightJunk.Replace(s, "）");
        return s;
    }

    // 处理一行：返回 null 表示该行应删除
    private static string ProcessLine(string line, ref bool inBlock, ref bool inText)
    {
        StringBuilder sb = new StringBuilder(line.Length + 16);
        int i = 0, n = line.Length;
        bool modified = false;
        bool done = false;

        while (!done)
        {
            if (inBlock)
            {
                int end = line.IndexOf("*/", i, StringComparison.Ordinal);
                if (end < 0)
                {
                    string seg = line.Substring(i);
                    string cleaned = CleanCommentText(seg);
                    if (!string.Equals(cleaned, seg, StringComparison.Ordinal)) modified = true;
                    sb.Append(cleaned);
                    done = true;
                }
                else
                {
                    string seg = line.Substring(i, end + 2 - i);
                    string cleaned = CleanCommentText(seg);
                    if (!string.Equals(cleaned, seg, StringComparison.Ordinal)) modified = true;
                    sb.Append(cleaned);
                    i = end + 2;
                    inBlock = false;
                }
            }
            else if (inText)
            {
                int end = line.IndexOf("\"\"\"", i, StringComparison.Ordinal);
                if (end < 0)
                {
                    sb.Append(line.Substring(i));
                    done = true;
                }
                else
                {
                    sb.Append(line.Substring(i, end + 3 - i));
                    i = end + 3;
                    inText = false;
                }
            }
            else
            {
                // code 状态：找到下一个注释/字符串起始
                int j = i;
                while (j < n)
                {
                    char c = line[j];
                    if (c == '/' && j + 1 < n && (line[j + 1] == '/' || line[j + 1] == '*')) break;
                    if (c == '"') break;
                    if (c == '\'') break;
                    j++;
                }
                if (j >= n)
                {
                    sb.Append(line.Substring(i));
                    done = true;
                    continue;
                }
                sb.Append(line.Substring(i, j - i));
                char cj = line[j];
                if (cj == '/' && line[j + 1] == '/')
                {
                    string seg = line.Substring(j + 2);
                    string cleaned = CleanCommentText(seg);
                    if (!string.Equals(cleaned, seg, StringComparison.Ordinal)) modified = true;
                    sb.Append("//").Append(cleaned);
                    done = true;
                }
                else if (cj == '/' && line[j + 1] == '*')
                {
                    sb.Append("/*");
                    i = j + 2;
                    inBlock = true;
                }
                else if (cj == '"')
                {
                    if (j + 2 < n && line[j + 1] == '"' && line[j + 2] == '"')
                    {
                        sb.Append("\"\"\"");
                        i = j + 3;
                        inText = true;
                    }
                    else
                    {
                        int k = j + 1;
                        while (k < n)
                        {
                            if (line[k] == '\\') { k += 2; continue; }
                            if (line[k] == '"') break;
                            k++;
                        }
                        if (k >= n) { sb.Append(line.Substring(j)); done = true; }
                        else { sb.Append(line.Substring(j, k + 1 - j)); i = k + 1; }
                    }
                }
                else // '\''
                {
                    int k = j + 1;
                    while (k < n)
                    {
                        if (line[k] == '\\') { k += 2; continue; }
                        if (line[k] == '\'') break;
                        k++;
                    }
                    if (k >= n) { sb.Append(line.Substring(j)); done = true; }
                    else { sb.Append(line.Substring(j, k + 1 - j)); i = k + 1; }
                }
            }
        }

        if (!modified) return line;

        string result = sb.ToString();
        result = result.TrimEnd();

        string trimmedStart = result.TrimStart();
        // 替换后变成空的注释行 → 删除
        if (Regex.IsMatch(trimmedStart, @"^//\s*$")) return null;
        if (Regex.IsMatch(trimmedStart, @"^\*\s*$")) return null;
        if (Regex.IsMatch(trimmedStart, @"^/\*\s*\*/\s*$")) return null;
        // 行尾空注释（code + //）→ 去掉 //
        result = Regex.Replace(result, @"(\S)\s*//\s*$", "$1");
        // 注释以冒号开头残留 → 去掉冒号
        result = Regex.Replace(result, @"(//\s*)：", "$1");
        result = Regex.Replace(result, @"(\*\s*)：", "$1");
        return result;
    }

    // 返回 object[]{ newText, changedLineCount }
    public static object[] ProcessText(string text)
    {
        string[] lines = text.Split('\n');
        bool inBlock = false, inText = false;
        List<string> outLines = new List<string>(lines.Length);
        int changed = 0;

        foreach (string raw in lines)
        {
            string line = raw;
            bool hadCr = false;
            if (line.EndsWith("\r")) { line = line.Substring(0, line.Length - 1); hadCr = true; }

            string processed = ProcessLine(line, ref inBlock, ref inText);
            if (processed == null) { changed++; continue; }

            string newLine = processed + (hadCr ? "\r" : "");
            if (!string.Equals(newLine, raw, StringComparison.Ordinal)) changed++;
            outLines.Add(newLine);
        }

        return new object[] { string.Join("\n", outLines.ToArray()), changed };
    }
}
'@

Add-Type -TypeDefinition $csSource -Language CSharp

$root = "d:\zyg_new_work\moyun-project-document\moyun-server\src\main\java\com\moyun"
$dirs = @("portal", "ledger", "pay", "system", "core", "common", "util", "ext\file", "ext\job", "ext\generator", "ext\cms")

$allFiles = @()
foreach ($d in $dirs) {
    $p = Join-Path $root $d
    if (Test-Path $p) {
        $allFiles += Get-ChildItem -Path $p -Recurse -Filter *.java -File
    }
}
$allFiles = $allFiles | Sort-Object FullName -Unique

$totalScanned = 0
$totalModified = 0
$totalChangedLines = 0
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
$sampleDiffs = New-Object System.Collections.Generic.List[string]

foreach ($f in $allFiles) {
    $totalScanned++
    $text = [System.IO.File]::ReadAllText($f.FullName)
    $res = [CommentVersionCleaner]::ProcessText($text)
    $newText = $res[0]
    $changed = [int]$res[1]
    if ($changed -gt 0) {
        $totalModified++
        $totalChangedLines += $changed
        # 生成 diff 样例（前后不同的行）
        $oldLines = $text -split "`n"
        $newLines = $newText -split "`n"
        $oi = 0; $ni = 0
        $shown = 0
        while (($oi -lt $oldLines.Count) -and ($ni -lt $newLines.Count) -and ($shown -lt 3)) {
            $o = $oldLines[$oi].TrimEnd("`r")
            $nw = $newLines[$ni].TrimEnd("`r")
            if ($o -ceq $nw) { $oi++; $ni++ }
            else {
                $sampleDiffs.Add("$($f.FullName.Substring($root.Length + 1))")
                $sampleDiffs.Add("  - $o")
                $sampleDiffs.Add("  + $nw")
                # 推进：旧行可能被删除（新行无对应）或被修改
                if (($ni + 1 -lt $newLines.Count) -and ($oi + 1 -lt $oldLines.Count) -and ($oldLines[$oi + 1].TrimEnd("`r") -ceq $newLines[$ni + 1].TrimEnd("`r"))) {
                    # 修改行：只删了 token，行数对齐但简单起进都推进
                }
                $oi++; $ni++
                $shown++
            }
        }
        if ($Apply) {
            [System.IO.File]::WriteAllText($f.FullName, $newText, $utf8NoBom)
        }
    }
}

Write-Host "=== 统计 ==="
Write-Host "扫描文件数: $totalScanned"
Write-Host "修改文件数: $totalModified"
Write-Host "修改行数: $totalChangedLines"
Write-Host "=== 样例 diff（前 120 组） ==="
$sampleDiffs | Select-Object -First 360 | ForEach-Object { Write-Host $_ }
