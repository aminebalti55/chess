# ♟️ Chess Multiplayer — Full-Stack (Angular + Spring Boot)

A production-leaning prototype of a real-time multiplayer chess app built with Angular, Spring Boot, WebSockets/STOMP, and PostgreSQL (Supabase). It's designed to showcase solid full-stack fundamentals, clear architecture, and the precise features required for a comprehensive chess application.

## ✨ Features (mapped to implementation levels)

### 🟢 Level 1 — Base (required)
- 👤 **Authentication**: Register + login with JWT tokens
- 🟢 **Presence**: List currently online users (live updates)
- ✉️ **Invitations**: Invite online players + accept/decline (live DM via STOMP user destinations)

### 🟡 Level 2 — Functional (expected)
- 🎮 **Game creation** upon invitation acceptance
- 🧩 **8×8 chessboard UI** built with Angular
- 🔁 **Real-time move synchronization** via WebSockets
- 💾 **Minimal persistence**: Each move stored in database
- 🔄 **Reconnection/resume** for active games

### 🔴 Level 3 — Bonus (optional)
- ⏯️ **Replay system**: Step through past moves sequentially
- ✅ **Server-side move validation** with sanity checks
- 🧭 **UX enhancements**: Side panel with move list, game status, etc.

## 🧱 Architecture

```
repo-root/
├── backend/                           # Spring Boot 3.5.x (Java 17/22)
│   ├── src/main/java/com/example/chess/
│   │   ├── config/                    # Security, CORS, WebSocket, STOMP interceptor (JWT on CONNECT)
│   │   ├── auth/                      # AuthController, AuthService, JwtService
│   │   ├── user/                      # User entity + repository
│   │   ├── lobby/                     # LobbyService + LobbyController (presence, invites)
│   │   ├── game/                      # Game, Move, GameStatus, repos, GameService, GameController, GameRules
│   │   └── common/                    # Dto.java (single holder for REST/WS DTOs)
│   └── src/main/resources/
│       └── application.properties     # Uses env vars for DB + JWT
└── frontend/                          # Angular 17+ (Node 18+)
    └── chess-client/
        ├── src/app/core/              # Auth service/guard/interceptor, STOMP service, models
        ├── src/app/shared/            # UI components (Navbar, etc.)
        └── src/app/features/          # auth/, lobby/, game/ modules & pages
```

### Key realtime design choices
- **STOMP over WebSockets** (`/ws`), with broker prefixes: `/app` (client → server), `/topic` (broadcast), `/user` (DM)
- **JWT verified during STOMP CONNECT** via a ChannelInterceptor (no unsecured WS traffic)
- **Spring's user destinations**: `convertAndSendToUser(<principalName>, "/queue/...")` where the principal name is the userId (string) to guarantee correct routing

## 🚀 Quickstart

### Prerequisites
- **Java 17 or Java 22** (JDK) + Maven/Gradle
- **Node 18+** + Angular CLI
- **Supabase account** (PostgreSQL)
- **IntelliJ IDEA** (recommended for environment variable setup)

## 🗄️ Supabase Setup (PostgreSQL)

### Create a new project
In Supabase, create a project and copy:
- **Host** (e.g. `aws-1-eu-north-1.pooler.supabase.com`)
- **Database**: `postgres` (default)
- **User**: `postgres.<project-ref>` (e.g., `postgres.vbdtwmmxdijmbzwbboqk`)
- **Password**: Set/reset a strong password (you control this)
- **Port**: `5432` (Session Pooler) or `6543` (Transaction Pooler)

### Database URL format
```
postgresql://postgres.<project-ref>:<DB_PASSWORD>@<region>.pooler.supabase.com:5432/postgres?sslmode=require
```

### Create schema
In Supabase SQL editor, run your schema script (users, games, moves, optional invitations, and views).

## 🔧 Backend Configuration (Spring Boot)

### Environment Variables Setup in IntelliJ IDEA

> **⚠️ SECURITY WARNING**: Never commit real database credentials or secrets to version control!

