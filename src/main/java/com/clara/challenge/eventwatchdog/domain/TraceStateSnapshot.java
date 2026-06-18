package com.clara.challenge.eventwatchdog.domain;

import java.time.Instant;
import java.util.Objects;

public record TraceStateSnapshot(
    String traceId,
    TraceStatus status,
    String lastEventId,
    String lastEventName,
    EventResult lastEventResult,
    String nextExpectedEvent,
    Instant waitingSince,
    Instant nextExpectedBefore,
    int eventsReceived,
    Instant completedAt) {

  public TraceStateSnapshot {
    requireText(traceId, "traceId");
    Objects.requireNonNull(status, "status must not be null");
    requireText(lastEventId, "lastEventId");
    requireText(lastEventName, "lastEventName");
    Objects.requireNonNull(lastEventResult, "lastEventResult must not be null");
    if (eventsReceived <= 0) {
      throw new IllegalArgumentException("eventsReceived must be positive");
    }
    if (nextExpectedEvent != null && nextExpectedEvent.isBlank()) {
      throw new IllegalArgumentException("nextExpectedEvent must not be blank when provided");
    }
  }

  private static void requireText(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }
  }
}
