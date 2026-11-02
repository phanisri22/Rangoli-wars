# Builds the services and starts each one in its own window (H2 databases, no Docker needed).
# Then run the frontend:  cd frontend; npm install; npm run dev  ->  http://localhost:5173
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

& .\gradlew.bat assemble -x test
if ($LASTEXITCODE -ne 0) { throw 'Build failed' }

$services = @(
    @{ Name = 'auth-service';        File = 'auth-service\build\libs\app.jar' },
    @{ Name = 'game-service';        File = 'game-service\build\libs\app.jar' },
    @{ Name = 'leaderboard-service'; File = 'leaderboard-service\build\libs\app.war' },
    @{ Name = 'gateway';             File = 'gateway\build\libs\app.jar' }
)

New-Item -ItemType Directory -Force (Join-Path $root 'data') | Out-Null
foreach ($s in $services) {
    $jar = Join-Path $root $s.File
    Start-Process powershell -WorkingDirectory (Join-Path $root 'data') -ArgumentList @(
        '-NoExit', '-Command', "`$host.UI.RawUI.WindowTitle = '$($s.Name)'; java -Xmx256m -jar '$jar'"
    )
}

Write-Host 'Started auth (8081), game (8082), leaderboard (8083) and gateway (8080).'
Write-Host 'Admin page: http://localhost:8080/admin  (admin / admin123 locally)'
