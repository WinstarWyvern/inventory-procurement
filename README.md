# Inventory Procurement API (Java / Spring Boot version)

Backend Technical Test submission — the same Inventory Procurement business
flow (Purchase Request → Approval → Purchase Order → Goods Receipt →
Inventory Update) implemented in **Java 17 + Spring Boot 3 + Spring Data
JPA + PostgreSQL**.

> This is a from-scratch reimplementation of the same case study, not a
> line-by-line port — it follows normal Spring Boot conventions
> (`@RestController` / `@Service` / `@Repository` / entities) rather than
> mirroring the structure of any other version.

## A note on how this was built

I don't have a Java/Maven build environment with internet access to Maven
Central in the sandbox I write code in, so **I was not able to run
`mvn test` or `mvn spring-boot:run` myself** to verify this compiles and
passes, the way I normally would. I wrote it carefully, kept every class
small and single-purpose, and the business logic mirrors an equivalent
implementation I *did* build and fully test end-to-end (same rules, same
test scenarios). Still — please run `mvn clean test` as your first step
after opening this in VS Code, and if anything doesn't compile, paste me
the error and I'll fix it immediately.

## Project Overview

Warehouse staff (`USER`) raise Purchase Requests for a single warehouse.
An `APPROVER` approves or rejects them. An approved request becomes a
Purchase Order against a supplier. Goods received against that order —
including partial deliveries — update the warehouse's stock balance and
write an inventory movement record.

## Tech Stack

- **Language:** Java 17
- **Framework:** Spring Boot 3.3 (Spring MVC / Spring Web)
- **Database access:** Spring Data JPA (Hibernate) — repository interfaces
  with derived query methods and a few `@Query`/`@EntityGraph` annotations
  where a plain derived method isn't expressive enough
- **Database:** PostgreSQL 16 or 17
- **Migrations:** Flyway
- **Auth:** JWT (`jjwt`) + Spring Security, `BCryptPasswordEncoder` for passwords
- **Validation:** Jakarta Bean Validation (`spring-boot-starter-validation`)
- **Boilerplate reduction:** Lombok (`@Getter`/`@Setter` on entities only —
  DTOs are plain Java `record`s, which need no Lombok at all)
- **Testing:** JUnit 5 + Spring Boot Test + MockMvc, against a real local
  PostgreSQL test database
- **API docs:** Postman Collection (`docs/postman_collection.json`)
- **Build tool:** Maven

Everything used here is free and open-source — no paid services, no
Docker required (though you can use it if you prefer).

## Project Structure

```
src/main/java/com/procurement/api/
  ProcurementApiApplication.java   # main() - Spring Boot entrypoint
  domain/          # JPA entities (User, Product, Supplier, Warehouse,
                   #   InventoryBalance, InventoryMovement,
                   #   PurchaseRequest(+Item), PurchaseOrder(+Item),
                   #   GoodsReceipt(+Item)) and their enums
  repository/      # Spring Data JPA repository interfaces
  dto/             # request/response records, one subpackage per module
  service/         # business logic - one class per module, plain Java,
                   #   no framework magic beyond @Service/@Transactional
  web/             # @RestController classes - thin, delegate to services
  security/        # JWT creation/verification + the auth filter
  config/          # SecurityConfig, DataSeeder
  common/          # AppException, error/response envelopes, global
                   #   exception handler
src/main/resources/
  application.yml            # config, reads from env vars with sane defaults
  db/migration/V1__init.sql  # Flyway migration - the whole schema
src/test/java/com/procurement/api/
  AbstractIntegrationTest.java   # shared MockMvc + fixture setup
  PurchaseRequestTests.java
  PurchaseOrderTests.java
  GoodsReceiptTests.java
docs/
  postman_collection.json / postman_environment.json
```

Each module (e.g. Purchase Request) has its logic in exactly one service
class. Controllers do almost nothing except map HTTP ↔ DTOs and call the
service — all the "can this happen" business rules live in `service/`,
in plain, explicit `if` statements rather than anything clever, so they're
easy to read top to bottom.

## Database Design

Same schema shape as the case study describes, created by one Flyway
migration (`V1__init.sql`) so the whole database can be rebuilt from
nothing by just starting the app:

- **Master data:** `users`, `products`, `suppliers`, `warehouses`, each
  with `is_active` where required and unique constraints on `sku` /
  warehouse `code` / `username`.
- **Purchase Request:** `purchase_requests` (header) + `purchase_request_items`.
  A unique constraint on `(purchase_request_id, product_id)` stops the same
  product appearing twice on one request, enforced by the database itself.
