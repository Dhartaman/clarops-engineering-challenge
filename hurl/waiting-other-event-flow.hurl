POST http://localhost:8080/api/events
Content-Type: application/json
{
  "eventId": "phase4a-waiting-event-{{runId}}-001",
  "traceId": "phase4a-waiting-trace-{{runId}}",
  "eventName": "APPLICATION_RECEIVED",
  "result": "SUCCESS",
  "occurredAt": "2099-06-15T10:00:00Z",
  "nextExpectedEvent": "RULES_EVALUATED",
  "nextEventTtlSeconds": 120,
  "finalEvent": false,
  "metadata": {
    "source": "phase4a-hurl"
  }
}

HTTP 201
[Asserts]
jsonpath "$.status" == "WAITING_OTHER_EVENT"
jsonpath "$.traceId" == "phase4a-waiting-trace-{{runId}}"
jsonpath "$.nextExpectedEvent" == "RULES_EVALUATED"
jsonpath "$.nextExpectedBefore" == "2099-06-15T10:02:00Z"
jsonpath "$.eventsReceived" == 1

GET http://localhost:8080/api/traces/phase4a-waiting-trace-{{runId}}/status

HTTP 200
[Asserts]
jsonpath "$.status" == "WAITING_OTHER_EVENT"
