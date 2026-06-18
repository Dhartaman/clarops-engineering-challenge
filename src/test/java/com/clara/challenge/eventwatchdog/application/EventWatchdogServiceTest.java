package com.clara.challenge.eventwatchdog.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.clara.challenge.eventwatchdog.domain.EventResult;
import com.clara.challenge.eventwatchdog.domain.IncomingEvent;
import com.clara.challenge.eventwatchdog.domain.TraceConflictReason;
import com.clara.challenge.eventwatchdog.domain.TraceStateSnapshot;
import com.clara.challenge.eventwatchdog.domain.TraceStateTransitionService;
import com.clara.challenge.eventwatchdog.domain.TraceStatus;
import com.clara.challenge.eventwatchdog.error.BusinessConflictException;
import com.clara.challenge.eventwatchdog.error.TraceNotFoundException;
import com.clara.challenge.eventwatchdog.persistence.TraceEventEntity;
import com.clara.challenge.eventwatchdog.persistence.TraceEventRepository;
import com.clara.challenge.eventwatchdog.persistence.TraceStateEntity;
import com.clara.challenge.eventwatchdog.persistence.TraceStateRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockMakers;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EventWatchdogServiceTest {

  private static final Instant BASE_TIME = Instant.parse("2026-06-15T10:00:00Z");
  private static final Instant NOW = Instant.parse("2026-06-15T10:03:00Z");
  private static final Clock FIXED_CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

  @Mock(mockMaker = MockMakers.SUBCLASS)
  private TraceEventRepository eventRepository;

  @Mock(mockMaker = MockMakers.SUBCLASS)
  private TraceStateRepository stateRepository;

  private EventWatchdogService service;

  @BeforeEach
  void setUp() {
    service =
        new EventWatchdogService(
            eventRepository, stateRepository, new TraceStateTransitionService(), FIXED_CLOCK);
  }

  @Test
  void shouldCreateTrace_WhenFirstEventAccepted() {
    // Arrange
    when(eventRepository.findById("evt-001")).thenReturn(Optional.empty());
    when(stateRepository.findById("trace-123")).thenReturn(Optional.empty());
    when(eventRepository.save(any(TraceEventEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(stateRepository.save(any(TraceStateEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    IncomingEvent event = event("evt-001", "APPLICATION_RECEIVED");

    // Act
    EventIngestionOutcome outcome = service.ingestEvent(event);

    // Assert
    assertFalse(outcome.duplicate());
    assertEquals(TraceStatus.STARTED, outcome.state().status());
    assertEquals(1, outcome.state().eventsReceived());

    ArgumentCaptor<TraceEventEntity> eventCaptor = ArgumentCaptor.forClass(TraceEventEntity.class);
    verify(eventRepository).save(eventCaptor.capture());
    TraceEventEntity savedEvent = eventCaptor.getValue();
    assertEquals("evt-001", savedEvent.getEventId());
    assertEquals("trace-123", savedEvent.getTraceId());
    assertEquals(EventResult.SUCCESS, savedEvent.getResult());
    assertEquals(NOW, savedEvent.getReceivedAt());

    ArgumentCaptor<TraceStateEntity> stateCaptor = ArgumentCaptor.forClass(TraceStateEntity.class);
    verify(stateRepository).save(stateCaptor.capture());
    TraceStateEntity savedState = stateCaptor.getValue();
    assertEquals("trace-123", savedState.getTraceId());
    assertEquals(TraceStatus.STARTED, savedState.getStatus());
    assertEquals("evt-001", savedState.getLastEventId());
    assertEquals(NOW, savedState.getCreatedAt());
    assertEquals(NOW, savedState.getUpdatedAt());
  }

  @Test
  void shouldReturnDuplicateOutcome_WhenEventIdAlreadyExistsForSameTrace() {
    // Arrange
    when(eventRepository.findById("evt-001"))
        .thenReturn(Optional.of(traceEvent("evt-001", "trace-123", "APPLICATION_RECEIVED")));
    when(stateRepository.findById("trace-123")).thenReturn(Optional.of(startedState()));
    IncomingEvent event = event("evt-001", "APPLICATION_RECEIVED");

    // Act
    EventIngestionOutcome outcome = service.ingestEvent(event);

    // Assert
    assertTrue(outcome.duplicate());
    assertEquals(TraceStatus.STARTED, outcome.state().status());
    assertEquals(1, outcome.state().eventsReceived());
    verify(eventRepository, never()).save(any());
    verify(stateRepository, never()).save(any());
  }

  @Test
  void shouldRejectDuplicateEventId_WhenExistingEventBelongsToDifferentTrace() {
    // Arrange
    when(eventRepository.findById("evt-001"))
        .thenReturn(Optional.of(traceEvent("evt-001", "other-trace", "APPLICATION_RECEIVED")));
    IncomingEvent event = event("evt-001", "APPLICATION_RECEIVED");

    // Act / Assert
    BusinessConflictException exception =
        assertThrows(BusinessConflictException.class, () -> service.ingestEvent(event));
    assertEquals("DUPLICATE_EVENT_TRACE_MISMATCH", exception.code());
    verify(eventRepository, never()).save(any());
    verify(stateRepository, never()).save(any());
  }

  @Test
  void shouldRejectUnexpectedEvent_WhenDomainReturnsConflict() {
    // Arrange
    when(eventRepository.findById("evt-002")).thenReturn(Optional.empty());
    when(stateRepository.findById("trace-123")).thenReturn(Optional.of(waitingState()));
    IncomingEvent event = event("evt-002", "CONTRACT_SIGNED", BASE_TIME.plusSeconds(30));

    // Act / Assert
    BusinessConflictException exception =
        assertThrows(BusinessConflictException.class, () -> service.ingestEvent(event));
    assertEquals(TraceConflictReason.UNEXPECTED_EVENT.name(), exception.code());
    verify(eventRepository, never()).save(any());
    verify(stateRepository, never()).save(any());
  }

  @Test
  void shouldMaterializeExpiration_WhenStatusLookupFindsExpiredWaitingTrace() {
    // Arrange
    when(stateRepository.findById("trace-123")).thenReturn(Optional.of(waitingState()));
    when(stateRepository.save(any(TraceStateEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // Act
    TraceStateSnapshot state = service.getTraceStatus("trace-123");

    // Assert
    assertEquals(TraceStatus.TTL_EXPIRED_FOR_EVENT, state.status());
    assertEquals("RULES_EVALUATED", state.nextExpectedEvent());

    ArgumentCaptor<TraceStateEntity> stateCaptor = ArgumentCaptor.forClass(TraceStateEntity.class);
    verify(stateRepository).save(stateCaptor.capture());
    TraceStateEntity savedState = stateCaptor.getValue();
    assertEquals(TraceStatus.TTL_EXPIRED_FOR_EVENT, savedState.getStatus());
    assertEquals("RULES_EVALUATED", savedState.getNextExpectedEvent());
    assertEquals(BASE_TIME, savedState.getWaitingSince());
    assertEquals(BASE_TIME.plusSeconds(120), savedState.getNextExpectedBefore());
    assertEquals(NOW, savedState.getUpdatedAt());
    verifyNoInteractions(eventRepository);
  }

  @Test
  void shouldThrowNotFound_WhenTraceStatusDoesNotExist() {
    // Arrange
    when(stateRepository.findById("missing-trace")).thenReturn(Optional.empty());

    // Act / Assert
    assertThrows(TraceNotFoundException.class, () -> service.getTraceStatus("missing-trace"));
    verify(stateRepository, never()).save(any());
    verifyNoInteractions(eventRepository);
  }

  private static IncomingEvent event(String eventId, String eventName) {
    return event(eventId, eventName, BASE_TIME);
  }

  private static IncomingEvent event(String eventId, String eventName, Instant occurredAt) {
    return new IncomingEvent(
        eventId,
        "trace-123",
        eventName,
        EventResult.SUCCESS,
        occurredAt,
        null,
        null,
        false,
        Map.of("country", "MX"));
  }

  private static TraceEventEntity traceEvent(String eventId, String traceId, String eventName) {
    TraceEventEntity entity = new TraceEventEntity();
    entity.setEventId(eventId);
    entity.setTraceId(traceId);
    entity.setEventName(eventName);
    entity.setResult(EventResult.SUCCESS);
    entity.setOccurredAt(BASE_TIME);
    entity.setReceivedAt(NOW);
    entity.setFinalEvent(false);
    entity.setMetadata(Map.of());
    return entity;
  }

  private static TraceStateEntity startedState() {
    TraceStateEntity entity = new TraceStateEntity();
    entity.setTraceId("trace-123");
    entity.setStatus(TraceStatus.STARTED);
    entity.setLastEventId("evt-001");
    entity.setLastEventName("APPLICATION_RECEIVED");
    entity.setLastEventResult(EventResult.SUCCESS);
    entity.setEventsReceived(1);
    entity.setCreatedAt(BASE_TIME);
    entity.setUpdatedAt(BASE_TIME);
    entity.setVersion(0L);
    return entity;
  }

  private static TraceStateEntity waitingState() {
    TraceStateEntity entity = startedState();
    entity.setStatus(TraceStatus.WAITING_OTHER_EVENT);
    entity.setNextExpectedEvent("RULES_EVALUATED");
    entity.setWaitingSince(BASE_TIME);
    entity.setNextExpectedBefore(BASE_TIME.plusSeconds(120));
    return entity;
  }
}
