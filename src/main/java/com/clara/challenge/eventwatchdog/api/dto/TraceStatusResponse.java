package com.clara.challenge.eventwatchdog.api.dto;

import com.clara.challenge.eventwatchdog.domain.EventResult;
import com.clara.challenge.eventwatchdog.domain.TraceStateSnapshot;
import com.clara.challenge.eventwatchdog.domain.TraceStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

public record TraceStatusResponse(
    @Schema(description = "Distributed flow trace identifier.") String traceId,
    @Schema(description = "Current trace status.") TraceStatus status,
    @Schema(description = "Most recent event identifier for the trace.") String lastEventId,
    @Schema(description = "Most recent event name for the trace.") String lastEventName,
    @Schema(description = "Result reported by the most recent event.") EventResult lastEventResult,
    @Schema(description = "Event currently expected before the TTL deadline.")
        String nextExpectedEvent,
    @Schema(description = "Timestamp when the trace started waiting for the expected event.")
        Instant waitingSince,
    @Schema(description = "Deadline for the next expected event.") Instant nextExpectedBefore,
    @Schema(description = "Number of accepted events received for this trace.") int eventsReceived,
    @Schema(description = "Timestamp when the trace completed, if completed.")
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