In IntelliJ IDEA, set these environment variables:
1. Go to **Run** → **Edit Configurations**
2. Select your Spring Boot configuration
3. In **Environment Variables**, add:

```bash
DB_URL=jdbc:postgresql://aws-1-eu-north-1.pooler.supabase.com:5432/postgres?sslmode=require
DB_USER=postgres.vbdtwmmxdijmbzwbboqk
DB_PASSWORD=your_actual_supabase_password
JWT_SECRET=ChessApp2024!MultiplayerGame#SecureAuth$Token9876543210ABCDEF
SERVER_PORT=8081
```

**Important Notes:**
- Replace `your_actual_supabase_password` with your real Supabase password
- The JWT_SECRET should be a long, random string (minimum 256 bits)
- Use different secrets for production environments
- Consider using a `.env` file locally (add to `.gitignore`)

### Application Properties
Your `application.properties` should reference environment variables:

```properties
# Database Configuration
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USER}
spring.datasource.password=${DB_PASSWORD}
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA/Hibernate Configuration
spring.jpa.hibernate.ddl-auto=update
spring.jpa.open-in-view=false
spring.jpa.properties.hibernate.jdbc.time_zone=UTC

# Server Configuration
server.port=${SERVER_PORT:8081}

# JWT Configuration
jwt.secret=${JWT_SECRET}

# Logging (tune during development)
logging.level.org.springframework.web=INFO
logging.level.org.hibernate.SQL=INFO
logging.level.com.zaxxer.hikari=INFO
```

### Run the backend
From `backend/` directory:

```bash
./mvnw spring-boot:run
# or with Gradle
./gradlew bootRun
```

Server should boot at `http://localhost:8081`.

## 🧩 Frontend Configuration (Angular)

### Environment setup
Edit `frontend/chess-client/src/environments/environment.ts`:

```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8081/api',
  wsUrl: 'ws://localhost:8081/ws'
};
```

### Install & run
```bash
cd frontend/chess-client
npm install
ng serve
```

App available at: `http://localhost:4200`

## 🔐 Authentication Flow (JWT)

1. **Register**: `POST /api/auth/register` → `{ token, userId, displayName }`
2. **Login**: `POST /api/auth/login` → `{ token, userId, displayName }`
3. **Current user**: `GET /api/users/me` (with `Authorization: Bearer <token>`)

Frontend stores the token in `localStorage`. An HTTP interceptor adds the `Authorization` header. `AuthGuard` protects routes like `/lobby` and `/game/:id`.

## 📡 WebSockets / STOMP

### Configuration
- **Endpoint**: `/ws`
- **Prefixes**:
  - Client → Server: `/app/...`
  - Broadcast: `/topic/...`
  - Direct to user: `/user/queue/...`
- **Heartbeats**: Enabled both sides to keep connections healthy
- **JWT**: Sent via `Authorization` connect header; validated in a `ChannelInterceptor`
- **Principal name**: `userId` (string) - critical for `convertAndSendToUser(userId, "/queue/invitations", ...)` routing

### Key Destinations

#### Presence
- Client requests snapshot: `SEND /app/presence.request`
- Server broadcasts: `/topic/online-users`

#### Invitations
- Client sends: `SEND /app/invite.send` (payload `{ toUserId }`)
- Server DMs recipient: `/user/queue/invitations`
- Recipient replies: `SEND /app/invite.reply` (payload `{ invitationId, accept }`)
- On accept, server DMs both: `/user/queue/game-created`

#### Game moves
- Client sends move: `SEND /app/games/{id}/move`
- Server validates, persists, and broadcasts: `/topic/games/{id}`

## 🗃️ Persistence Model

### User
- `id`, `email` (unique), `password_hash`, `display_name`, timestamps

### Game
- `id`, `white_player_id`, `black_player_id`
- `status` (CREATED|STARTED|FINISHED)
- `last_fen` (optional snapshot)
- timestamps

### Move
- `id`, `game_id`, `move_number`, `from_square`, `to_square`
- `san` (optional), `promotion` (optional), `played_by_user_id`, `played_at`, `fen_after` (optional)
- Indexes for `game_id`, `(game_id, move_number)`

