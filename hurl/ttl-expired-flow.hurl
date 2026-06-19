POST http://localhost:8080/api/events
Content-Type: application/json
{
  "eventId": "phase4a-expired-event-001",
  "traceId": "phase4a-expired-trace-001",
  "eventName": "APPLICATION_RECEIVED",
  "result": "SUCCESS",
  "occurredAt": "2020-01-01T00:00:00Z",
  "nextExpectedEvent": "RULES_EVALUATED",
  "nextEventTtlSeconds": 1,
  "finalEvent": false,
  "metadata": {
    "source": "phase4a-hurl"
  }
}

HTTP 201

GET http://localhost:8080/api/traces/phase4a-expired-trace-001/status

HTTP 200
[Asserts]
jsonpath "$.status" == "TTL_EXPIRED_FOR_EVENT"
jsonpath "$.traceId" == "phase4a-expired-trace-001"
jsonpath "$.nextExpectedEvent" == "RULES_EVALUATED"
jsonpath "$.nextExpectedBefore" == "2020-01-01T00:00:01Z"
