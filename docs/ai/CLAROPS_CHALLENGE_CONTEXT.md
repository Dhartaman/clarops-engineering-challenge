# ClarOps Engineering Challenge Context

## Purpose

This file is the working context for implementing Clara's **Distributed Event Watchdog Challenge** with Codex or another AI coding assistant.

The goal is not to over-engineer a production-grade event platform. The goal is to deliver a small, functional, well-reasoned MVP that shows senior backend judgment, clear task decomposition, meaningful validation, and responsible AI-assisted development.

---

## Source Materials

Use these files as the source of truth:

- `README.md`: challenge requirements, expected behavior, deliverables, evaluation criteria.
- `SETUP.md`: local setup, Docker/PostgreSQL bootstrap, formatting, and verification commands.
- `pom.xml`: Java/Spring Boot stack, dependencies, test tooling, and formatting plugins.
- `clara_clarops_senior_backend_engineer_ai_ready.md`: JD-aligned expectations for the ClarOps Senior Backend Engineer role.

---

## Challenge Summary

Build a Spring Boot service that receives distributed events, tracks flow state by `traceId`, and reports the current trace status.

Required endpoints:

```http
POST /events
GET /traces/{traceId}/status
```

The service must support these statuses:

- `STARTED`
- `WAITING_OTHER_EVENT`
- `TTL_EXPIRED_FOR_EVENT`
- `COMPLETED`

Core behavior:

1. Receive events associated with a `traceId`.
2. Persist received events.
3. Maintain current state for each trace.
4. Allow an event to define `nextExpectedEvent` and `nextEventTtlSeconds`.
5. Allow an event to mark the trace as completed with `finalEvent = true`.
6. Detect TTL expiration lazily when `GET /traces/{traceId}/status` is called.

---

## Hard Scope Boundaries

Do **not** add these unless explicitly requested later:

- Kafka, SQS, Pub/Sub, RabbitMQ, or any external broker.
- Scheduler or background expiration job.
- Flyway or Liquibase.
- Authentication or authorization.
- UI.
- Distributed locks.
- Multi-tenant dynamic flow definitions.
- Large observability stack.
- Docker changes unless required by the existing setup.

The challenge explicitly allows a small MVP. Avoid building an iron cathedral when a sharp pocketknife is enough.

---

## Existing Stack

From `pom.xml`:

- Java 21.
- Spring Boot 4.0.2.
- Spring WebMVC.
- Spring Validation.
- Spring Data JPA.
- PostgreSQL runtime driver.
- Spring Actuator.
- Spring Boot Docker Compose support.
- MapStruct and Lombok are available, but optional.
- Spring Boot starter test.
- Spotless formatting for Java, SQL, Markdown, and `pom.xml`.

Useful commands from `SETUP.md`:

```bash
cp docker/example.env docker/.env
./mvnw spring-boot:run
curl http://localhost:8080/api/health
./mvnw clean spotless:apply verify
```

DDL must be added to:

```text
docker/init-scripts/db/01-init-schema.sql
```

---

## ClarOps JD Alignment

The implementation should deliberately signal these ClarOps expectations:

- Senior-level ownership through documented assumptions and trade-offs.
- Strong Java/Spring Boot fundamentals.
- Clean integration boundaries and event-driven reasoning, even without adding a real broker.
- Clear communication in `README.md`, `TASKS.md`, and `AI_USAGE.md`.
- Responsible AI usage, including explicit prompts, accepted suggestions, rejected suggestions, and manual corrections.
- Practical internal-platform mindset: APIs and status responses should be useful for humans, operators, and future AI agents.

---

## Proposed Implementation Philosophy

Prioritize this order:

1. Requirement analysis and task breakdown.
2. Simple data model with both event history and current trace state.
3. Core state transition logic isolated from Spring wiring.
4. REST API validation and consistent errors.
5. Meaningful unit tests for business rules.
6. Hurl E2E tests for the public HTTP contract.
7. README, TASKS, and AI_USAGE polished enough for interview discussion.

Keep the core logic explainable in a technical interview.

