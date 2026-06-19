POST http://localhost:8080/api/events
Content-Type: application/json
{
  "eventId": "phase4b-duplicate-event-001",
  "traceId": "phase4b-duplicate-trace-001",
  "eventName": "APPLICATION_RECEIVED",
  "result": "SUCCESS",
  "occurredAt": "2099-06-15T10:00:00Z",
  "nextExpectedEvent": "RULES_EVALUATED",
  "nextEventTtlSeconds": 120,
  "finalEvent": false,
  "metadata": {
    "source": "phase4b-hurl"
  }
}

HTTP 201
[Asserts]
jsonpath "$.traceId" == "phase4b-duplicate-trace-001"
jsonpath "$.status" == "WAITING_OTHER_EVENT"
jsonpath "$.eventsReceived" == 1

POST http://localhost:8080/api/events
Content-Type: application/json
{
  "eventId": "phase4b-duplicate-event-001",
  "traceId": "phase4b-duplicate-trace-001",
  "eventName": "APPLICATION_RECEIVED",
  "result": "SUCCESS",
  "occurredAt": "2099-06-15T10:00:00Z",
  "nextExpectedEvent": "RULES_EVALUATED",
  "nextEventTtlSeconds": 120,
  "finalEvent": false,
  "metadata": {
    "source": "phase4b-hurl"
  }
}

HTTP 200
[Asserts]
jsonpath "$.traceId" == "phase4b-duplicate-trace-001"
jsonpath "$.status" == "WAITING_OTHER_EVENT"
jsonpath "$.eventsReceived" == 1
