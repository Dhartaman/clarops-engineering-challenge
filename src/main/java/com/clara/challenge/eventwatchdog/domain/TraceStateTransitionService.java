package com.clara.challenge.eventwatchdog.domain;

import java.time.Instant;
import java.util.Objects;

public class TraceStateTransitionService {

  public TraceTransitionResult applyFirstEvent(IncomingEvent event) {
    Objects.requireNonNull(event, "event must not be null");
    return new TraceTransitionResult.Accepted(snapshotFrom(event, 1));
  }

  public TraceTransitionResult applyNextEvent(
      TraceStateSnapshot currentState, IncomingEvent event) {
    Objects.requireNonNull(currentState, "currentState must not be null");
    Objects.requireNonNull(event, "event must not be null");

    if (!currentState.traceId().equals(event.traceId())) {
      throw new IllegalArgumentException("event traceId must match current traceId");
    }

    // Terminal states reject new events in the MVP; duplicate replay is handled before this domain
    // transition is invoked.
    return switch (currentState.status()) {
      case STARTED -> acceptNext(currentState, event);
      case WAITING_OTHER_EVENT -> applyWaitingTransition(currentState, event);
      case COMPLETED ->
          new TraceTransitionResult.Conflict(
              TraceConflictReason.TRACE_ALREADY_COMPLETED,
              "Trace is already completed and cannot accept new events");
      case TTL_EXPIRED_FOR_EVENT ->
          new TraceTransitionResult.Conflict(
              TraceConflictReason.TRACE_ALREADY_EXPIRED,
              "Trace is already expired and cannot accept new events");
    };
  }

  public TraceStateSnapshot evaluateExpiration(TraceStateSnapshot currentState, Instant now) {
    Objects.requireNonNull(currentState, "currentState must not be null");
    Objects.requireNonNull(now, "now must not be null");

    if (currentState.status() != TraceStatus.WAITING_OTHER_EVENT
        || currentState.nextExpectedBefore() == null
        || !now.isAfter(currentState.nextExpectedBefore())) {
      return currentState;
    }

    return new TraceStateSnapshot(
        currentState.traceId(),
        TraceStatus.TTL_EXPIRED_FOR_EVENT,
        currentState.lastEventId(),
        currentState.lastEventName(),
        currentState.lastEventResult(),
        currentState.nextExpectedEvent(),
        currentState.waitingSince(),
        currentState.nextExpectedBefore(),
        currentState.eventsReceived(),
        null);
  }

  private TraceTransitionResult applyWaitingTransition(
      TraceStateSnapshot currentState, IncomingEvent event) {
    ensureValidWaitingSnapshot(currentState);

    if (!event.eventName().equals(currentState.nextExpectedEvent())) {
      return new TraceTransitionResult.Conflict(
          TraceConflictReason.UNEXPECTED_EVENT,
          "Trace is waiting for event "
              + currentState.nextExpectedEvent()
              + " but received "
              + event.eventName());
    }
    if (event.occurredAt().isAfter(currentState.nextExpectedBefore())) {
      return new TraceTransitionResult.Conflict(
          TraceConflictReason.LATE_EVENT,
          "Expected event "
              + event.eventName()
              + " occurred at "
              + event.occurredAt()
              + " after deadline "
              + currentState.nextExpectedBefore());
    }

    return acceptNext(currentState, event);
  }

  private void ensureValidWaitingSnapshot(TraceStateSnapshot currentState) {
    if (currentState.nextExpectedEvent() == null
        || currentState.waitingSince() == null
        || currentState.nextExpectedBefore() == null) {
      throw new IllegalStateException(
          "WAITING_OTHER_EVENT snapshot must include nextExpectedEvent, waitingSince, and"
              + " nextExpectedBefore");
    }
  }

  private TraceTransitionResult acceptNext(TraceStateSnapshot currentState, IncomingEvent event) {
    return new TraceTransitionResult.Accepted(
        snapshotFrom(event, currentState.eventsReceived() + 1));
  }

  private TraceStateSnapshot snapshotFrom(IncomingEvent event, int eventsReceived) {
    if (event.finalEvent()) {
      return new TraceStateSnapshot(
          event.traceId(),
          TraceStatus.COMPLETED,
          event.eventId(),
          event.eventName(),
          event.result(),
          null,
          null,
          null,
          eventsReceived,
          event.occurredAt());
    }

    if (event.definesNextExpectation()) {
      // Deadlines follow business event time rather than server receive time, so transport delay
      // does not shift the agreed TTL window.
      return new TraceStateSnapshot(
          event.traceId(),
          TraceStatus.WAITING_OTHER_EVENT,
          event.eventId(),
          event.eventName(),
          event.result(),
          event.nextExpectedEvent(),
          event.occurredAt(),
          event.occurredAt().plusSeconds(event.nextEventTtlSeconds()),
          eventsReceived,
          null);
    }

    return new TraceStateSnapshot(
        event.traceId(),
        TraceStatus.STARTED,
        event.eventId(),
        event.eventName(),
        event.result(),
        null,
        null,
        null,
        eventsReceived,
        null);
  }
}