---

## Proposed Data Model

Use the existing `clarops_challenge_schema` schema unless the current project structure suggests another convention.

### Table: `trace_events`

Stores immutable event history.

Suggested columns:

- `event_id varchar(100) primary key`
- `trace_id varchar(100) not null`
- `event_name varchar(150) not null`
- `result varchar(20) not null check (result in ('SUCCESS', 'ERROR'))`
- `occurred_at timestamptz not null`
- `received_at timestamptz not null default now()`
- `next_expected_event varchar(150)`
- `next_event_ttl_seconds integer check (next_event_ttl_seconds is null or next_event_ttl_seconds > 0)`
- `final_event boolean not null default false`
- `metadata jsonb`

Suggested indexes:

- `idx_trace_events_trace_id_occurred_at` on `(trace_id, occurred_at)`.
- `idx_trace_events_trace_id_received_at` on `(trace_id, received_at)`.

### Table: `trace_states`

Stores current derived state per trace.

Suggested columns:

- `trace_id varchar(100) primary key`
- `status varchar(40) not null`
- `last_event_id varchar(100) not null references trace_events(event_id)`
- `last_event_name varchar(150) not null`
- `last_event_result varchar(20) not null`
- `next_expected_event varchar(150)`
- `waiting_since timestamptz`
- `next_expected_before timestamptz`
- `events_received integer not null`
- `completed_at timestamptz`
- `created_at timestamptz not null default now()`
- `updated_at timestamptz not null default now()`
- `version bigint` if using optimistic locking with JPA.

Why both tables:

- `trace_events` preserves auditability and helps explain what happened.
- `trace_states` makes status retrieval simple and efficient.
- This is appropriate for an internal operational watchdog where both current state and history matter.

---

## Proposed Business Decisions

Document these in the final `README.md`.

### TTL calculation

Calculate `nextExpectedBefore` from the event's `occurredAt` plus `nextEventTtlSeconds`.

Reasoning:

- The event contract includes `occurredAt`, which represents the business event time.
- It allows deterministic tests.
- It avoids changing behavior depending on local machine receive time.

Trade-off:

- In a real distributed system, clock skew and late delivery would require stronger event-time vs processing-time rules.

### Duplicate `eventId`

Treat duplicate `eventId` as idempotent.

Suggested behavior:

- Do not insert a second event.
- Do not mutate trace state again.
- Return the current trace status with HTTP `200 OK`.

Reasoning:

- Distributed systems commonly retry event delivery.
- Idempotency is safer than double-counting events.

### Unexpected event name while waiting

Persist the event, update `lastEventName`, and keep the trace in a valid state based on the new event payload.

Suggested MVP rule:

- If a trace is waiting for event `X` and receives event `Y`, accept `Y` as the latest observed event but include a documented assumption that this MVP does not reject out-of-order or unexpected events.
- If `Y` defines a new expectation, the trace waits for that new expectation.
- If `Y` is final, the trace becomes `COMPLETED`.
- If `Y` has no next expectation and is not final, the trace becomes `STARTED`.

Alternative stricter rule:

- Reject unexpected event with `409 Conflict`.

Recommended for this MVP:

- Accept and document. It keeps the service tolerant and simple.

### Late expected event after TTL expired

TTL expiration is lazily evaluated at status query time. If a late event arrives after the deadline but before status is queried, use the event ingestion path to advance the trace based on the received event.

Document this limitation clearly:

- This MVP does not persist an expiration event automatically.
- A production version may record a terminal expired state or emit an alert once the deadline is crossed.

### Completed traces

Once a trace is `COMPLETED`, do not allow additional events to mutate state.

Suggested behavior:

- Persisting additional events after completion is optional; for simplicity, reject with `409 Conflict` or ignore idempotently if duplicate.
- Recommended MVP: return `409 Conflict` for a new event on a completed trace, except duplicate `eventId`, which remains idempotent.

Reasoning:

- A completed trace should not later become waiting or expired.

### First event is final

Mark trace as `COMPLETED` immediately.

