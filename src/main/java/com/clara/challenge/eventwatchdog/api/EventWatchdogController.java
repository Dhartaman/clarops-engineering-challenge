package com.clara.challenge.eventwatchdog.api;

import com.clara.challenge.eventwatchdog.api.dto.EventRequest;
import com.clara.challenge.eventwatchdog.api.dto.TraceStatusResponse;
import com.clara.challenge.eventwatchdog.application.EventIngestionOutcome;
import com.clara.challenge.eventwatchdog.application.EventWatchdogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
public class EventWatchdogController {

  private final EventWatchdogService eventWatchdogService;

  @PostMapping("/events")
  public ResponseEntity<TraceStatusResponse> ingestEvent(@Valid @RequestBody EventRequest request) {
    EventIngestionOutcome outcome = eventWatchdogService.ingestEvent(request.toIncomingEvent());
    HttpStatus status = outcome.duplicate() ? HttpStatus.OK : HttpStatus.CREATED;
    return ResponseEntity.status(status).body(TraceStatusResponse.from(outcome.state()));
  }

  @GetMapping("/traces/{traceId}/status")
  public TraceStatusResponse getTraceStatus(@PathVariable String traceId) {
    return TraceStatusResponse.from(eventWatchdogService.getTraceStatus(traceId));
  }
}
