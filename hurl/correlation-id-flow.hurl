GET http://localhost:8080/api/traces/phase4d-correlation-trace-{{runId}}/status
X-Correlation-Id: phase4d-correlation-{{runId}}

HTTP 404
[Asserts]
header "X-Correlation-Id" == "phase4d-correlation-{{runId}}"
jsonpath "$.status" == 404
jsonpath "$.title" == "Trace not found"