- **Purchase Order:** `purchase_orders` (header) + `purchase_order_items`.
  A unique constraint on `purchase_request_id` enforces "one PR → at most
  one PO" at the database level — not just in application code.
- **Goods Receipt:** `goods_receipts` (header) + `goods_receipt_items`.
- **Inventory:**
  - `inventory_balances` — one row per `(warehouse_id, product_id)`
    holding the *current* stock quantity (what the stock-lookup endpoints
    read).
  - `inventory_movements` — an append-only ledger of every stock change
    (currently only `PURCHASE_RECEIPT`), with a signed quantity and a
    `reference` (the GR number) — the audit trail behind every balance.

Enums (`role`, `status`, `movement_type`) are stored as `VARCHAR` with a
`CHECK` constraint rather than native Postgres enum types, and mapped on
the Java side with `@Enumerated(EnumType.STRING)`. This is a deliberate
simplification — native Postgres enums need extra Hibernate configuration
to work smoothly, and a `VARCHAR` + `CHECK` gives the same guarantee
(only valid values can be stored) with far less moving parts. See
"Engineering Decisions" below.

## Setup (VS Code, 100% free tooling)

You need three things, all free: a JDK, Maven, and PostgreSQL. Everything
below works with no Docker and no paid accounts.

### 1. Install the JDK (Java 17)

