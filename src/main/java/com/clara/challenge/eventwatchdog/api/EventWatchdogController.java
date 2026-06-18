package com.clara.challenge.eventwatchdog.api;

import com.clara.challenge.eventwatchdog.api.dto.EventRequest;
import com.clara.challenge.eventwatchdog.api.dto.TraceStatusResponse;
import com.clara.challenge.eventwatchdog.application.EventIngestionOutcome;
import com.clara.challenge.eventwatchdog.application.EventWatchdogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
@RequiredArgsConstructor
@Tag(name = "Event Watchdog", description = "Receive distributed events and query trace status.")
public class EventWatchdogController {

  private final EventWatchdogService eventWatchdogService;

  @Operation(
      summary = "Ingest a distributed event",
      description =
          "Stores a new event, updates the trace state, and returns the current trace status.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "201",
        description = "New event accepted",
        content = @Content(schema = @Schema(implementation = TraceStatusResponse.class))),
    @ApiResponse(
        responseCode = "200",
        description = "Duplicate event replay handled idempotently",
        content = @Content(schema = @Schema(implementation = TraceStatusResponse.class))),
    @ApiResponse(
        responseCode = "400",
        description = "Request validation failed",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(
        responseCode = "409",
        description = "Event conflicts with the current trace state",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
  })
  @PostMapping("/events")
  public ResponseEntity<TraceStatusResponse> ingestEvent(@Valid @RequestBody EventRequest request) {
    EventIngestionOutcome outcome = eventWatchdogService.ingestEvent(request.toIncomingEvent());
    HttpStatus status = outcome.duplicate() ? HttpStatus.OK : HttpStatus.CREATED;
    return ResponseEntity.status(status).body(TraceStatusResponse.from(outcome.state()));
  }

  @Operation(
      summary = "Get trace status",
      description =
          "Returns the current trace status, evaluating TTL expiration lazily when applicable.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Trace status found",
        content = @Content(schema = @Schema(implementation = TraceStatusResponse.class))),
    @ApiResponse(
        responseCode = "404",
        description = "Trace was not found",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
  })
  @GetMapping("/traces/{traceId}/status")
  public TraceStatusResponse getTraceStatus(
      @Parameter(description = "Trace identifier to query", example = "trace-123") @PathVariable
          String traceId) {
    return TraceStatusResponse.from(eventWatchdogService.getTraceStatus(traceId));
  }
}
