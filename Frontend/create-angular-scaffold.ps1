# create-angular-scaffold.ps1
# Usage (from chess-client root):  powershell -ExecutionPolicy Bypass -File .\create-angular-scaffold.ps1

$ErrorActionPreference = 'Stop'

function Ensure-InAngularRoot {
  if (-not (Test-Path ".\angular.json")) {
    Write-Error "angular.json not found. Run this script from your Angular project root (e.g., chess-client)."
  }
}

function Run([string]$cmd) {
  Write-Host "→ $cmd" -ForegroundColor Cyan
  iex $cmd
}

Ensure-InAngularRoot

# Core module + services/guards/interceptor
Run "ng g m core"
Run "ng g s core/auth/auth"
Run "ng g g core/auth/auth --flat=false"
Run "ng g interceptor core/auth/token"
Run "ng g s core/ws/stomp"
Run "ng g m shared"

# Shared presentational
Run "ng g c shared/navbar --export"

# Features – auth
Run "ng g m features/auth --route auth --module app.module"
Run "ng g c features/auth/login"
Run "ng g c features/auth/register"

# Features – lobby
Run "ng g m features/lobby --route lobby --module app.module"
Run "ng g c features/lobby/lobby"
Run "ng g c features/lobby/invite-dialog"

# Features – game
Run "ng g m features/game --route game --module app.module"
Run "ng g c features/game/game"
Run "ng g c features/game/board"
Run "ng g c features/game/moves-panel"
Run "ng g s features/game/game"
Run "ng g s features/lobby/lobby"

Write-Host ""
Write-Host "✅ Angular scaffold created. Next steps:" -ForegroundColor Green
Write-Host "1) Set environment.apiUrl and environment.wsUrl."
Write-Host "2) Wire TokenInterceptor in CoreModule (HTTP_INTERCEPTORS)."
Write-Host "3) Implement StompService connect/subscribe/send."
Write-Host "4) Build Login/Register, Lobby, and Game flows."
