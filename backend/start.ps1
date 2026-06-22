# 读取项目根目录的 .env 文件
$envFile = Join-Path $PSScriptRoot ".." ".env"
if (Test-Path $envFile) {
    Get-Content $envFile | ForEach-Object {
        if ($_ -match "^\s*([^#][^=]+)=(.+)$") {
            $name = $matches[1].Trim()
            $value = $matches[2].Trim()
            [Environment]::SetEnvironmentVariable($name, $value, "Process")
            Write-Host "Set $name"
        }
    }
    Write-Host ""
}

# 启动后端
Write-Host "Starting backend..." -ForegroundColor Green
& "$PSScriptRoot\mvnw.cmd" spring-boot:run
