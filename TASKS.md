# Implementation Tasks

This task list keeps the challenge implementation small, reviewable, and suitable for
AI-assisted development. Each phase should stay within its scope and avoid unrelated
infrastructure or broad refactors.

## Phase 0 — Baseline Repository Inspection

Goal:

- Inspect the existing Spring Boot structure, health endpoint, configuration, tests, formatting
  setup, and SQL initialization script.
- Identify existing package and testing conventions before implementation.

Files likely to be created or updated:

- None for implementation.
- Optional inspection notes only when explicitly requested.

Explicit non-goals:

- Do not implement event watchdog logic.
- Do not create controllers, entities, repositories, or DDL.
- Do not add dependencies or infrastructure.

Validation commands:

```bash
./mvnw test
./mvnw spotless:check
```

Status:

- Completed as inspection only.

## Phase 0B — Architecture Decisions

Goal:

- Document the selected MVP architecture and behavior before writing implementation code.
- Align future work around the modular monolith and `eventwatchdog` package structure.

Files likely to be created or updated:

- `docs/ARCHITECTURE.md`
- `AGENTS.md`
- Spotless may format existing Markdown files.

Explicit non-goals:

- Do not implement business logic.
- Do not create controllers, entities, repositories, or DDL.
- Do not update README examples yet.

Validation commands:

```bash
./mvnw spotless:apply
./mvnw spotless:check
./mvnw test
```

Status:

- Completed. Architecture decisions are documented in `docs/ARCHITECTURE.md`.

## Phase 1A — Documentation Planning

Goal:

- Create implementation task planning in `TASKS.md`.
- Create AI usage tracking in `AI_USAGE.md`.
- Prepare the repository for implementation without changing runtime behavior.

Files likely to be created or updated:

- `TASKS.md`
- `AI_USAGE.md`

Explicit non-goals:

- Do not implement business logic.
- Do not create controllers, entities, repositories, or DDL.
- Do not modify Docker configuration, dependencies, or README content.

Validation commands:

```bash
./mvnw spotless:apply
./mvnw spotless:check
./mvnw test
```

Status:

- Completed as documentation planning only.

## Phase 1B — Data Model, DDL, JPA Entities, And Repositories

Goal:

- Define and implement the persistence model required by the event watchdog.
- Keep the persistence layer aligned with the architecture decisions and existing PostgreSQL setup.

Files likely to be created or updated:

- `docker/init-scripts/db/01-init-schema.sql`
- `src/main/java/com/clara/challenge/eventwatchdog/persistence/TraceEventEntity.java`
- `src/main/java/com/clara/challenge/eventwatchdog/persistence/TraceStateEntity.java`
- `src/main/java/com/clara/challenge/eventwatchdog/persistence/TraceEventRepository.java`
- `src/main/java/com/clara/challenge/eventwatchdog/persistence/TraceStateRepository.java`
- Persistence-focused tests only if useful for repository behavior.

Required scope:

- Update `docker/init-scripts/db/01-init-schema.sql`.
- Create `trace_events` for immutable event history.
- Create `trace_states` for current trace status lookup.
- Use PostgreSQL `jsonb` for event metadata.
- Use optimistic locking for trace state, likely with a `version` column and JPA `@Version`.
- Create JPA entities and repositories.

Explicit non-goals:

- Do not create REST endpoints yet.
- Do not implement full state transition orchestration yet.
- Do not add migration tools such as Flyway or Liquibase.
- Do not change Docker configuration.

Validation commands:

```bash
./mvnw spotless:apply
./mvnw spotless:check
./mvnw test
```

## Phase 2 — Domain State Transition Logic And Unit Tests

Goal:

- Implement deterministic state transition rules independent from HTTP.
- Keep business rules testable without requiring Spring context.

Files likely to be created or updated:

- `src/main/java/com/clara/challenge/eventwatchdog/domain/EventResult.java`
- `src/main/java/com/clara/challenge/eventwatchdog/domain/TraceStatus.java`
- `src/main/java/com/clara/challenge/eventwatchdog/domain/*Command.java`
- `src/main/java/com/clara/challenge/eventwatchdog/domain/*Snapshot.java`
- `src/main/java/com/clara/challenge/eventwatchdog/domain/TraceStateTransitionService.java`
- `src/test/java/com/clara/challenge/eventwatchdog/domain/*Test.java`

Required scope:

- Implement state transition rules for `STARTED`, `WAITING_OTHER_EVENT`,
  `TTL_EXPIRED_FOR_EVENT`, and `COMPLETED`.
- Use Java 21 records where appropriate for immutable command and snapshot objects.
- Add plain unit tests for business rules.
- Cover duplicate replay, unexpected event conflict, late event conflict, completed trace conflict,
  expired trace conflict, and lazy TTL expiration.

Explicit non-goals:

- No Spring context unless necessary.
- Do not create REST controllers yet.
- Do not add background workers, custom threads, schedulers, or external event infrastructure.

Validation commands:

```bash
./mvnw spotless:apply
./mvnw spotless:check
./mvnw test
```

## Phase 3 — REST API, Validation, And Error Handling

Goal:

