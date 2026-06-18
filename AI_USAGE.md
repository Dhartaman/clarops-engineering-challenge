# AI Usage

This file documents how AI assistance was used for the Clara ClarOps Distributed Event Watchdog
Challenge. The goal is to keep AI contributions explicit, reviewable, and easy to discuss.

## Tools Used

- ChatGPT: used to help frame prompts, clarify decisions, and review trade-offs.
- Codex: used to inspect the repository, create documentation, and prepare implementation plans.

## Prompt Log

### Prompt 0 — Baseline Repository Inspection

Summary:

- Codex inspected the repository structure, existing health feature, Spring configuration, current
  tests, SQL initialization script, Maven setup, and Spotless configuration.
- Codex summarized existing package conventions and validation behavior.
- Codex did not change files.

Files created:

- None.

Files updated:

- None.

Files deleted:

- None.

### Prompt 0B — Architecture Decisions

Summary:

- Codex created `docs/ARCHITECTURE.md`.
- Codex updated `AGENTS.md` to align future work with the selected architecture and behavior.
- Codex formatted `docs/ai/CLAROPS_CHALLENGE_CONTEXT.md` through Spotless.
- A follow-up architecture patch clarified `/api` endpoint paths, lazy expiration
  materialization, Hurl scope, and optional OpenAPI timing.

Files created:

- `docs/ARCHITECTURE.md`.

Files updated:

- `AGENTS.md`.
- `docs/ai/CLAROPS_CHALLENGE_CONTEXT.md`, formatting only.
- `docs/ARCHITECTURE.md`, for follow-up clarifications.

Files deleted:

- None.

### Prompt 1A — Documentation Planning

Summary:

- Codex was asked to prepare implementation planning documentation before any business logic.
- Scope is limited to creating or updating `TASKS.md` and `AI_USAGE.md`.
- Codex was instructed not to create controllers, entities, repositories, DDL, dependencies, Docker
  changes, README changes, or implementation code.

Files created:

- None. `TASKS.md` and `AI_USAGE.md` already existed as empty files.

Files updated:

- `TASKS.md`.
- `AI_USAGE.md`.

Files deleted:

- None.

### Prompt 1B — Persistence Model

Summary:

- Codex was asked to add the persistence model only.
- Scope included DDL, minimal domain enums, JPA entities, Spring Data repositories, and this
  `AI_USAGE.md` update.
- Codex was instructed not to add REST controllers, DTOs, business transition services, validation
  or error handling, Hurl tests, README updates, dependencies, Docker changes, or infrastructure.

Files created:

- `src/main/java/com/clara/challenge/eventwatchdog/domain/EventResult.java`.
- `src/main/java/com/clara/challenge/eventwatchdog/domain/TraceStatus.java`.
- `src/main/java/com/clara/challenge/eventwatchdog/persistence/TraceEventEntity.java`.
- `src/main/java/com/clara/challenge/eventwatchdog/persistence/TraceStateEntity.java`.
- `src/main/java/com/clara/challenge/eventwatchdog/persistence/TraceEventRepository.java`.
- `src/main/java/com/clara/challenge/eventwatchdog/persistence/TraceStateRepository.java`.

Files updated:

- `docker/init-scripts/db/01-init-schema.sql`.
- `AI_USAGE.md`.

Files deleted:

- None.

Assumptions and decisions:

- JSONB metadata uses Hibernate built-in JSON mapping with `@JdbcTypeCode(SqlTypes.JSON)` and
  `Map<String, Object>` so no dependency is needed.
- Trace state stores the foreign-key value `lastEventId` directly instead of a JPA association to
  keep the persistence model simple for the MVP.
- Enums are stored as strings with `@Enumerated(EnumType.STRING)`.
- Optimistic locking uses `@Version` on `TraceStateEntity.version`.

### Prompt 1B SQL Patch — Trace State Waiting Constraints

Summary:

- Codex was asked to refine only the `trace_states` waiting-field SQL constraints.
- Codex kept the existing schema, health table, trace tables, Java code, Docker setup, and business
  behavior unchanged.
- The waiting-state constraint now requires `next_expected_event`, `waiting_since`, and
  `next_expected_before` when status is `WAITING_OTHER_EVENT`.
- A structural deadline-order constraint was added so `next_expected_before` must be after
  `waiting_since` when both are present.

Files created:

- None.

Files updated:

- `docker/init-scripts/db/01-init-schema.sql`.
- `AI_USAGE.md`.

Files deleted:

- None.

Assumptions and decisions:

- Business flow rules remain in domain/application code, not SQL constraints.
- The SQL patch is a structural integrity guard only and does not implement Phase 2 behavior.

### Prompt 1B SQL Correction — Trace Event Result Column

Summary:

- Codex was asked to rename the `trace_events` SQL column from quoted `"result"` to
  `event_result`.
- Codex updated the trace event result check constraint name and expression.
- Codex updated `TraceEventEntity` to map the `result` Java field to the `event_result` column.
- No business logic or Phase 2 work was changed.

Files created:

- None.

Files updated:

- `docker/init-scripts/db/01-init-schema.sql`.
- `src/main/java/com/clara/challenge/eventwatchdog/persistence/TraceEventEntity.java`.
- `AI_USAGE.md`.

Files deleted:

- None.

Assumptions and decisions:

- The API/domain concept can remain named `result`; only the database column was renamed to avoid a
  quoted SQL identifier.

## Accepted Suggestions

- Keep the existing Java 21 / Spring Boot / PostgreSQL stack.
- Use a modular monolith around the `eventwatchdog` business capability.
- Use package boundaries: `api`, `application`, `domain`, `persistence`, `error`.
- Keep PostgreSQL for the MVP and store metadata as `jsonb`.
- Use event history plus current trace state.
- Calculate TTL from `occurredAt + nextEventTtlSeconds`.
- Evaluate TTL lazily on status lookup.
- Treat duplicate `eventId` as idempotent.
- Use records for DTOs and small immutable domain objects.
- Use Lombok where it reduces boilerplate.
- Use meaningful, low-noise logs.

## Rejected Suggestions

- Using DynamoDB in the MVP, because PostgreSQL/JPA/DDL are already provided and DynamoDB would add
  unnecessary setup and review complexity.
- Introducing ports/adapters in the MVP, because the package boundaries are enough for this small
  challenge and explicit persistence ports can be introduced later.
- Custom threads or schedulers, because lazy TTL evaluation is enough for the MVP.
- Kafka/SQS/Pub/Sub/RabbitMQ or external event infrastructure, because the challenge does not
  require it.
- Using MapStruct initially, because manual mapping is simpler for the small number of DTOs.

## Manual Decisions

- Duplicate event replay returns `200 OK` with current trace status.
- New accepted events return `201 Created`.
- Unexpected event while waiting returns `409 Conflict`.
- Late expected event after the deadline returns `409 Conflict`.
- `TTL_EXPIRED_FOR_EVENT` is terminal for MVP except duplicate replay.
- `COMPLETED` is terminal for MVP except duplicate replay.
- First event with `finalEvent = true` completes the trace immediately.
- `result = ERROR` does not automatically complete the trace.
- `nextExpectedEvent` and `nextEventTtlSeconds` must be provided together.
- Unknown trace status lookup returns `404 Not Found`.
- OpenAPI/Swagger may be added later only if compatible with Spring Boot 4.0.2.

## Manual Corrections

- The architecture was adjusted to use strict `409 Conflict` behavior for unexpected and late
  events.
- Endpoint documentation was clarified to distinguish logical challenge paths from public
  application paths under the `/api` context path.
- OpenAPI was explicitly kept optional and time-boxed so it cannot block the MVP.
- Hurl tests were scoped to public API behavior only, with no internal database assertions.

## Validation Log

### Prompt 0

- `./mvnw test`: passed.
- `./mvnw spotless:check`: initially blocked by sandbox access to `~/.m2`; rerun with escalation
  identified Markdown formatting issues that were pre-existing at the time.

### Prompt 0B

- `./mvnw spotless:apply`: passed after rerun with escalation for Maven `~/.m2` access.
- `./mvnw spotless:check`: passed after rerun with escalation for Maven `~/.m2` access.
- `./mvnw test`: passed.

### Prompt 1A

- `./mvnw spotless:apply`: initially blocked by sandbox access to `~/.m2`; rerun with escalation
  passed and formatted `TASKS.md` and `AI_USAGE.md`.
- `./mvnw spotless:check`: initially blocked by sandbox access to `~/.m2`; rerun with escalation
  passed.
- `./mvnw test`: passed.

### Prompt 1B

- `./mvnw spotless:apply`: initially blocked by sandbox access to `~/.m2`; rerun with escalation
  passed and formatted `docker/init-scripts/db/01-init-schema.sql` and `AI_USAGE.md`.
- `./mvnw spotless:check`: initially blocked by sandbox access to `~/.m2`; rerun with escalation
  passed.
- `./mvnw test`: passed. The test run compiled the Hibernate JSONB mapping successfully and logged
  the known sandbox-blocked PostgreSQL metadata warning before Maven exited successfully.

### Prompt 1B SQL Patch

- `./mvnw spotless:apply`: initially blocked by sandbox access to `~/.m2`; rerun with escalation
  passed and formatted `docker/init-scripts/db/01-init-schema.sql` and `AI_USAGE.md`.
- `./mvnw spotless:check`: initially blocked by sandbox access to `~/.m2`; rerun with escalation
  passed.
- `./mvnw test`: passed. The test run logged the known sandbox-blocked PostgreSQL metadata warning
  before Maven exited successfully.

## Notes For Interview Discussion

- The MVP intentionally favors clarity over platform breadth.
- Event history and current trace state are stored separately to support auditability and fast
  status lookup.
- TTL expiration is lazy because the challenge does not require a scheduler or alerting system.
- Duplicate event replay is idempotent because distributed senders commonly retry.
- Strict conflict behavior for unexpected and late events keeps the MVP predictable.
- PostgreSQL `jsonb` gives flexible metadata storage without adding a second database.
- OpenAPI is useful but optional; README examples and Hurl tests are enough if compatibility or time
  becomes a risk.