### `result = ERROR`

Treat `result` as event outcome, not overall trace status.

Suggested behavior:

- An `ERROR` event may still define `nextExpectedEvent`.
- An `ERROR` event may be final and complete the flow.

### Metadata storage

Store metadata as PostgreSQL `jsonb`.

Reasoning:

- Flexible operational metadata fits JSON.
- It avoids premature schema design.
- It remains queryable later if needed.

### Unknown `traceId`

Return `404 Not Found` with a clear Problem Details style response if the project already uses Spring error conventions. Otherwise use a simple JSON error.

---

## Suggested API Contracts

### POST `/events`

Request:

```json
{
  "eventId": "evt-001",
  "traceId": "trace-123",
  "eventName": "APPLICATION_RECEIVED",
  "result": "SUCCESS",
  "occurredAt": "2026-06-15T10:00:00Z",
  "nextExpectedEvent": "RULES_EVALUATED",
  "nextEventTtlSeconds": 120,
  "finalEvent": false,
  "metadata": {
    "country": "MX",
    "entityId": "company-123"
  }
}
```

Response suggestion:

```json
{
  "traceId": "trace-123",
  "status": "WAITING_OTHER_EVENT",
  "lastEventName": "APPLICATION_RECEIVED",
  "lastEventResult": "SUCCESS",
  "nextExpectedEvent": "RULES_EVALUATED",
  "nextExpectedBefore": "2026-06-15T10:02:00Z",
  "eventsReceived": 1
}
```

### GET `/traces/{traceId}/status`

Return the current state, evaluating TTL lazily.

Response fields:

- `traceId`
- `status`
- `lastEventName`
- `lastEventResult`
- `nextExpectedEvent`
- `nextExpectedBefore`
- `eventsReceived`
- `completedAt` when available

---

## Suggested Package Shape

Adapt package names to the existing repo.

```text
com.clara.clarops.challenge
  eventwatchdog
    api
      EventController
      TraceStatusController
      dto
        EventRequest
        TraceStatusResponse
    domain
      EventResult
      TraceStatus
      EventWatchdogService
      TraceStateTransitionService
    persistence
      TraceEventEntity
      TraceStateEntity
      TraceEventRepository
      TraceStateRepository
    error
      ApiExceptionHandler
      TraceNotFoundException
      CompletedTraceConflictException
```

Keep `TraceStateTransitionService` easy to unit test without Spring context.

---

## Required Deliverables Checklist

- [ ] Functional `POST /events`.
- [ ] Functional `GET /traces/{traceId}/status`.
- [ ] Database schema DDL inside `docker/init-scripts/db/01-init-schema.sql`.
- [ ] Updated `README.md` with understanding, assumptions, decisions, trade-offs, run instructions, Hurl instructions, examples.
- [ ] `TASKS.md` with scoped tasks used during development.
- [ ] `AI_USAGE.md` with tools, prompts, accepted/rejected suggestions, manual corrections.
- [ ] Unit tests for core state transition logic.
- [ ] Hurl E2E tests for STARTED, WAITING_OTHER_EVENT, COMPLETED, TTL_EXPIRED_FOR_EVENT.
- [ ] Optional Hurl tests for duplicate events, completed trace conflict, and unknown trace.
- [ ] `./mvnw clean spotless:apply verify` passes.
- [ ] `hurl --test hurl/*.hurl` passes while app is running.

---

## Suggested Development Phases

### Phase 0 — Baseline inspection

Goal: inspect current project structure, existing packages, health endpoint, application config, and SQL init script.

Do not implement business logic yet.

Output:

- Summary of current structure.
- Proposed file list to create/update.
- Potential conflicts or conventions to follow.

Validation:

```bash
./mvnw test
./mvnw spotless:check
```

### Phase 1 — Data model and DDL

Goal: define persistence schema and JPA entities/repositories.

Scope:

- Update `docker/init-scripts/db/01-init-schema.sql`.
- Add entities for `trace_events` and `trace_states`.
- Add repositories.
- Do not add controllers yet.

