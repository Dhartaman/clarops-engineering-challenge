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

### Prompt 3B — Optional OpenAPI Documentation

Summary:

- Codex was asked to add lightweight OpenAPI documentation for the existing event watchdog REST
  API without changing runtime behavior.
- Codex first verified that `org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.3` resolves and
  is aligned with Spring Boot 4 before adding it.
- Codex added minimal API metadata, documented the existing event watchdog controller responses,
  and added concise schema descriptions to request/response DTO fields.
- Codex hid the legacy `/health` controller from OpenAPI so the generated spec stays focused on
  the Phase 3 event watchdog API while leaving the runtime endpoint unchanged.

Files created:

- `src/main/java/com/clara/challenge/eventwatchdog/api/OpenApiConfiguration.java`.

Files updated:

- `pom.xml`.
- `src/main/java/com/clara/challenge/HealthController.java`.
- `src/main/java/com/clara/challenge/eventwatchdog/api/EventWatchdogController.java`.
- `src/main/java/com/clara/challenge/eventwatchdog/api/dto/EventRequest.java`.
- `src/main/java/com/clara/challenge/eventwatchdog/api/dto/TraceStatusResponse.java`.
- `TASKS.md`.
- `AI_USAGE.md`.

Files deleted:

- None.

Assumptions and decisions:

- Springdoc v3.0.3 is the selected Spring Boot 4-compatible OpenAPI dependency for this phase.
- The public application URLs include the configured `/api` context path, while generated OpenAPI
  path keys are documented as application-relative paths.
- The actual springdoc UI is Swagger UI, not Scalar, so the verified UI path is
  `/api/swagger-ui/index.html`.
- OpenAPI annotations are documentation only and do not alter endpoint paths, JSON contracts,
  status transitions, error behavior, SQL, Docker, Hurl files, or tests.

### Prompt 3B Documentation Patch — OpenAPI URLs And Configuration Restraint

Summary:

- Codex was asked to make a documentation-only Phase 3B patch after OpenAPI verification.
- Codex documented the verified OpenAPI JSON and Swagger UI URLs under the existing `/api` context
  path.
- Codex recorded that no custom springdoc paths are configured because the defaults already expose
  `/api/v3/api-docs`, `/api/swagger-ui.html`, and `/api/swagger-ui/index.html`.
- Codex recorded that `spring.mvc.problemdetails.enabled` was intentionally not added before
  Phase 4 Hurl E2E validation to avoid changing Spring built-in error response behavior.

Files created:

- None.

Files updated:

- `README.md`.
- `TASKS.md`.
- `AI_USAGE.md`.

Files deleted:

- None.

Assumptions and decisions:

- This patch is documentation only.
- Existing OpenAPI Java annotations, dependency declarations, endpoint behavior, SQL, Docker,
  Hurl, and tests are left unchanged.
- The project source currently declares the springdoc starter through `pom.xml`; this patch does
  not modify the dependency version.

### Prompt 4A — Required Hurl E2E Tests

Summary:

- Codex was asked to add only the four required public API Hurl scenarios for `STARTED`,
  `WAITING_OTHER_EVENT`, `COMPLETED`, and `TTL_EXPIRED_FOR_EVENT`.
- Each Hurl file creates its own trace with unique identifiers and then verifies the corresponding
  status lookup without relying on execution order or database queries.
- Codex did not change production Java, dependencies, configuration, SQL, Docker, README, or add
  optional edge-case and correlation-id coverage.

Files created:

- `hurl/started-flow.hurl`.
- `hurl/waiting-other-event-flow.hurl`.
- `hurl/completed-flow.hurl`.
- `hurl/ttl-expired-flow.hurl`.

Files updated:

- `TASKS.md`.
- `AI_USAGE.md`.

Files deleted:

- None.

Assumptions and decisions:

- Hurl targets `http://localhost:8080` and includes the configured `/api` context path in every
  request.
- The completed flow does not assert `completedAt` because it is not required for Phase 4A.
- The expired flow does not assert the POST status body beyond `201`; expiration is asserted only
  after the required lazy GET evaluation.
- The original waiting fixture used a June 15, 2026 deadline that had already expired by the June
  19, 2026 validation date. A follow-up correction replaces it with a deterministic far-future
  deadline so the scenario tests `WAITING_OTHER_EVENT` rather than lazy expiration.

### Prompt 4A Correction — Deterministic Waiting Fixture

Summary:

- Codex was asked to correct only the waiting-flow Hurl fixture after validation showed that its
  fixed deadline had already expired.
- The waiting event timestamp was moved to June 15, 2099 while retaining the 120-second TTL, and
  the expected deadline was updated to June 15, 2099 at 10:02 UTC.
- The failure was a test fixture issue, not a production bug; lazy TTL expiration behaved as
  designed.

Files created:

- None.

Files updated:

- `hurl/waiting-other-event-flow.hurl`.
- `AI_USAGE.md`.
- `TASKS.md`, to record final Phase 4A validation status.

Files deleted:

- None.

Assumptions and decisions:

- A fixed far-future timestamp keeps the waiting scenario deterministic without sleeps, runtime
  clock overrides, or production changes.

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
- Add `org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.3` after verifying dependency
  resolution for Spring Boot 4-era artifacts.
- Use a small `@OpenAPIDefinition` configuration class for API title, description, and version.
- Use controller and DTO annotations for concise OpenAPI descriptions instead of changing runtime
  code.
- Hide the unrelated `/health` endpoint from the event watchdog OpenAPI spec without removing the
  endpoint.
