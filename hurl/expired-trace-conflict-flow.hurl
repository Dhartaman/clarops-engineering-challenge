POST http://localhost:8080/api/events
Content-Type: application/json
{
  "eventId": "phase4b-expired-start-event-{{runId}}-001",
  "traceId": "phase4b-expired-conflict-trace-{{runId}}",
  "eventName": "APPLICATION_RECEIVED",
  "result": "SUCCESS",
  "occurredAt": "2020-01-01T00:00:00Z",
  "nextExpectedEvent": "RULES_EVALUATED",
  "nextEventTtlSeconds": 1,
  "finalEvent": false
}

HTTP 201

GET http://localhost:8080/api/traces/phase4b-expired-conflict-trace-{{runId}}/status

HTTP 200
[Asserts]
jsonpath "$.status" == "TTL_EXPIRED_FOR_EVENT"

POST http://localhost:8080/api/events
Content-Type: application/json
{
  "eventId": "phase4b-expired-event-{{runId}}-002",
  "traceId": "phase4b-expired-conflict-trace-{{runId}}",
  "eventName": "RULES_EVALUATED",
  "result": "SUCCESS",
  "occurredAt": "2020-01-01T00:00:02Z",
  "finalEvent": false
}

HTTP 409
[Asserts]
jsonpath "$.status" == 409
jsonpath "$.title" == "Trace conflict"