- Implement the public HTTP API using the documented behavior and `/api` context path.
- Add validation, consistent errors, and low-noise business logging.

Files likely to be created or updated:

- `src/main/java/com/clara/challenge/eventwatchdog/api/EventController.java`
- `src/main/java/com/clara/challenge/eventwatchdog/api/TraceStatusController.java`
- `src/main/java/com/clara/challenge/eventwatchdog/api/dto/EventRequest.java`
- `src/main/java/com/clara/challenge/eventwatchdog/api/dto/TraceStatusResponse.java`
- `src/main/java/com/clara/challenge/eventwatchdog/application/*Service.java`
- `src/main/java/com/clara/challenge/eventwatchdog/error/*Exception.java`
- `src/main/java/com/clara/challenge/eventwatchdog/error/ApiExceptionHandler.java`
- Controller/application tests as needed.

Required scope:

- Implement `POST /api/events`.
- Implement `GET /api/traces/{traceId}/status`.
- Use Bean Validation for request validation.
- Enforce that `nextExpectedEvent` and `nextEventTtlSeconds` are provided together.
- Use consistent JSON error handling.
- Add meaningful, low-noise business logs.

Explicit non-goals:

- Do not add authentication or authorization.
- Do not add OpenAPI unless Phase 3B is explicitly started.
- Do not add brokers, schedulers, custom threads, or external infrastructure.

Validation commands:

```bash
./mvnw spotless:apply
./mvnw spotless:check
./mvnw test
./mvnw spring-boot:run
```

## Phase 3B — Optional OpenAPI Documentation

Goal:

- Add OpenAPI/Swagger only if endpoint contracts are stable and dependency compatibility with
  Spring Boot 4.0.2 is clear.

Files likely to be created or updated:

- `pom.xml`, only if a compatible dependency is selected.
- API documentation configuration files, only if required.
- `README.md`, if OpenAPI access details need to be documented.

Explicit non-goals:

- Do not let OpenAPI block the core deliverables.
- Do not add it if compatibility or time becomes risky.
- Do not change API behavior for documentation tooling.

Validation commands:

```bash
./mvnw spotless:apply
./mvnw spotless:check
./mvnw test
```

Fallback:

- If OpenAPI is skipped, use `README.md` examples and Hurl tests as the MVP API documentation.

Status:

- Completed with `org.springdoc:springdoc-openapi-starter-webmvc-ui`.
- Verified OpenAPI JSON at `/api/v3/api-docs`.
- Verified Swagger UI at `/api/swagger-ui/index.html` via `/api/swagger-ui.html` redirect.
- Kept `/health` available at runtime but hidden from the event watchdog OpenAPI spec.
- Kept springdoc path configuration out of `application.yml` because springdoc defaults already
  expose the desired URLs under the existing `/api` context path.
- Intentionally did not enable `spring.mvc.problemdetails.enabled` before Phase 4 Hurl validation
  to avoid changing built-in Spring error response behavior while explicit API error handling is in
  place.

## Phase 4 — Hurl E2E Tests

Goal:

- Validate the public HTTP API contract from the outside.
- Keep Hurl tests focused on request/response behavior, not internal database details.

Files likely to be created or updated:

- `hurl/started-flow.hurl`
- `hurl/waiting-other-event-flow.hurl`
- `hurl/completed-flow.hurl`
- `hurl/ttl-expired-flow.hurl`
- Optional edge-case Hurl files.

Required scope:

- Add Hurl tests for `STARTED`.
- Add Hurl tests for `WAITING_OTHER_EVENT`.
- Add Hurl tests for `COMPLETED`.
- Add Hurl tests for `TTL_EXPIRED_FOR_EVENT`.
- Add edge-case Hurl tests for duplicate event replay, unexpected event conflict, late event
  conflict, completed trace conflict, expired trace conflict, and unknown trace if feasible.

Explicit non-goals:

- Do not assert internal database details.
- Do not depend on test execution order unless the Hurl file itself creates the required state.

Validation commands:

```bash
./mvnw spotless:apply
./mvnw spotless:check
./mvnw test
./mvnw spring-boot:run
hurl --test hurl/*.hurl
```

Status:

- Phase 4A implementation completed with the four required flow scenarios.
- Runtime validation passes all four scenarios. The waiting fixture uses a deterministic
  far-future deadline so lazy expiration does not change its expected `WAITING_OTHER_EVENT`
  status.
- Phase 4B completed with six optional edge-case scenarios using only the public HTTP API.
- Runtime validation passes all ten Phase 4 Hurl files and all 20 HTTP requests.

## Phase 5 — README Polish And Final Verification

Goal:

- Make the final solution easy to review and discuss in an interview.
- Document assumptions, decisions, trade-offs, run commands, API examples, and test commands.

Files likely to be created or updated:

- `README.md`
- `TASKS.md`
- `AI_USAGE.md`
- Any final Hurl examples or docs that need small corrections.

Explicit non-goals:

- Do not add new behavior during documentation polish.
- Do not add optional infrastructure.
- Do not broaden scope beyond the MVP.

Validation commands:

```bash
./mvnw clean spotless:apply verify
hurl --test hurl/*.hurl
```

