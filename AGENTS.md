# AGENTS.md

## Project Context

This repository contains the Clara ClarOps Senior Backend Engineer technical challenge:
Distributed Event Watchdog.

The goal is to build a small Spring Boot service that receives distributed events, tracks flow state by `traceId`, and reports whether the flow is `STARTED`, `WAITING_OTHER_EVENT`, `TTL_EXPIRED_FOR_EVENT`, or `COMPLETED`.

The solution should be a clear MVP, not a production-grade event platform.

## Core Endpoints

Implement and validate:

- `POST /events`
- `GET /traces/{traceId}/status`

## Tech Stack

Use the existing project stack:

- Java 21
- Spring Boot
- Spring WebMVC
- Spring Data JPA
- PostgreSQL
- Bean Validation
- JUnit / Spring Boot Test
- Hurl for E2E tests
- Spotless for formatting

Do not introduce new frameworks or infrastructure unless explicitly requested.

## Out of Scope

Do not add:

- Kafka
- SQS
- Pub/Sub
- RabbitMQ
- Flyway
- Liquibase
- A scheduler or background job
- Authentication or authorization
- UI
- Distributed locks
- Multi-tenant support
- Large architecture rewrites

TTL expiration should be evaluated lazily when `GET /traces/{traceId}/status` is called.

## Implementation Principles

Prefer simple, explicit, reviewable code.

Avoid broad refactors. Build on top of the existing project structure.

Keep business logic testable without requiring Spring wiring when possible.

Use clear names for domain concepts:

- Event
- Trace
- Trace status
- Expected event
- TTL expiration
- Final event

Use a modular monolith organized around the `eventwatchdog` business capability:

```text
com.clara.challenge
|-- health
`-- eventwatchdog
    |-- api
    |   `-- dto
    |-- application
    |-- domain
    |-- persistence
    `-- error
```

Keep API contracts, orchestration, business rules, persistence, and error handling separated.
Do not introduce ports/adapters for the MVP. Explicit persistence ports can be introduced later if
storage changes, for example to DynamoDB or an event stream.

## Architecture Decisions

Use PostgreSQL for the MVP because the repository already provides PostgreSQL, Spring Data JPA, and
SQL initialization scripts.

Store event metadata as PostgreSQL `jsonb`.

Do not use DynamoDB in the MVP. Treat DynamoDB as a future option only if throughput, serverless
architecture, or storage access patterns justify it.

Store both immutable event history and current trace state:

- `trace_events` for audit/history;
- `trace_states` for current status lookup.

TTL expiration must be calculated from `occurredAt + nextEventTtlSeconds`.

TTL expiration must be evaluated lazily when `GET /traces/{traceId}/status` is called.

Duplicate `eventId` handling is idempotent:

- do not insert the event again;
- do not mutate trace state again;
- return the current trace status with `200 OK`.

New accepted events should return `201 Created`.

Unknown trace status lookup should return `404 Not Found`.

Unexpected events are strict conflicts. If a trace is waiting for one expected event and a different
event arrives, return `409 Conflict`.

Late expected events are strict conflicts. If the expected event arrives after the deadline, return
`409 Conflict`.

`TTL_EXPIRED_FOR_EVENT` is terminal for the MVP, except duplicate event replay.

`COMPLETED` is terminal for the MVP, except duplicate event replay.

A first event with `finalEvent = true` completes the trace immediately.

`result = ERROR` does not automatically complete the trace. It is the result of the event, not the
global trace status.

`nextExpectedEvent` and `nextEventTtlSeconds` must be provided together.

## Java Usage

Use Java 21 records for immutable DTOs and small domain command/snapshot objects.

Use Lombok where it reduces boilerplate:

- `@RequiredArgsConstructor` for Spring services/controllers;
- `@Slf4j` for services/error handling where logs are useful;
- `@Getter`, `@Setter`, `@NoArgsConstructor` for JPA entities.

Do not use `@Data` on JPA entities.

Do not use MapStruct initially. Manual mapping is simpler for this MVP.

## Concurrency Guidance

Do not use custom threads, `ExecutorService`, `CompletableFuture`, `@Async`, or background workers.

Use transactional boundaries, database constraints, and optimistic locking on trace state.

Production ordering could be handled by a broker partitioned by `traceId`.

## Logging Guidance

Use meaningful, low-noise business logs.

Suggested log levels:

- `INFO`: event accepted, duplicate replay handled, trace completed;
- `WARN`: TTL expired, unexpected event conflict, late event conflict, completed/expired trace
  conflict;
- `ERROR`: unexpected exceptions only.

Do not log full metadata payloads.

Do not log sensitive information.

Do not log repository-level noise.

Optional lightweight correlation id support can be added later:

- accept `X-Correlation-Id`;
- generate one if absent;
- return it in the response;
- put it in MDC for logs.

## OpenAPI Guidance

OpenAPI/Swagger is useful but not core to the first implementation phase.

Add it later only after REST endpoints are stable and only if compatible with Spring Boot 4.0.2.

If dependency compatibility is unclear, keep API examples in `README.md` and Hurl tests instead.

## Database Guidance

Use PostgreSQL with a simple schema that supports:

- storing received events;
- storing current trace state;
- detecting duplicate `eventId`;
- querying trace status by `traceId`;
- keeping metadata as JSON/JSONB if practical.

Use the existing SQL initialization script under:

`docker/init-scripts/db/01-init-schema.sql`

Do not add migration tools.

## Expected Documentation

Keep these files updated:

- `TASKS.md`
- `AI_USAGE.md`
- `README.md`

`TASKS.md` should describe small, scoped implementation tasks.

`AI_USAGE.md` should document prompts, accepted AI suggestions, rejected AI suggestions, and manual corrections.

`README.md` should explain the final solution, assumptions, decisions, trade-offs, how to run the app, how to run tests, and request/response examples.

## Testing Standard

Unit tests should focus on business rules and state transitions:

- first event creates a trace;
- event with next expected event moves to `WAITING_OTHER_EVENT`;
- final event moves to `COMPLETED`;
- TTL expiration returns `TTL_EXPIRED_FOR_EVENT`;
- duplicate event behavior;
- late event behavior, if implemented;
- unexpected event behavior, if implemented.

Use readable test names, preferably:

`shouldExpectedBehavior_WhenCondition`

Prefer Arrange / Act / Assert structure.

Hurl tests should validate the public HTTP API only. Do not assert internal database details unless explicitly justified.

## Validation Commands

Before considering work complete, run:

```bash
./mvnw clean spotless:apply verify
```

When the app is running, validate Hurl scenarios with:

```bash
hurl --test hurl/*.hurl
```

## Codex Working Rules

When making changes:

1. Inspect the existing structure before editing.
2. List files that will be created, modified, or deleted.
3. Keep each phase small and focused.
4. Do not implement unrelated improvements.
5. Do not remove existing setup unless required.
6. Explain any technical decision that affects behavior.
7. Update TASKS.md and AI_USAGE.md when relevant.
8. After each phase, summarize:
   - files created;
   - files modified;
   - tests added;
   - commands run;
   - remaining work.