### (Optional) Invitation
- `id`, `from_user_id`, `to_user_id`, `status` (PENDING|ACCEPTED|DECLINED), `game_id?`
- timestamps

### Views
- `v_user_active_games` — quick active games lookup

## 🧪 Testing

### Backend (JUnit + Mockito)
- **Auth**: Service tests for register/login (hashing, duplicate email, JWT generation)
- **GameController**: `@WebMvcTest` with mocked `GameService`, `SimpMessagingTemplate`
- **GameService**: Unit tests for `recordMove`, turn validation, basic rules (via `GameRules`)
- **WebSocket**: Slice tests for `JwtChannelInterceptor` (CONNECT with token OK/KO)

Run tests:
```bash
./mvnw test
# or
./gradlew test
```

### Frontend (Jasmine/Karma)
- Services (Auth/Lobby/Game) with `HttpClientTestingModule`
- Components for form validation, navigation, and dialog behavior

```bash
ng test
```

## 🧭 How to Demo

1. **Start backend**, then **start frontend**
2. **Register two users** (e.g., in Chrome and Firefox, or Chrome normal vs Incognito)
3. **Both login** → `/lobby` lists 2 online users
4. **User A clicks Invite on User B** → User B receives a dialog to accept/decline
5. **On accept**, both are routed to `/game/:id`. Moves are synced in real-time
6. **Reload either tab** — the game resumes (moves fetched from DB; live WS resumes)
7. **(Bonus)** Use replay controls in the game view to step through moves

## 🛠️ Troubleshooting

### Common Issues

- **Port 8080 already in use** → App uses 8081; adjust `server.port` or free 8081
- **Cannot connect to DB / UnknownHost** → Use the Supabase IPv4 Pooler URL (5432 or 6543), with `sslmode=require`
- **"Cannot alter type of a column used by a view"** → Avoid destructive `ddl-auto` changes; drop/recreate dependent view or run a migration (Flyway) that handles it

### WebSocket Issues

**No invitations received but connected:**
- Ensure principal name is `userId` string in `JwtChannelInterceptor`
- Client subscribes to `/user/queue/invitations`
- Heartbeats enabled on both server and client

**Material Dialog not showing (Angular standalone)** → Add `provideAnimations()` + `provideMatDialog()` in bootstrap providers

## 🧰 Development Tools

- **PowerShell scaffold scripts** (backend Java packages/classes) included for clean structure generation
- **Angular CLI commands** listed in docs to generate core/shared/features modules and components

## 🧪 Tech Stack

### Backend
- **Spring Boot 3.5.x** (Web, Security, WebSocket, Validation, Data JPA, Actuator)
- **PostgreSQL** (Supabase), Hikari, Lombok
- **JWT**: `io.jsonwebtoken:jjwt-*`
- **Testing**: JUnit 5, Mockito

### Frontend
- **Angular 17+**
- **Angular Material** (dialogs)
- **STOMP over WebSockets**
- **RxJS**, Reactive Forms

## 🔒 Environment Variables & Security

### Required Environment Variables
```bash
DB_URL=jdbc:postgresql://<region>.pooler.supabase.com:5432/postgres?sslmode=require
DB_USER=postgres.<project-ref>
DB_PASSWORD=<YOUR_SUPABASE_PASSWORD>
JWT_SECRET=<A_LONG_RANDOM_STRING>
SERVER_PORT=8081
```

### Security Best Practices
- ❗ **Never commit secrets** to version control
- Use `.gitignore` for `.env` files
- Rotate secrets regularly
- Use different secrets for different environments
- Verify your CI/CD (if any) injects runtime secrets securely

## 📜 License / Notes

This repository is for technical assessment/demo purposes. You may adapt it for your own portfolio; **be sure to rotate and remove all secrets** before sharing or deploying.

> **⚠️ Important**: The example environment variables shown in this README are for demonstration purposes only. Always use your own unique, secure credentials and never commit them to version control.