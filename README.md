# Rangoli Wars

A multiplayer, turn-based territory game. Players take turns coloring tiles on a grid. Closing a loop of your tiles captures everything inside it, and power cards (Splash, Shield, Swap) change the board. Bots fill empty seats and take over for players who time out or disconnect.

Built with **Core Java 17, Spring Boot 3.5, Spring Security (JWT), Spring Cloud Gateway, WebSockets (STOMP), Oracle Database (plain SQL via JdbcTemplate), SOAP web services (Spring WS + JAXB), XML (StAX + XSD), JSP/JSTL and Servlets, React + TypeScript, Docker, and GitHub Actions with SonarCloud**.

## Architecture

```
browser ──► nginx (frontend) ──► gateway :8080 ──┬─► auth-service :8081 ──────► Oracle (auth_svc)
                                                  ├─► game-service :8082 (REST + /ws STOMP)
                                                  │        │ POST /internal/matches
                                                  │        ▼
                                                  └─► leaderboard-service :8083 ► Oracle (leaderboard_svc)
                                                        REST · SOAP /soap · JSP /admin · CSV servlet
```

| Module | What it does |
|---|---|
| `game-engine` | Pure Java game rules, no Spring. **State** (lobby / playing / finished), **Command** with undo (each move type), **Factory** (cards, commands), **Strategy** (Easy / Medium / Hard bots), **Observer** (game events). |
| `auth-service` | Register / login, BCrypt passwords, issues JWTs. |
| `game-service` | Rooms, turn timer (`ScheduledExecutorService`), bots, presence tracking, live state over STOMP; reports finished matches. |
| `leaderboard-service` | Stores matches and ratings in Oracle with SQL (window functions, batch inserts), REST API, XML replay export, SOAP endpoint with WSDL, JSP admin dashboard, CSV export servlet. Packaged as a WAR. |
| `gateway` | Spring Cloud Gateway: single entry point, request IDs, WebSocket routing, hides `/internal/**`. |
| `frontend` | React + TypeScript (Vite) client. |

## Run locally (no Docker, H2 database)

Requires Java 17+ and Node 20+.

```powershell
.\scripts\run-local.ps1          # builds and starts the 4 services in separate windows
cd frontend; npm install; npm run dev
```

Open http://localhost:5173. Admin page: http://localhost:8080/admin (`admin` / `admin123` locally).

## Run the full stack with Docker (Oracle)

```bash
cp .env.example .env             # then edit every value
docker compose up -d --build
```

Open http://localhost (or the `HTTP_PORT` from `.env`). The first Oracle start takes 1–2 minutes.

## Tests

```bash
./gradlew test                   # unit + integration tests for all modules
./gradlew test jacocoTestReport  # coverage reports in */build/reports/jacoco
```

## Useful URLs (through the gateway)

| URL | |
|---|---|
| `/api/auth/swagger-ui.html`, `/api/game/swagger-ui.html`, `/api/leaderboard/swagger-ui.html` | Swagger UI per service |
| `/soap/leaderboard.wsdl` | SOAP WSDL (`getTopPlayers`, `getPlayerStats`) |
| `/api/leaderboard/matches/{id}/replay.xml` | XML replay of a match (schema: `leaderboard-service/src/main/resources/xml/replay.xsd`) |
| `/admin` | JSP admin dashboard (HTTP Basic) |
| `/admin/export/leaderboard.csv` | Servlet CSV export |
