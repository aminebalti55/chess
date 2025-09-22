# ======= MINIMAL SCAFFOLD (clean + create) + Level 3 helper =======
# Run from project root (folder containing src\main\java)

$javaRoot = "src/main/java"
$basePkg  = "com.example.chess"

function PkgPath($subpkg) {
  $full = if ($subpkg -eq "") { $basePkg } else { "$basePkg.$subpkg" }
  return (Join-Path $javaRoot ($full -replace '\.', '/'))
}
function NewPkgClass([string]$subpkg,[string]$name,[string]$kind='class',[string]$body=''){
  $dir = PkgPath $subpkg
  New-Item -ItemType Directory -Force -Path $dir | Out-Null
  $pkg = "package $basePkg" + ($(if($subpkg){".$subpkg"}else{""})) + ";"
  if(-not $body){
    if($kind -eq 'enum'){ $body = "public enum $name { }" }
    elseif($kind -eq 'interface'){ $body = "public interface $name { }" }
    else { $body = "public class $name { }" }
  }
  $content = "$pkg`r`n`r`n$body`r`n"
  Set-Content -Path (Join-Path $dir "$name.java") -Value $content -NoNewline
}

# 0) CLEAN up extras (safe if they don't exist)
$toRemove = @(
  "presence","invitation","game.ws","game.rest","game.repo","game.service","game.model",
  "common/dto","common.dto","auth/PasswordService","auth/UserDetailsServiceImpl",
  "config/ApplicationExceptionAdvice","common/GlobalExceptionHandler","common/ApiError",
  "user/UserService","invitation","presence","game/ws","game/rest"
)
foreach($p in $toRemove){
  $path1 = PkgPath $p
  if(Test-Path $path1){ Remove-Item -Recurse -Force $path1 -ErrorAction SilentlyContinue }
  if($p -match "/"){
    $pkg = Split-Path $p -Parent
    $cls = Split-Path $p -Leaf
    $file = Join-Path (PkgPath $pkg) ($cls + ".java")
    if(Test-Path $file){ Remove-Item -Force $file -ErrorAction SilentlyContinue }
  }
}

# 1) Minimal package set
New-Item -ItemType Directory -Force -Path (PkgPath "config") | Out-Null
New-Item -ItemType Directory -Force -Path (PkgPath "auth")   | Out-Null
New-Item -ItemType Directory -Force -Path (PkgPath "user")   | Out-Null
New-Item -ItemType Directory -Force -Path (PkgPath "game")   | Out-Null
New-Item -ItemType Directory -Force -Path (PkgPath "lobby")  | Out-Null
New-Item -ItemType Directory -Force -Path (PkgPath "common") | Out-Null

# 2) CONFIG
NewPkgClass "config" "WebSocketConfig"
NewPkgClass "config" "SecurityConfig"
NewPkgClass "config" "CorsConfig"
NewPkgClass "config" "JwtChannelInterceptor"

# 3) AUTH
NewPkgClass "auth" "AuthController"
NewPkgClass "auth" "AuthService"
NewPkgClass "auth" "JwtService"

# 4) USER
NewPkgClass "user" "User"
NewPkgClass "user" "UserRepository" "interface"

# 5) GAME (entities, repos, service, controller)
NewPkgClass "game" "Game"
NewPkgClass "game" "Move"
NewPkgClass "game" "GameStatus" "enum"
NewPkgClass "game" "GameRepository" "interface"
NewPkgClass "game" "MoveRepository" "interface"
NewPkgClass "game" "GameService"
NewPkgClass "game" "GameController"

# 5b) LEVEL 3 helper (basic server-side move checks)
NewPkgClass "game" "GameRules"

# 6) LOBBY (presence + invites)
NewPkgClass "lobby" "LobbyService"
NewPkgClass "lobby" "LobbyController"

# 7) COMMON — single DTO holder
$dtoBody = @"
public class Dto {

  // ===== AUTH =====
  public static class RegisterRequest { /* email, displayName, password */ }
  public static class LoginRequest { /* email, password */ }
  public static class AuthResponse { /* token, userId, displayName */ }
  public static class CurrentUserDto { /* id, email, displayName */ }

  // ===== LOBBY / PRESENCE / INVITES =====
  public static class OnlineUsersDto { /* List<UserLite> users */ }
  public static class InviteSend { /* toUserId */ }
  public static class InviteReply { /* invitationId, accept */ }
  public static class InviteNotification { /* invitationId, fromUser */ }
  public static class GameCreated { /* gameId, whitePlayerId, blackPlayerId */ }

  // ===== GAME / MOVES / REPLAY =====
  public static class MoveSend { /* from, to, promotion?, san? */ }
  public static class MoveBroadcast { /* moveNumber, from, to, san?, by, ts, promotion? */ }
  public static class MoveRecord { /* moveNumber, fromSquare, toSquare, san, promotion, playedByUserId, playedAt, fenAfter? */ }
  public static class ActiveGameDto { /* gameId, youAreWhite, status, lastFen? */ }
  public static class ReplaySliceRequest { /* optional: fromMove, limit */ }

  // minimal embedded type
  public static class UserLite { /* id, displayName */ }
}
"@
NewPkgClass "common" "Dto" "class" $dtoBody

Write-Host "✅ Minimal scaffold (with GameRules for Level 3) ready under $javaRoot for package $basePkg"
