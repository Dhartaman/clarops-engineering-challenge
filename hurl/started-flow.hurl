POST http://localhost:8080/api/events
Content-Type: application/json
{
  "eventId": "phase4a-started-event-001",
  "traceId": "phase4a-started-trace-001",
  "eventName": "APPLICATION_RECEIVED",
  "result": "SUCCESS",
  "occurredAt": "2026-06-15T10:00:00Z",
  "finalEvent": false,
  "metadata": {
    "source": "phase4a-hurl"
  }
}

HTTP 201
[Asserts]
jsonpath "$.status" == "STARTED"
jsonpath "$.traceId" == "phase4a-started-trace-001"
jsonpath "$.lastEventName" == "APPLICATION_RECEIVED"
jsonpath "$.lastEventResult" == "SUCCESS"
jsonpath "$.eventsReceived" == 1

GET http://localhost:8080/api/traces/phase4a-started-trace-001/status

HTTP 200
[Asserts]
jsonpath "$.status" == "STARTED"
