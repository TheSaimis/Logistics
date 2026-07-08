# Logistics IMS — agent notes

Full-stack inventory management system. Angular 20 + SCSS (`frontend/`, port 4200),
Spring Boot 3.5 / Java 21 (`backend/`, port 8080), PostgreSQL 17 (local service, db `logistics`,
postgres/postgres). See README.md for features, ROADMAP.md for the agreed plan — read it
before adding features; the "Production gate" section is binding before any deployment.

## Environment quirks (will bite you)

- **JAVA_HOME must be set explicitly** — PATH java is 1.8. Use:
  `$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-21.0.10.7-hotspot'` before any `.\mvnw.cmd`.
- **No global Maven** — always use the wrapper `backend\mvnw.cmd`.
- **Angular is pinned to v20** — installed Node 24.13.1 is below Angular 21/22 minimums. Don't upgrade @angular/* past 20 without upgrading Node first.
- Spring Boot pinned to 3.5.x — Initializr default (4.x) renamed starters; don't bump casually.
- No git repo yet (roadmap Phase 1).

## Run / test

- Backend: `cd backend; .\mvnw.cmd spring-boot:run` (background). Health: GET :8080/actuator/health
- Frontend: preview_start name "frontend" (.claude/launch.json) or `npx ng serve` in `frontend/`
- Backend tests: `.\mvnw.cmd test` (unit-only, no DB needed)
- Frontend prod-ish check: `npx ng build --configuration development`

## Auth & profiles

- `spring.profiles.default: dev`. Dev seeds users: admin@logistics.local / Admin123!,
  manager@… / Manager123!, viewer@… / Viewer123! (UserSeeder, @Profile("dev") — never widen it).
- Outside dev, startup FAILS unless JWT_SECRET env var is overridden (SecurityStartupCheck — intentional, don't "fix" it).
- Login lockout: 5 fails → 15 min 429 (LoginAttemptService, in-memory). If you lock yourself out during testing, restart the backend.
- API testing: POST /api/auth/login → accessToken; pass as `Authorization: Bearer <token>`.

## Conventions & gotchas

- JPQL with nullable params on Postgres: wrap in `cast(:param as string/long)` or Hibernate
  binds nulls as bytea → `function lower(bytea) does not exist`. Already done in repositories; follow the pattern.
- Unhandled exceptions used to surface as 401s (the /error endpoint is now permitAll; a 401 on a valid token still usually means a 500 underneath — check backend logs).
- Products are soft-deleted (`active=false`) to preserve movement history. Stock changes go
  ONLY through StockService.recordMovement (guards negative stock, writes the movement row).
- Admin-relevant mutations must call `AuditService.record(action, entityType, id, details)` —
  keep this when adding new admin features.
- Passwords: validate with `PasswordPolicy.validate()` wherever a password is set.
- Frontend: standalone components + signals, lazy routes in app.routes.ts, shared styles in
  styles.scss (user has customized --border; don't revert design tokens). API base in core/api.ts.
- Table preferences (columns/page size) persist in localStorage keys `lg_product_cols`, `lg_product_pagesize`; SKU+Name columns are locked on purpose.
- Browser automation: preview_fill does not trigger Angular ngModel — use preview_eval dispatching `new Event('input', {bubbles:true})`.
