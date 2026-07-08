# Logistics IMS — Plan & Roadmap

Status date: 2026-07-08. Reviewed by LLM council (2 rounds); round 2 verdict: unanimous
approval for the current local/dev stage, contingent on the Production gate below being
treated as binding before any deployment beyond localhost.

## Where the project stands

**Done and verified:**
- Full stack running locally: Angular 20 (:4200) + Spring Boot 3.5/Java 21 (:8080) + PostgreSQL 17 (`logistics` db)
- Auth: JWT access (15 min) + rotating refresh tokens (DB-stored, revocable), Google OAuth2 wiring, roles ADMIN/MANAGER/VIEWER enforced server-side
- Inventory: products CRUD (soft delete), search + filters (category, supplier, status, stock level), server-side sorting, configurable columns (SKU/Name locked), page size — persisted per user in localStorage
- Stock: IN/OUT/ADJUSTMENT/TRANSFER movements with negative-stock protection and full history
- Dashboard with warehouse filter; admin analytics (4 charts); admin user management
- Security hardening (council round 1 fixes, all verified by execution):
  - Demo users seeded only under `dev` profile (`spring.profiles.default: dev`)
  - App refuses to boot outside `dev` with the default JWT secret (`SecurityStartupCheck`)
  - Audit trail (`audit_log` table, `AuditService`) for user/product/warehouse changes + admin Audit log page
  - Login lockout: 5 failed attempts → 15 min lock (429), in-memory (single node)
  - Password policy: min 8, letters+digits, common-password blocklist — applied to register and admin-created accounts
  - Demo credentials removed from the login page
- Tests: 14 unit tests (StockService movement math + PasswordPolicy) — `mvnw test`

## Phase 1 — Operating basics (next up)

1. **Password reset flow** — token-based email reset (needs SMTP config), or minimally an
   admin-triggered "force reset on next login". Currently recovery = admin sets a new password.
2. **Git history** — repo is not yet under version control. `git init`, commit, remote.
3. **Reorder suggestions** (council's top feature pick): burn rate from stock_movements +
   current level + reorder_level → "draft PO" list per supplier. Turns record-keeping into decisions.
4. Category/Supplier delete audit entries + movement CSV export (small, high admin value).

## Phase 2 — Production gate (BINDING before exposure beyond localhost)

Council kill switch: exposing the app before these are done voids the security verdict.

1. **Move JWTs out of localStorage** → httpOnly, Secure, SameSite cookies for the refresh
   token (access token can stay in memory). Re-enable CSRF protection for the cookie flow.
2. **HTTPS** everywhere (reverse proxy — Caddy/nginx/Traefik).
3. **Secrets**: real `JWT_SECRET` (≥64 random bytes b64), real DB password, `SPRING_PROFILES_ACTIVE=prod`,
   `CORS_ORIGINS` set to the real frontend origin, `GOOGLE_CLIENT_ID/SECRET` if OAuth used.
4. **Docker Compose**: postgres + backend + frontend behind the proxy (user already planned this).
5. **DB backups**: scheduled `pg_dump` at minimum; test a restore once.
6. Rate limiting across nodes (move LoginAttemptService to DB/Redis) — only if scaling past one node.
7. Actuator: keep only `health` exposed publicly; secure the rest.

## Phase 3 — Growth features (after production)

- Per-role dashboards (manager: throughput/reorder alerts; admin: audit/security health)
- Purchase orders & receiving workflow (links suppliers → IN movements)
- Barcode scanning support (the seed data already has scanner products…)
- Multi-warehouse transfer approvals; movement reversal (compensating entries, never edits)
- Email/webhook alerts on low stock
- i18n (Lithuanian first, given warehouse locations)

## Known technical debt

- `WarehouseController.list()` does N+1 stock sums (fine at 3 warehouses; fix with a grouped query when >20)
- Movements page loads first 200 products into a dropdown — replace with typeahead search at scale
- `AnalyticsService.movementsDaily` aggregates in Java — move to SQL `date_trunc` when movements grow large
- Mockito self-attaching warning under JDK 21+ (harmless; add `-javaagent` per Mockito docs when it breaks)
- No integration tests against a real DB (consider Testcontainers)
