POST http://localhost:8080/api/events
Content-Type: application/json
{
  "eventId": "phase4b-unexpected-start-event-{{runId}}-001",
  "traceId": "phase4b-unexpected-trace-{{runId}}",
  "eventName": "APPLICATION_RECEIVED",
  "result": "SUCCESS",
  "occurredAt": "2099-06-15T10:00:00Z",
  "nextExpectedEvent": "RULES_EVALUATED",
  "nextEventTtlSeconds": 300,
  "finalEvent": false
}

HTTP 201

POST http://localhost:8080/api/events
Content-Type: application/json
{
  "eventId": "phase4b-unexpected-event-{{runId}}-002",
  "traceId": "phase4b-unexpected-trace-{{runId}}",
  "eventName": "PAYMENT_CAPTURED",
  "result": "SUCCESS",
  "occurredAt": "2099-06-15T10:01:00Z",
  "finalEvent": false
}

HTTP 409
[Asserts]
jsonpath "$.status" == 409
jsonpath "$.title" == "Trace conflict"
