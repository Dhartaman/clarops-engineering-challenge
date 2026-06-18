package com.clara.challenge.eventwatchdog.api.dto;

import com.clara.challenge.eventwatchdog.domain.EventResult;
import com.clara.challenge.eventwatchdog.domain.IncomingEvent;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Instant;
import java.util.Map;

public record EventRequest(
    @Schema(description = "Unique event identifier used for idempotent replay handling.") @NotBlank
        String eventId,
    @Schema(description = "Distributed flow trace identifier.") @NotBlank String traceId,
    @Schema(description = "Name of the event received for the trace.") @NotBlank String eventName,
    @Schema(description = "Result reported by the event.") @NotNull EventResult result,
    @Schema(description = "Timestamp when the event occurred.") @NotNull Instant occurredAt,
    @Schema(description = "Next event expected for the trace, if another event must arrive.")
        String nextExpectedEvent,
    @Schema(description = "TTL in seconds for the next expected event.") @Positive Integer nextEventTtlSeconds,
    @Schema(description = "Whether this event completes the trace.") boolean finalEvent,
    @Schema(description = "Optional event metadata stored as JSON.") Map<String, Object> metadata) {

  @AssertTrue(message = "nextExpectedEvent and nextEventTtlSeconds must be provided together")
  public boolean isNextExpectationValid() {
    boolean hasExpectedEvent = nextExpectedEvent != null && !nextExpectedEvent.isBlank();
    boolean hasTtl = nextEventTtlSeconds != null;
    return hasExpectedEvent == hasTtl
        && (nextExpectedEvent == null || !nextExpectedEvent.isBlank());
  }

  public IncomingEvent toIncomingEvent() {
    return new IncomingEvent(
        eventId,
        traceId,
        eventName,
        result,
        occurredAt,
        nextExpectedEvent,
        nextEventTtlSeconds,
        finalEvent,
        metadata);
  }
}
