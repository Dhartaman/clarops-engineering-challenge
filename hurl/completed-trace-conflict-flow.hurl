POST http://localhost:8080/api/events
Content-Type: application/json
{
  "eventId": "phase4b-completed-event-{{runId}}-001",
  "traceId": "phase4b-completed-conflict-trace-{{runId}}",
  "eventName": "FLOW_COMPLETED",
  "result": "SUCCESS",
  "occurredAt": "2099-06-15T10:00:00Z",
  "finalEvent": true
}

HTTP 201
[Asserts]
jsonpath "$.status" == "COMPLETED"
jsonpath "$.eventsReceived" == 1

POST http://localhost:8080/api/events
Content-Type: application/json
{
  "eventId": "phase4b-completed-event-{{runId}}-002",
  "traceId": "phase4b-completed-conflict-trace-{{runId}}",
  "eventName": "AFTER_COMPLETION_EVENT",
  "result": "SUCCESS",
  "occurredAt": "2099-06-15T10:01:00Z",
  "finalEvent": false
}

HTTP 409
[Asserts]
jsonpath "$.status" == 409
jsonpath "$.title" == "Trace conflict"
