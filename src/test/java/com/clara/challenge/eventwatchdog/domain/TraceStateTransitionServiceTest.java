package com.clara.challenge.eventwatchdog.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TraceStateTransitionServiceTest {

  private static final Instant BASE_TIME = Instant.parse("2026-06-15T10:00:00Z");

  private final TraceStateTransitionService service = new TraceStateTransitionService();

  @Test
  void shouldCreateStartedTrace_WhenFirstEventHasNoExpectation() {
    // Arrange
    IncomingEvent event = event("evt-001", "APPLICATION_RECEIVED");

    // Act
    TraceTransitionResult result = service.applyFirstEvent(event);

    // Assert
    TraceStateSnapshot state = acceptedState(result);
    assertEquals(TraceStatus.STARTED, state.status());
    assertEquals("trace-123", state.traceId());
    assertEquals("evt-001", state.lastEventId());
    assertEquals("APPLICATION_RECEIVED", state.lastEventName());
    assertEquals(EventResult.SUCCESS, state.lastEventResult());
    assertEquals(1, state.eventsReceived());
    assertNull(state.nextExpectedEvent());
    assertNull(state.waitingSince());
    assertNull(state.nextExpectedBefore());
    assertNull(state.completedAt());
  }

  @Test
  void shouldCreateWaitingTrace_WhenFirstEventDefinesExpectedEvent() {
    // Arrange
    IncomingEvent event =
        eventWaitingFor("evt-001", "APPLICATION_RECEIVED", "RULES_EVALUATED", 120);

    // Act
    TraceTransitionResult result = service.applyFirstEvent(event);

    // Assert
    TraceStateSnapshot state = acceptedState(result);
    assertEquals(TraceStatus.WAITING_OTHER_EVENT, state.status());
    assertEquals("RULES_EVALUATED", state.nextExpectedEvent());
    assertEquals(BASE_TIME, state.waitingSince());
    assertEquals(BASE_TIME.plusSeconds(120), state.nextExpectedBefore());
    assertEquals(1, state.eventsReceived());
    assertNull(state.completedAt());
  }

  @Test
  void shouldCompleteTrace_WhenFirstEventIsFinal() {
    // Arrange
    IncomingEvent event = finalEvent("evt-001", "APPLICATION_COMPLETED", BASE_TIME);

    // Act
    TraceTransitionResult result = service.applyFirstEvent(event);

    // Assert
    TraceStateSnapshot state = acceptedState(result);
    assertEquals(TraceStatus.COMPLETED, state.status());
    assertEquals("APPLICATION_COMPLETED", state.lastEventName());
    assertEquals(1, state.eventsReceived());
    assertEquals(BASE_TIME, state.completedAt());
    assertNull(state.nextExpectedEvent());
    assertNull(state.waitingSince());
    assertNull(state.nextExpectedBefore());
  }

  @Test
  void shouldCompleteTrace_WhenAcceptedEventIsFinal() {
    // Arrange
    TraceStateSnapshot currentState =
        waitingState("evt-001", "APPLICATION_RECEIVED", "RULES_EVALUATED");
    IncomingEvent event = finalEvent("evt-002", "RULES_EVALUATED", BASE_TIME.plusSeconds(60));

    // Act
    TraceTransitionResult result = service.applyNextEvent(currentState, event);

    // Assert
    TraceStateSnapshot state = acceptedState(result);
    assertEquals(TraceStatus.COMPLETED, state.status());
    assertEquals("evt-002", state.lastEventId());
    assertEquals("RULES_EVALUATED", state.lastEventName());
    assertEquals(2, state.eventsReceived());
    assertEquals(BASE_TIME.plusSeconds(60), state.completedAt());
    assertNull(state.nextExpectedEvent());
    assertNull(state.waitingSince());
    assertNull(state.nextExpectedBefore());
  }

  @Test
  void shouldKeepErrorAsEventResult_WhenErrorEventIsNotFinal() {
    // Arrange
    TraceStateSnapshot currentState = startedState();
    IncomingEvent event =
        event("evt-002", "RULES_EVALUATED", EventResult.ERROR, BASE_TIME.plusSeconds(30));

    // Act
    TraceTransitionResult result = service.applyNextEvent(currentState, event);

    // Assert
    TraceStateSnapshot state = acceptedState(result);
    assertEquals(TraceStatus.STARTED, state.status());
    assertEquals(EventResult.ERROR, state.lastEventResult());
    assertEquals("RULES_EVALUATED", state.lastEventName());
    assertEquals(2, state.eventsReceived());
    assertNull(state.completedAt());
  }

  @Test
  void shouldRejectUnexpectedEvent_WhenTraceIsWaitingForDifferentEvent() {
    // Arrange
    TraceStateSnapshot currentState =
        waitingState("evt-001", "APPLICATION_RECEIVED", "RULES_EVALUATED");
    IncomingEvent event = event("evt-002", "CONTRACT_SIGNED", BASE_TIME.plusSeconds(30));

    // Act
    TraceTransitionResult result = service.applyNextEvent(currentState, event);

    // Assert
    TraceTransitionResult.Conflict conflict =
        assertInstanceOf(TraceTransitionResult.Conflict.class, result);
    assertEquals(TraceConflictReason.UNEXPECTED_EVENT, conflict.reason());
  }

  @Test
  void shouldRejectLateEvent_WhenExpectedEventArrivesAfterDeadline() {
    // Arrange
    TraceStateSnapshot currentState =
        waitingState("evt-001", "APPLICATION_RECEIVED", "RULES_EVALUATED");
    IncomingEvent event = event("evt-002", "RULES_EVALUATED", BASE_TIME.plusSeconds(121));

    // Act
    TraceTransitionResult result = service.applyNextEvent(currentState, event);

    // Assert
    TraceTransitionResult.Conflict conflict =
        assertInstanceOf(TraceTransitionResult.Conflict.class, result);
    assertEquals(TraceConflictReason.LATE_EVENT, conflict.reason());
  }

  @Test
  void shouldRejectNewEvent_WhenTraceIsCompleted() {
    // Arrange
    TraceStateSnapshot currentState =
        new TraceStateSnapshot(
            "trace-123",
            TraceStatus.COMPLETED,
            "evt-001",
            "APPLICATION_COMPLETED",
            EventResult.SUCCESS,
            null,
            null,
            null,
            1,
            BASE_TIME);
    IncomingEvent event = event("evt-002", "RULES_EVALUATED", BASE_TIME.plusSeconds(30));

    // Act
    TraceTransitionResult result = service.applyNextEvent(currentState, event);

    // Assert
    TraceTransitionResult.Conflict conflict =
        assertInstanceOf(TraceTransitionResult.Conflict.class, result);
    assertEquals(TraceConflictReason.TRACE_ALREADY_COMPLETED, conflict.reason());
  }

  @Test
  void shouldRejectNewEvent_WhenTraceIsExpired() {
    // Arrange
    TraceStateSnapshot currentState = expiredState();
    IncomingEvent event = event("evt-002", "RULES_EVALUATED", BASE_TIME.plusSeconds(130));

    // Act
    TraceTransitionResult result = service.applyNextEvent(currentState, event);

    // Assert
    TraceTransitionResult.Conflict conflict =
        assertInstanceOf(TraceTransitionResult.Conflict.class, result);
    assertEquals(TraceConflictReason.TRACE_ALREADY_EXPIRED, conflict.reason());
  }

  @Test
  void shouldReturnExpiredState_WhenWaitingDeadlineHasPassed() {
    // Arrange
    TraceStateSnapshot currentState =
        waitingState("evt-001", "APPLICATION_RECEIVED", "RULES_EVALUATED");

    // Act
    TraceStateSnapshot state = service.evaluateExpiration(currentState, BASE_TIME.plusSeconds(121));

    // Assert
    assertEquals(TraceStatus.TTL_EXPIRED_FOR_EVENT, state.status());
    assertEquals("RULES_EVALUATED", state.nextExpectedEvent());
    assertEquals(BASE_TIME, state.waitingSince());
    assertEquals(BASE_TIME.plusSeconds(120), state.nextExpectedBefore());
    assertNull(state.completedAt());
  }

  @Test
  void shouldKeepCurrentState_WhenWaitingDeadlineHasNotPassed() {
    // Arrange
    TraceStateSnapshot currentState =
        waitingState("evt-001", "APPLICATION_RECEIVED", "RULES_EVALUATED");

    // Act
    TraceStateSnapshot state = service.evaluateExpiration(currentState, BASE_TIME.plusSeconds(120));

    // Assert
    assertSame(currentState, state);
    assertEquals(TraceStatus.WAITING_OTHER_EVENT, state.status());
  }

  @Test
  void shouldKeepExpiredState_WhenExpirationIsEvaluatedAgain() {
    // Arrange
    TraceStateSnapshot currentState = expiredState();

    // Act
    TraceStateSnapshot state = service.evaluateExpiration(currentState, BASE_TIME.plusSeconds(240));

    // Assert
    assertSame(currentState, state);
    assertEquals(TraceStatus.TTL_EXPIRED_FOR_EVENT, state.status());
  }

  @Test
  void shouldRejectInvalidWaitingSnapshot_WhenWaitingFieldsAreMissing() {
    // Arrange
    TraceStateSnapshot currentState =
        new TraceStateSnapshot(
            "trace-123",
            TraceStatus.WAITING_OTHER_EVENT,
            "evt-001",
            "APPLICATION_RECEIVED",
            EventResult.SUCCESS,
            "RULES_EVALUATED",
            null,
            BASE_TIME.plusSeconds(120),
            1,
            null);
    IncomingEvent event = event("evt-002", "RULES_EVALUATED", BASE_TIME.plusSeconds(30));

    // Act / Assert
    assertThrows(IllegalStateException.class, () -> service.applyNextEvent(currentState, event));
  }

  private static TraceStateSnapshot acceptedState(TraceTransitionResult result) {
    return assertInstanceOf(TraceTransitionResult.Accepted.class, result).state();
  }

  private static TraceStateSnapshot startedState() {
    return new TraceStateSnapshot(
        "trace-123",
        TraceStatus.STARTED,
        "evt-001",
        "APPLICATION_RECEIVED",
        EventResult.SUCCESS,
        null,
        null,
        null,
        1,
        null);
  }

  private static TraceStateSnapshot waitingState(
      String lastEventId, String lastEventName, String nextExpectedEvent) {
    return new TraceStateSnapshot(
        "trace-123",
        TraceStatus.WAITING_OTHER_EVENT,
        lastEventId,
        lastEventName,
        EventResult.SUCCESS,
        nextExpectedEvent,
        BASE_TIME,
        BASE_TIME.plusSeconds(120),
        1,
        null);
  }

  private static TraceStateSnapshot expiredState() {
    return new TraceStateSnapshot(
        "trace-123",
        TraceStatus.TTL_EXPIRED_FOR_EVENT,
        "evt-001",
        "APPLICATION_RECEIVED",
        EventResult.SUCCESS,
        "RULES_EVALUATED",
        BASE_TIME,
        BASE_TIME.plusSeconds(120),
        1,
        null);
  }

  private static IncomingEvent event(String eventId, String eventName) {
    return event(eventId, eventName, BASE_TIME);
  }

  private static IncomingEvent event(String eventId, String eventName, Instant occurredAt) {
    return event(eventId, eventName, EventResult.SUCCESS, occurredAt);
  }

  private static IncomingEvent event(
      String eventId, String eventName, EventResult result, Instant occurredAt) {
    return new IncomingEvent(
        eventId, "trace-123", eventName, result, occurredAt, null, null, false, Map.of());
  }

  private static IncomingEvent eventWaitingFor(
      String eventId, String eventName, String nextExpectedEvent, int nextEventTtlSeconds) {
    return new IncomingEvent(
        eventId,
        "trace-123",
        eventName,
        EventResult.SUCCESS,
        BASE_TIME,
        nextExpectedEvent,
        nextEventTtlSeconds,
        false,
        Map.of());
  }

  private static IncomingEvent finalEvent(String eventId, String eventName, Instant occurredAt) {
    return new IncomingEvent(
        eventId,
        "trace-123",
        eventName,
        EventResult.SUCCESS,
        occurredAt,
        null,
        null,
        true,
        Map.of());
  }
}