- Rely on springdoc default API docs and Swagger UI paths under the existing `/api` context path
  instead of adding redundant `springdoc.*` path properties.

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
- Using springdoc 2.x, because Phase 3B requested a Spring Boot 4-compatible v3.x dependency.
- Trying multiple unrelated OpenAPI libraries, because OpenAPI is optional and must not block the
  MVP.
- Adding Scalar UI, because the selected springdoc starter provides Swagger UI at
  `/api/swagger-ui/index.html`.
- Changing endpoint paths, request/response contracts, error handling, SQL, Docker files, Hurl
  files, or tests for documentation.
- Adding redundant custom springdoc path properties to `application.yml`, because the defaults
  already produce the verified URLs under `/api`.
- Enabling `spring.mvc.problemdetails.enabled` before Hurl validation, because it could change
  built-in Spring error responses while the project already has explicit API error handling.

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
- OpenAPI/Swagger is included with springdoc v3.0.3 and remains optional documentation, not a
  runtime behavior dependency for the MVP flow rules.

## Manual Corrections

- The architecture was adjusted to use strict `409 Conflict` behavior for unexpected and late
  events.
- Endpoint documentation was clarified to distinguish logical challenge paths from public
  application paths under the `/api` context path.
- OpenAPI was explicitly kept optional and time-boxed so it cannot block the MVP.
- Hurl tests were scoped to public API behavior only, with no internal database assertions.
- Phase 3B documentation was clarified to point reviewers to Swagger UI, not Scalar, and to explain
  why no custom springdoc path or Spring MVC ProblemDetails property was added.

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

### Prompt 3B

- `./mvnw dependency:get -Dartifact=org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.3 -Dtransitive=false`:
  initially blocked by sandbox access to `~/.m2`; rerun with escalation passed and resolved the
  artifact from Maven Central.
- `./mvnw spotless:apply`: passed and formatted the OpenAPI-annotated Java files.
- `./mvnw spotless:check`: passed.
- `./mvnw test`: passed with 20 tests. The existing Spring context test logged the known
  sandbox-blocked PostgreSQL metadata warning before Maven exited successfully. Springdoc
  initialized `/v3/api-docs` and `/swagger-ui.html` in the test context.
- First `./mvnw spring-boot:run`: failed before app startup because sandboxed Maven could not
  access the local Docker socket.
- Escalated `./mvnw spring-boot:run`: passed. The app started on port 8080 with context path
  `/api`.
- `curl http://localhost:8080/api/v3/api-docs`: passed with `200 application/json`.
- `curl http://localhost:8080/api/swagger-ui.html`: passed with `302` redirect to
  `http://localhost:8080/api/swagger-ui/index.html`.
- `curl http://localhost:8080/api/swagger-ui/index.html`: passed with `200 text/html`.
- `curl http://localhost:8080/api/scalar`: returned `500 application/problem+json` because Scalar
  is not provided by the selected springdoc starter; Swagger UI is the actual verified UI path.
- After hiding `/health` from OpenAPI, `./mvnw spotless:apply`, `./mvnw spotless:check`, and
  `./mvnw test` all passed again.
- Final escalated `./mvnw spring-boot:run`: passed. The generated OpenAPI document contained title
  `ClarOps Distributed Event Watchdog API` and paths `/events` and
  `/traces/{traceId}/status`.
- Final `curl http://localhost:8080/api/v3/api-docs`: passed with `200 application/json`.
- Final `curl http://localhost:8080/api/swagger-ui.html`: passed with `302` redirect to
  `http://localhost:8080/api/swagger-ui/index.html`.
- Final `curl http://localhost:8080/api/swagger-ui/index.html`: passed with `200 text/html`.

### Prompt 4A

- `./mvnw spotless:apply`: passed and formatted `TASKS.md` and `AI_USAGE.md`.
- `./mvnw spotless:check`: passed.
- `./mvnw test`: passed with 20 tests. The existing Spring context test logged the known
  sandbox-blocked PostgreSQL metadata warning before Maven exited successfully.
- `hurl --version`: passed; Hurl 8.0.1 is installed.
- First `./mvnw spring-boot:run`: failed because sandboxed Maven could not access the local Docker
  socket.
- Escalated `./mvnw spring-boot:run`: passed. PostgreSQL became healthy and the app started on port
  8080 with context path `/api`.
- First `hurl --test hurl/*.hurl`: could not connect to the host-side app from the sandbox.
- Escalated `hurl --test hurl/*.hurl`: three of four files passed. `started-flow.hurl`,
  `completed-flow.hurl`, and `ttl-expired-flow.hurl` passed with two requests each.
- `waiting-other-event-flow.hurl` reached both endpoints but failed its GET assertion: the actual
  status was `TTL_EXPIRED_FOR_EVENT`, while `WAITING_OTHER_EVENT` was requested. Its fixed deadline
  is June 15, 2026 at 10:02 UTC, four days before the June 19, 2026 validation date.

### Prompt 4A Correction

- `./mvnw spotless:apply`: passed and formatted `AI_USAGE.md`.
- `./mvnw spotless:check`: passed.
- `./mvnw test`: passed with 20 tests. The existing Spring context test logged the known
  sandbox-blocked PostgreSQL metadata warning before Maven exited successfully.
- Escalated `./mvnw spring-boot:run`: passed. PostgreSQL became healthy with a fresh volume and the
  app started on port 8080 with context path `/api`.
- Escalated `hurl --test hurl/*.hurl`: passed all four files and all eight requests with no
  failures. The corrected waiting flow remained `WAITING_OTHER_EVENT`.

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

