package com.clara.challenge.eventwatchdog.api.dto;

import com.clara.challenge.eventwatchdog.domain.EventResult;
import com.clara.challenge.eventwatchdog.domain.IncomingEvent;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Instant;
import java.util.Map;

public record EventRequest(
    @NotBlank String eventId,
    @NotBlank String traceId,
    @NotBlank String eventName,
    @NotNull EventResult result,
    @NotNull Instant occurredAt,
    String nextExpectedEvent,
    @Positive Integer nextEventTtlSeconds,
    boolean finalEvent,
    Map<String, Object> metadata) {

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
