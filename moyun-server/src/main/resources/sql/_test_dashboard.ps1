$body = @{ username = 'admin'; password = 'admin123' } | ConvertTo-Json
try {
    $response = Invoke-RestMethod -Uri 'http://localhost:8080/login' -Method Post -ContentType 'application/json' -Body $body
    $token = $response.token
    Write-Host "Admin login success, token length: $($token.Length)"
    
    $headers = @{ Authorization = "Bearer $token" }
    
    Write-Host ""
    Write-Host "===== 1. 后台首页待办列表 ====="
    $dashboard = Invoke-RestMethod -Uri 'http://localhost:8080/system/dashboard' -Method Get -Headers $headers
    Write-Host "Dashboard data:"
    $dashboard | ConvertTo-Json -Depth 8
    
} catch {
    Write-Host "Error: $($_.Exception.Message)"
    try {
        $reader = [System.IO.StreamReader]::new($_.Exception.Response.GetResponseStream())
        $errBody = $reader.ReadToEnd()
        Write-Host "Error Body: $errBody"
    } catch {}
}
