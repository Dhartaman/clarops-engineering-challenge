package com.clara.challenge.eventwatchdog.api.dto;

import com.clara.challenge.eventwatchdog.domain.EventResult;
import com.clara.challenge.eventwatchdog.domain.TraceStateSnapshot;
import com.clara.challenge.eventwatchdog.domain.TraceStatus;
import java.time.Instant;

public record TraceStatusResponse(
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

  public static TraceStatusResponse from(TraceStateSnapshot state) {
    return new TraceStatusResponse(
        state.traceId(),
        state.status(),
        state.lastEventId(),
        state.lastEventName(),
        state.lastEventResult(),
        state.nextExpectedEvent(),
        state.waitingSince(),
        state.nextExpectedBefore(),
        state.eventsReceived(),
        state.completedAt());
  }
}
