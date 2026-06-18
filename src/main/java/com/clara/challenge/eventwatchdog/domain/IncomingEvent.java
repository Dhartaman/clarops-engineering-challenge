package com.clara.challenge.eventwatchdog.domain;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public record IncomingEvent(
    String eventId,
    String traceId,
    String eventName,
    EventResult result,
    Instant occurredAt,
    String nextExpectedEvent,
    Integer nextEventTtlSeconds,
    boolean finalEvent,
    Map<String, Object> metadata) {

  public IncomingEvent {
    requireText(eventId, "eventId");
    requireText(traceId, "traceId");
    requireText(eventName, "eventName");
    Objects.requireNonNull(result, "result must not be null");
    Objects.requireNonNull(occurredAt, "occurredAt must not be null");

    if (nextExpectedEvent != null && nextExpectedEvent.isBlank()) {
      throw new IllegalArgumentException("nextExpectedEvent must not be blank when provided");
    }
    if (nextEventTtlSeconds != null && nextEventTtlSeconds <= 0) {
      throw new IllegalArgumentException("nextEventTtlSeconds must be positive when provided");
    }
    if ((nextExpectedEvent == null) != (nextEventTtlSeconds == null)) {
      throw new IllegalArgumentException(
          "nextExpectedEvent and nextEventTtlSeconds must be provided together");
    }

    metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
  }

  boolean definesNextExpectation() {
    return nextExpectedEvent != null;
  }

  private static void requireText(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }
  }
}
