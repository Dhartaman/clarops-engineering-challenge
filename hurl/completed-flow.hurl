POST http://localhost:8080/api/events
Content-Type: application/json
{
  "eventId": "phase4a-completed-event-{{runId}}-001",
  "traceId": "phase4a-completed-trace-{{runId}}",
  "eventName": "FLOW_COMPLETED",
  "result": "SUCCESS",
  "occurredAt": "2026-06-15T10:03:00Z",
  "finalEvent": true,
  "metadata": {
    "source": "phase4a-hurl"
  }
}

HTTP 201
[Asserts]
jsonpath "$.status" == "COMPLETED"
jsonpath "$.traceId" == "phase4a-completed-trace-{{runId}}"
jsonpath "$.lastEventName" == "FLOW_COMPLETED"
jsonpath "$.lastEventResult" == "SUCCESS"
jsonpath "$.eventsReceived" == 1

GET http://localhost:8080/api/traces/phase4a-completed-trace-{{runId}}/status

HTTP 200
[Asserts]
jsonpath "$.status" == "COMPLETED"