- **Windows:** download the free [Eclipse Temurin 17 JDK](https://adoptium.net/temurin/releases/?version=17) installer and run it.
- **macOS:** `brew install openjdk@17` (or the Temurin installer above).
- **Linux (Debian/Ubuntu):** `sudo apt install openjdk-17-jdk`.

Verify: `java -version` should print `17.x`.

### 2. Install Maven

- **Windows:** `choco install maven` (via [Chocolatey](https://chocolatey.org/)), or download from [maven.apache.org](https://maven.apache.org/download.cgi) and add its `bin` folder to your `PATH`.
- **macOS:** `brew install maven`.
- **Linux:** `sudo apt install maven`.

Verify: `mvn -version`.

> Alternative: you don't strictly need Maven on your `PATH` at all if you
> install the **"Extension Pack for Java"** in VS Code (see step 4) — it
> can run Maven goals for you from its own bundled tooling. Installing
> Maven yourself is still recommended so `mvn` also works from a plain
> terminal.

### 3. Install PostgreSQL (free, local)

- **Windows/macOS:** download the free installer from
  [postgresql.org/download](https://www.postgresql.org/download/) (choose
  version 16 or 17). During setup, set a password for the `postgres` user
  and remember it — you'll use it below. The installer also offers
  **pgAdmin**, a free GUI you can use to browse the database if you like.
- **macOS (Homebrew alternative):** `brew install postgresql@16 && brew services start postgresql@16`.
- **Linux (Debian/Ubuntu):** `sudo apt install postgresql` (then `sudo -u postgres psql` to set a password with `\password postgres`).

Then create the two databases this project uses (one for running the app,
one for tests) — easiest via `psql` or pgAdmin:

```sql
CREATE DATABASE inventory_procurement;
CREATE DATABASE inventory_procurement_test;
```

### 4. Install VS Code extensions

Open VS Code → Extensions (`Ctrl+Shift+X` / `Cmd+Shift+X`) → install:

- **Extension Pack for Java** (by Microsoft) — bundles language support,
  debugger, test runner, and Maven support.
- **Spring Boot Extension Pack** (by VMware/Microsoft) — adds a "Spring
  Boot Dashboard" so you can start/stop the app with one click instead of
  the terminal, plus `application.yml` autocompletion.
- (Optional) **PostgreSQL** (by Microsoft/Chris Kolkman) or use pgAdmin
  if you want a GUI to look at the database's tables and rows.

### 5. Open the project and configure the database connection

Open this folder in VS Code. Configuration is read from environment
variables (with working local defaults already baked into
`application.yml`), so for a fresh Postgres install on `localhost:5432`
with username `postgres`, **the only thing you may need to change** is
the password. Easiest way: create a `.env`-style override isn't built
into plain Spring Boot, so instead either:

- **Option A (simplest):** if your local `postgres` user's password is
  `postgres` (matches the defaults), you don't need to change anything.
- **Option B:** set environment variables before running, e.g. in a
  terminal:
  ```bash
  export DATABASE_URL=jdbc:postgresql://localhost:5432/inventory_procurement
  export DATABASE_USERNAME=postgres
  export DATABASE_PASSWORD=your_actual_password
  ```
  (On Windows PowerShell: `$env:DATABASE_PASSWORD="your_actual_password"`.)
- **Option C:** edit the defaults directly in
  `src/main/resources/application.yml` — fine for local-only development.

### 6. Run it

From the integrated terminal in VS Code:

```bash
mvn clean install     # downloads dependencies, compiles, runs tests
mvn spring-boot:run   # starts the API on http://localhost:8080
```

Or, using the **Spring Boot Dashboard** (from the extension pack): open
the Spring Boot panel in the sidebar, find `inventory-procurement-api`,
and click the ▶ Run button.

Flyway runs the migration automatically on startup — no separate
migration step needed. `GET http://localhost:8080/health` should return
`{"status":"ok"}` once it's up.

### 7. Seed test data

The app seeds master data + the required `1 USER, 1 APPROVER` when
`SEED_ENABLED=true` (it's `false` by default so a normal restart never
re-seeds unexpectedly — though the seeder is idempotent either way, safe
to run more than once).

```bash
SEED_ENABLED=true mvn spring-boot:run
```

(PowerShell: `$env:SEED_ENABLED="true"; mvn spring-boot:run`.)

This creates:
- 2 warehouses (Jakarta, Surabaya), 3 products, 2 suppliers
- **Test credentials:**

| Role | Username | Password |
|---|---|---|
| USER | `john.user` | `password123` |
| APPROVER | `sarah.approver` | `password123` |

You can stop the app (`Ctrl+C`) and start it again normally afterwards
(`SEED_ENABLED` unset / `false`) — the data stays.

## Environment Variables

All read from environment variables in `application.yml`, each with a
sensible local default:

| Variable | Description | Default |
|---|---|---|
| `DATABASE_URL` | JDBC connection string | `jdbc:postgresql://localhost:5432/inventory_procurement` |
| `DATABASE_USERNAME` | Postgres username | `postgres` |
| `DATABASE_PASSWORD` | Postgres password | `postgres` |
| `PORT` | HTTP port | `8080` |
| `JWT_SECRET` | Secret used to sign JWTs — **change in production** | (a long dev-only default) |
| `JWT_EXPIRATION_MINUTES` | JWT lifetime in minutes | `480` (8 hours) |
| `SEED_ENABLED` | Set to `true` once to seed master data + test users | `false` |

## Migration

Flyway migrations live in `src/main/resources/db/migration/` and run
**automatically every time the app starts** — there's no separate
"migrate" command to run. `V1__init.sql` creates the entire schema from
nothing.

If you ever change an entity, add a new file like `V2__add_something.sql`
rather than editing `V1__init.sql` — Flyway tracks which migrations have
already run and refuses to modify a checksummed file that already
executed.

## Seed

See "Setup, step 7" above: `SEED_ENABLED=true mvn spring-boot:run`.

## Run Application

```bash
mvn spring-boot:run
```

or the Spring Boot Dashboard's ▶ Run button in VS Code. The API listens
on `http://localhost:8080` (override with the `PORT` env var).

## Testing

```bash
mvn test
```

Tests run against a **separate** database
(`inventory_procurement_test`, per `src/test/resources/application-test.yml`)
so they never touch your development data — create it once with
`CREATE DATABASE inventory_procurement_test;` (see Setup step 3). Each
test method runs inside a transaction that's automatically rolled back
afterwards, so tests never leak data between each other and no manual
cleanup step is needed.

Tests cover the same scenarios called out in the brief:
- Cannot submit a Purchase Request without items
- Cannot approve/reject a Purchase Request that is not `SUBMITTED`
- Cannot create a Purchase Order from a non-`APPROVED` Purchase Request
- Cannot create more than one Purchase Order from the same Purchase Request
- Cannot receive a quantity greater than ordered (single receipt, and cumulative across receipts)
- A Goods Receipt increases warehouse stock
- A fully received Purchase Order becomes `RECEIVED`

Plus role-based authorization, inactive product/supplier rejection,
duplicate-product rejection, DRAFT-only editing, and receiving against a
`DRAFT`/`RECEIVED` PO.

## API Documentation

A Postman collection is at `docs/postman_collection.json`, with a
matching environment at `docs/postman_environment.json` (points at
`http://localhost:8080`).

1. Import both files into Postman.
2. Select the "Inventory Procurement - Local (Spring Boot)" environment.
3. Run **Auth → Login as USER** and **Auth → Login as APPROVER** first —
   each saves its JWT into a collection variable that every other request
   reuses automatically.
4. Requests are grouped in the order of the main business flow.

## Engineering Decisions

1. **Enums are stored as `VARCHAR` + `CHECK` constraint, not native
   PostgreSQL enum types.** Native Postgres enums need Hibernate-specific
   type configuration (a custom `@Type`/converter, or a dialect-specific
   annotation) to map cleanly to a Java `enum`, which is an extra moving
   part for very little benefit here. A `VARCHAR` column with a `CHECK
   (status IN (...))` constraint gives the exact same guarantee — only a
   fixed set of values can ever be stored — while staying plain, boring
   SQL that any tool can read, and maps to `@Enumerated(EnumType.STRING)`
   with zero extra configuration. Given the brief explicitly asks for
   understandable code over optimal code, this was the easier trade to
   make.

2. **The whole Goods Receipt operation runs inside one Spring
   `@Transactional` method**, covering every step the brief lists: create
   the receipt → update the PO's received quantities → recompute the PO's
   status → update the warehouse's stock balance → write the inventory
   movement. All validation happens before any entity is modified, and if
   anything throws partway through, Spring rolls the entire transaction
   back — none of the partial writes are committed. This directly answers
   the case study's "must not end up partially updated" requirement.
   `GoodsReceiptService.create()` is intentionally one long, linear method
   rather than split into many tiny private methods passing state around —
   for this specific operation, being able to read the whole sequence of
   steps top-to-bottom in one place is more understandable than jumping
   between several small methods.

3. **Business-invariant uniqueness rules are enforced at the database
   schema level, not only checked in Java.** For example, a unique
   constraint on `purchase_orders.purchase_request_id` guarantees "one PR
   → at most one PO" even if two requests raced past the application-level
   check at the same moment; a unique constraint on
   `(purchase_request_id, product_id)` guarantees no duplicate product
   line on a request. The service layer still checks these conditions
   first (to return a clean, specific `error.code` instead of a raw
   database error), but the database is the real backstop.

4. **DTOs are plain Java `record`s, not classes with Lombok or builder
   annotations.** Records are immutable, have almost no boilerplate on
   their own (constructor + accessors are generated by the language), and
   make the shape of every request/response obvious at a glance without
   needing to know what any annotation expands into. Lombok is used only
   on JPA entities, which genuinely need mutable getters/setters for
   Hibernate to manage them.

5. **Controllers are intentionally "dumb."** Every controller method is a
   few lines: bind the request, call one service method, wrap the result
   in `DataResponse.of(...)`. There is no logic in the `web` package at
   all (aside from `@PreAuthorize` on the two `APPROVER`-only endpoints) —
   anyone reading a controller can see the entire list of endpoints for a
   module at a glance, and anyone wanting the actual business rules knows
   to go straight to the matching `service` class.

## Assumptions

Same assumptions as the underlying case study interpretation:

- Both `USER` and `APPROVER` can perform the same "staff" write actions
  (create/edit Purchase Requests, submit, create Purchase Orders, mark as
  ordered, record Goods Receipts). Only `approve`/`reject` are restricted
  to `APPROVER` (`@PreAuthorize("hasRole('APPROVER')")` on those two
  endpoints specifically).
- Both roles can see all Purchase Requests / Purchase Orders, not only
  ones they created.
- A Purchase Order starts in `DRAFT` when created from an approved PR, and
  only moves to `ORDERED` via the explicit "Mark as Ordered" action —
  Goods Receipts require the PO to already be `ORDERED` or
  `PARTIALLY_RECEIVED`.
- `CANCELLED` exists in the Purchase Order status model (per the brief)
  but no endpoint currently transitions a PO into it — cancellation isn't
  in the "Required API Capabilities" list, so it was left out of the API
  surface while still existing in the data model.
- Supplier `email`/`phone` are optional.
- A Purchase Request's requester/approver is always taken from the
  authenticated JWT, never from the request body.
- Approval/rejection remarks are optional.

## Limitations

- No pagination or filtering beyond a `status` query parameter on list
  endpoints.
- No rate limiting or structured request logging beyond Spring Boot's
  defaults.
- Document numbering (`PR-2026-000001` etc.) counts existing rows for the
  year rather than using a dedicated database sequence — simple and
  sufficient at this system's expected scale, but not immune to a
  theoretical race under very high concurrent load at a year boundary.
- No `CANCELLED` transition endpoint for Purchase Orders (see Assumptions).
- **This code has not been compiled or run by me** — see the note at the
  top of this README. Please run `mvn clean test` first and let me know
  if anything needs fixing.
