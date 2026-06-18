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

### Prompt 2 — Domain State Transition Logic And Unit Tests

Summary:

- Codex was asked to implement pure domain state transition logic under `eventwatchdog.domain`.
- Codex added immutable domain records for incoming events and trace state snapshots.
- Codex added explicit conflict results for expected domain conflicts instead of throwing
  exceptions.
- Codex added plain JUnit tests for first-event, waiting, final, error-result, conflict, terminal,
  and lazy TTL expiration behavior.
- Codex did not add REST controllers, DTOs, application services, persistence adapters, DDL,
  dependencies, Hurl tests, Docker changes, OpenAPI, or external infrastructure.

Files created:

- `src/main/java/com/clara/challenge/eventwatchdog/domain/IncomingEvent.java`.
- `src/main/java/com/clara/challenge/eventwatchdog/domain/TraceStateSnapshot.java`.
- `src/main/java/com/clara/challenge/eventwatchdog/domain/TraceConflictReason.java`.
- `src/main/java/com/clara/challenge/eventwatchdog/domain/TraceTransitionResult.java`.
- `src/main/java/com/clara/challenge/eventwatchdog/domain/TraceStateTransitionService.java`.
- `src/test/java/com/clara/challenge/eventwatchdog/domain/TraceStateTransitionServiceTest.java`.

Files updated:

- `AI_USAGE.md`.

Files deleted:

- None.

Assumptions and decisions:

- Duplicate detection is deferred to application/persistence orchestration because pure domain
  logic does not know which event IDs already exist.
- Expected domain conflicts are represented as `TraceTransitionResult.Conflict` rather than
  exceptions.
- Lazy expiration is pure and idempotent: it returns an expired snapshot only when a waiting trace
  is evaluated after its deadline.
- Waiting fields are preserved on expired snapshots for diagnostic context.

### Prompt 3 — REST API, Application Orchestration, And Error Handling

Summary:

- Codex was asked to implement Phase 3 without adding Hurl tests, OpenAPI, dependencies, DDL,
  Docker changes, README changes, schedulers, brokers, auth, UI, DynamoDB, or external
  infrastructure.
- Codex added API request and response DTOs, a REST controller, application orchestration,
  duplicate replay handling, lazy TTL materialization, consistent `ProblemDetail` error handling,
  and low-noise business logs.
- Codex kept state transition rules in `eventwatchdog.domain` and duplicate detection in
  application/persistence orchestration.
- Codex added focused application service tests with a fixed `Clock`.

Files created:

- `src/main/java/com/clara/challenge/eventwatchdog/api/EventWatchdogController.java`.
- `src/main/java/com/clara/challenge/eventwatchdog/api/dto/EventRequest.java`.
- `src/main/java/com/clara/challenge/eventwatchdog/api/dto/TraceStatusResponse.java`.
- `src/main/java/com/clara/challenge/eventwatchdog/application/EventIngestionOutcome.java`.
- `src/main/java/com/clara/challenge/eventwatchdog/application/EventWatchdogConfiguration.java`.
- `src/main/java/com/clara/challenge/eventwatchdog/application/EventWatchdogService.java`.
- `src/main/java/com/clara/challenge/eventwatchdog/error/ApiExceptionHandler.java`.
- `src/main/java/com/clara/challenge/eventwatchdog/error/BusinessConflictException.java`.
- `src/main/java/com/clara/challenge/eventwatchdog/error/TraceNotFoundException.java`.
- `src/test/java/com/clara/challenge/eventwatchdog/application/EventWatchdogServiceTest.java`.

Files updated:

- `AI_USAGE.md`.

Files deleted:

- None.

Assumptions and decisions:

- Controller mappings use `/events` and `/traces/{traceId}/status`; the configured `/api` context
  path exposes them publicly as `/api/events` and `/api/traces/{traceId}/status`.
- Duplicate replay returns the persisted trace state without mutating event history or trace state.
- Duplicate `eventId` with a different `traceId` is represented as a business conflict with code
  `DUPLICATE_EVENT_TRACE_MISMATCH`.
- New events save `trace_events` before `trace_states` so the existing `last_event_id` foreign key
  is satisfied in the transaction.
- Application tests use lightweight in-memory repository proxies instead of Mockito because Mockito
  inline agent attachment is not reliable in this execution environment.
