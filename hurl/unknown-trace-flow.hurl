GET http://localhost:8080/api/traces/phase4b-unknown-trace-{{runId}}/status

HTTP 404
[Asserts]
jsonpath "$.status" == 404
jsonpath "$.title" == "Trace not found"
jsonpath "$.traceId" == "phase4b-unknown-trace-{{runId}}"