Validation:

```bash
./mvnw test
./mvnw spotless:apply spotless:check
```

### Phase 2 — Domain state transition logic

Goal: implement state transition service independent from HTTP.

Scope:

- Enums: `EventResult`, `TraceStatus`.
- Domain/service methods for first event, waiting state, final event, duplicate event, completed trace conflict, and TTL lazy evaluation.
- Unit tests for pure business behavior.

Validation:

```bash
./mvnw test
```

### Phase 3 — REST API and validation

Goal: implement public endpoints.

Scope:

- `POST /events`.
- `GET /traces/{traceId}/status`.
- DTO validation.
- Consistent error handling.

Validation:

```bash
./mvnw test
./mvnw spring-boot:run
```

### Phase 4 — Hurl E2E tests

Goal: validate public API behavior externally.

Scope:

- Add `hurl/*.hurl` scenarios.
- Required coverage: STARTED, WAITING_OTHER_EVENT, COMPLETED, TTL_EXPIRED_FOR_EVENT.
- Optional: duplicate event, completed trace conflict, unknown trace.

Validation:

```bash
hurl --test hurl/*.hurl
```

### Phase 5 — Documentation polish

Goal: make the repo reviewable and interview-ready.

Scope:

- Update `README.md`.
- Add `TASKS.md`.
- Add `AI_USAGE.md` with the actual prompts used.
- Include run commands, examples, trade-offs, accepted/rejected AI suggestions.

Validation:

```bash
./mvnw clean spotless:apply verify
```

---

## Testing Standard

Use this standard for unit tests:

- Test method names: `shouldExpectedBehavior_WhenCondition`.
- Each test validates one business rule.
- Use Arrange / Act / Assert structure.
- Prefer plain unit tests over Spring context unless testing wiring.
- Focus on:
  - first event creates trace state;
  - event with next expected event moves to `WAITING_OTHER_EVENT`;
  - final event moves to `COMPLETED`;
  - TTL expiration returns `TTL_EXPIRED_FOR_EVENT`;
  - duplicate event behavior;
  - completed trace conflict;
  - late event behavior if implemented.
- Do not add behavior not documented in assumptions.

---

## Hurl Standard

Hurl tests must validate only public HTTP behavior.

Required files:

```text
hurl/started-flow.hurl
hurl/waiting-other-event-flow.hurl
hurl/completed-flow.hurl
hurl/ttl-expired-flow.hurl
```

Recommended optional files:

```text
hurl/duplicate-event-flow.hurl
hurl/completed-trace-conflict-flow.hurl
hurl/unknown-trace-flow.hurl
```

Avoid direct database assertions unless documented and justified.

---

## AI Usage Standard

Every Codex prompt used should be copied into `AI_USAGE.md`.

For each phase, record:

- Prompt used.
- Files Codex created, updated, or deleted.
- Suggestions accepted.
- Suggestions rejected.
- Manual corrections.
- Validation command run and result.

Avoid prompts like:

```text
Implement the whole challenge.
```

Prefer small, bounded prompts with explicit constraints and validation.

---

## Initial Codex Safety Instructions

Use these instructions in each Codex prompt:

- Do not implement outside the requested phase.
- Do not add new dependencies unless explicitly asked.
- Do not introduce Kafka, schedulers, migration tools, auth, or Docker changes.
- Follow existing project conventions.
- Keep changes small and reviewable.
- At the end, print a summary with:
  - files created;
  - files updated;
  - files deleted;
  - tests added;
  - commands run;
  - any assumptions made.

---

## Interview Defense Notes

Be ready to explain:

- Why event history and current state are stored separately.
- Why TTL uses `occurredAt` rather than receive time.
- Why duplicate events are idempotent.
- Why TTL expiration is lazy rather than scheduler-based.
- Why metadata is stored as JSONB.
- Why Hurl validates public API only.
- What was AI-generated and what was manually reviewed.
- What would change in production: event broker, durable expiration handling, alerting, distributed locking, trace versioning, richer observability, and possibly out-of-order event policies.