- Lazy TTL expiration is materialized by updating only the current trace state when status lookup
  observes an expired waiting trace.

### Prompt 3 Test Refactor — Mockito Repository Mocks

Summary:

- Codex was asked to refactor `EventWatchdogServiceTest` from custom dynamic proxy repository
  fakes to conventional Mockito mocks.
- Codex added a Mockito test resource that forces the subclass/classic mock maker so repository
  interface mocks do not require Mockito inline self-attach.
- Codex also marked the repository `@Mock` fields with Mockito's subclass mock maker constant for
  clarity.
- Codex kept `TraceStateTransitionService` real and did not change production code.

Files created:

- `src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker`.

Files updated:

- `src/test/java/com/clara/challenge/eventwatchdog/application/EventWatchdogServiceTest.java`.
- `AI_USAGE.md`.

Files deleted:

- None.

Assumptions and decisions:

- The tests only mock repository interfaces, so Mockito's subclass mock maker is sufficient.
- The test coverage and fixed-clock behavior from Phase 3 are preserved.

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
- Keep Phase 2 domain logic free of Spring dependencies.
- Use explicit domain transition results for accepted and conflict outcomes.
- Use a Spring application service as the transaction and persistence orchestration boundary.
- Use a fixed `Clock` in application tests to keep TTL materialization deterministic.
- Force Mockito's subclass mock maker for application tests that only need repository interface
  mocks.

## Rejected Suggestions

- Using DynamoDB in the MVP, because PostgreSQL/JPA/DDL are already provided and DynamoDB would add
  unnecessary setup and review complexity.
- Introducing ports/adapters in the MVP, because the package boundaries are enough for this small
  challenge and explicit persistence ports can be introduced later.
- Custom threads or schedulers, because lazy TTL evaluation is enough for the MVP.
- Kafka/SQS/Pub/Sub/RabbitMQ or external event infrastructure, because the challenge does not
  require it.
- Using MapStruct initially, because manual mapping is simpler for the small number of DTOs.
- Throwing exceptions for expected Phase 2 domain conflicts, because explicit result objects are
  easier to unit test and map in a later API layer.
- Adding controller or Hurl end-to-end tests in Phase 3, because the requested required coverage is
  focused on application orchestration and Hurl belongs to Phase 4.

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

### Prompt 2

- `./mvnw test`: passed in the first compile/test check with 13 tests. The new plain domain tests
  ran without Spring context, and the existing Spring context test logged the known sandbox-blocked
  PostgreSQL metadata warning before Maven exited successfully.
- `./mvnw spotless:apply`: passed after rerun with escalation for Maven `~/.m2` access and
  formatted `AI_USAGE.md`.
- `./mvnw spotless:check`: passed after rerun with escalation for Maven `~/.m2` access.
- `./mvnw test`: passed with 13 tests. The existing Spring context test logged the known
  sandbox-blocked PostgreSQL metadata warning before Maven exited successfully.

### Prompt 3

- `./mvnw test`: initial compile/test run failed because Mockito inline mock-maker could not
  self-attach in the execution environment.
- Codex replaced Mockito-based application tests with lightweight in-memory repository proxies.
- `./mvnw test`: passed after the test rewrite with 20 tests. The existing Spring context test
  logged the known sandbox-blocked PostgreSQL metadata warning before Maven exited successfully.
- `./mvnw spotless:apply`: passed after rerun with escalation for Maven `~/.m2` access and
  formatted the new Java files plus `AI_USAGE.md`.
- `./mvnw spotless:check`: passed after rerun with escalation for Maven `~/.m2` access.
- `./mvnw test`: passed with 20 tests. The existing Spring context test logged the known
  sandbox-blocked PostgreSQL metadata warning before Maven exited successfully.

### Prompt 3 Test Refactor

- `./mvnw spotless:apply`: passed and formatted `AI_USAGE.md`.
- `./mvnw spotless:check`: passed.
- `./mvnw test`: passed with 20 tests. The refactored Mockito application tests passed, and the
  existing Spring context test logged the known sandbox-blocked PostgreSQL metadata warning before
  Maven exited successfully.
- Mockito 5.20 still printed its inline self-attach warning during `MockitoExtension`
  initialization despite the subclass mock-maker resource and explicit subclass repository mocks.
  No production code, dependency, or build configuration change was made to suppress that warning.

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

