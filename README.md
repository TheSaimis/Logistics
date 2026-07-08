# Logistics IMS — Inventory Management System

Full-stack logistics inventory management system.

| Layer    | Stack                                                                 |
|----------|-----------------------------------------------------------------------|
| Frontend | Angular 20 (standalone components, signals), TypeScript, SCSS         |
| Backend  | Java 21, Spring Boot 3.5, Spring Security, JPA/Hibernate, Flyway      |
| Database | PostgreSQL 17                                                          |
| Auth     | JWT access (15 min) + rotating refresh tokens (7 days), Google OAuth2 |

## Features

- **Dashboard** — product/warehouse/supplier counts, total units, stock value, low-stock alerts, recent movements
- **Inventory** — searchable, paginated product catalog with categories, suppliers, unit prices, reorder levels; per-warehouse stock breakdown; soft-delete (deactivate)
- **Stock movements** — IN / OUT / ADJUSTMENT / TRANSFER between warehouses, with negative-stock protection and full audit trail (who, when, reference)
- **Warehouses** — CRUD with capacity and live stored-unit counts
- **Partners** — categories and suppliers management
- **Admin panel** — user management: create/edit users, assign roles, enable/disable, delete
- **Security** — BCrypt passwords, stateless JWT with refresh-token rotation, role-based access (ADMIN / MANAGER / VIEWER), CORS locked to the frontend origin

## Roles

| Role    | Access                                            |
|---------|---------------------------------------------------|
| ADMIN   | Everything, including user management (`/api/admin/**`) |
| MANAGER | Read + write inventory, movements, warehouses, partners |
| VIEWER  | Read-only                                          |

Demo accounts (seeded on first start):

- `admin@logistics.local` / `Admin123!`
- `manager@logistics.local` / `Manager123!`
- `viewer@logistics.local` / `Viewer123!`

## Running locally

Prerequisites: JDK 21, Node 20+, PostgreSQL running on `localhost:5432` with a `logistics` database
(default credentials `postgres` / `postgres`; override with `DB_URL`, `DB_USER`, `DB_PASSWORD`).

**Backend** (port 8080):

```powershell
cd backend
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-21.0.10.7-hotspot'
.\mvnw.cmd spring-boot:run
```

Flyway creates the schema and seed data automatically on first start.

**Frontend** (port 4200):

```powershell
cd frontend
npm install
npx ng serve
```

Open http://localhost:4200.

## Google OAuth2 (optional)

Create OAuth credentials in Google Cloud Console with redirect URI
`http://localhost:8080/login/oauth2/code/google`, then set:

```powershell
$env:GOOGLE_CLIENT_ID='...'
$env:GOOGLE_CLIENT_SECRET='...'
```

The "Continue with Google" button on the login page then works end-to-end; first-time
Google users are provisioned automatically with the VIEWER role.

## Production notes

- Set `JWT_SECRET` (base64, ≥64 random bytes), real DB credentials, and `CORS_ORIGINS`
- Serve the frontend build (`npx ng build`) behind the same domain or a reverse proxy
- The refresh-token store is in PostgreSQL, so tokens can be revoked server-side (logout does this)
