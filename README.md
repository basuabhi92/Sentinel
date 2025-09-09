# Nano Backend — Sentinel (Notification Assistant)

Event-first backend built on [Nano](https://github.com/NanoNative) with PostgreSQL + jOOQ. 
- **Clear event boundaries** between application services and DB.
- Planned work: **modes**: silent / push / digest (Rule engine later). Onboard more Apps.

## Architecture

### Services (extend `org.nanonative.nano.core.model.Service`)
- **PostgreSqlService** — the **only** service touching Postgres (via jOOQ). Attends to internal events.
- **UserService** — `/auth/register`, `/auth/login`
- **AppService** — `/api/apps/supported`, `/api/apps/{app}/link`, `/api/apps/{app}/unlink`.
- **NotificationsService** — provider ingestion & user actions:
    - `/webhook/ingest` → emits `notification.ingested`, persists notification.
    - `/api/apps/{app}/notifications/mark-read` → marks and purges.

## Requirements

- Java 21
- Maven 3.9+
- PostgreSQL 14+
- jOOQ code generation connectivity to DB
- Docker for local

## Build & Run

### Generate jOOQ classes and build project in one shot
* Setup Postgres DB on local. Run command `docker-compose up flyway` to bring up Postgres instance and run flyway migrations.
* Execute command: sh jooq-generator-local.sh -> this script will set all required env variables and run `mvn -Pjooq clean generate-sources install`.

The above two steps should make the app ready to start (if all dependencies have been resolved).

## Security & Privacy
App integrations require explicit user consent (OAuth or provider export).
Store only what’s needed for routing and user experience.
Provide “Mark all read & purge” to avoid data hoarding.

## Deployment
DB migrations via Flyway in CI (already set up).
Backend on GCE
