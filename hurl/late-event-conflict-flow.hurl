POST http://localhost:8080/api/events
Content-Type: application/json
{
  "eventId": "phase4b-late-start-event-{{runId}}-001",
  "traceId": "phase4b-late-trace-{{runId}}",
  "eventName": "APPLICATION_RECEIVED",
  "result": "SUCCESS",
  "occurredAt": "2020-01-01T00:00:00Z",
  "nextExpectedEvent": "RULES_EVALUATED",
  "nextEventTtlSeconds": 1,
  "finalEvent": false
}

HTTP 201

POST http://localhost:8080/api/events
Content-Type: application/json
{
  "eventId": "phase4b-late-event-{{runId}}-002",
  "traceId": "phase4b-late-trace-{{runId}}",
  "eventName": "RULES_EVALUATED",
  "result": "SUCCESS",
  "occurredAt": "2020-01-01T00:00:02Z",
  "finalEvent": false
}

HTTP 409
[Asserts]
jsonpath "$.status" == 409
jsonpath "$.title" == "Trace conflict"
