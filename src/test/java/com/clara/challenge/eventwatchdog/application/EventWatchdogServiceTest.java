package com.clara.challenge.eventwatchdog.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class EventWatchdogServiceTest {

  private static final Instant BASE_TIME = Instant.parse("2026-06-15T10:00:00Z");
  private static final Instant NOW = Instant.parse("2026-06-15T10:03:00Z");
  private static final Clock FIXED_CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

  @Test
  void shouldCreateTrace_WhenFirstEventAccepted() {
    // Arrange
    RepositoryStore<TraceEventEntity, String> eventStore =
        new RepositoryStore<>(TraceEventEntity::getEventId);
    RepositoryStore<TraceStateEntity, String> stateStore =
        new RepositoryStore<>(TraceStateEntity::getTraceId);
    EventWatchdogService service = service(eventStore, stateStore);
    IncomingEvent event = event("evt-001", "APPLICATION_RECEIVED");

    // Act
    EventIngestionOutcome outcome = service.ingestEvent(event);

    // Assert
    assertFalse(outcome.duplicate());
    assertEquals(TraceStatus.STARTED, outcome.state().status());
    assertEquals(1, outcome.state().eventsReceived());

    assertEquals(1, eventStore.saved().size());
    TraceEventEntity savedEvent = eventStore.saved().getFirst();
    assertEquals("evt-001", savedEvent.getEventId());
    assertEquals("trace-123", savedEvent.getTraceId());
    assertEquals(EventResult.SUCCESS, savedEvent.getResult());
    assertEquals(NOW, savedEvent.getReceivedAt());

    assertEquals(1, stateStore.saved().size());
    TraceStateEntity savedState = stateStore.saved().getFirst();
    assertEquals("trace-123", savedState.getTraceId());
    assertEquals(TraceStatus.STARTED, savedState.getStatus());
    assertEquals("evt-001", savedState.getLastEventId());
    assertEquals(NOW, savedState.getCreatedAt());
    assertEquals(NOW, savedState.getUpdatedAt());
  }

  @Test
  void shouldReturnDuplicateOutcome_WhenEventIdAlreadyExistsForSameTrace() {
    // Arrange
    RepositoryStore<TraceEventEntity, String> eventStore =
        new RepositoryStore<>(TraceEventEntity::getEventId);
    RepositoryStore<TraceStateEntity, String> stateStore =
        new RepositoryStore<>(TraceStateEntity::getTraceId);
    eventStore.put(traceEvent("evt-001", "trace-123", "APPLICATION_RECEIVED"));
    stateStore.put(startedState());
    EventWatchdogService service = service(eventStore, stateStore);
    IncomingEvent event = event("evt-001", "APPLICATION_RECEIVED");

    // Act
    EventIngestionOutcome outcome = service.ingestEvent(event);

    // Assert
    assertTrue(outcome.duplicate());
    assertEquals(TraceStatus.STARTED, outcome.state().status());
    assertEquals(1, outcome.state().eventsReceived());
    assertTrue(eventStore.saved().isEmpty());
    assertTrue(stateStore.saved().isEmpty());
  }

  @Test
  void shouldRejectDuplicateEventId_WhenExistingEventBelongsToDifferentTrace() {
    // Arrange
    RepositoryStore<TraceEventEntity, String> eventStore =
        new RepositoryStore<>(TraceEventEntity::getEventId);
    RepositoryStore<TraceStateEntity, String> stateStore =
        new RepositoryStore<>(TraceStateEntity::getTraceId);
    eventStore.put(traceEvent("evt-001", "other-trace", "APPLICATION_RECEIVED"));
    EventWatchdogService service = service(eventStore, stateStore);
    IncomingEvent event = event("evt-001", "APPLICATION_RECEIVED");

    // Act / Assert
    BusinessConflictException exception =
        assertThrows(BusinessConflictException.class, () -> service.ingestEvent(event));
    assertEquals("DUPLICATE_EVENT_TRACE_MISMATCH", exception.code());
    assertTrue(eventStore.saved().isEmpty());
    assertTrue(stateStore.saved().isEmpty());
  }

  @Test
  void shouldRejectUnexpectedEvent_WhenDomainReturnsConflict() {
    // Arrange
    RepositoryStore<TraceEventEntity, String> eventStore =
        new RepositoryStore<>(TraceEventEntity::getEventId);
    RepositoryStore<TraceStateEntity, String> stateStore =
        new RepositoryStore<>(TraceStateEntity::getTraceId);
    stateStore.put(waitingState());
    EventWatchdogService service = service(eventStore, stateStore);
    IncomingEvent event = event("evt-002", "CONTRACT_SIGNED", BASE_TIME.plusSeconds(30));

    // Act / Assert
    BusinessConflictException exception =
        assertThrows(BusinessConflictException.class, () -> service.ingestEvent(event));
    assertEquals(TraceConflictReason.UNEXPECTED_EVENT.name(), exception.code());
    assertTrue(eventStore.saved().isEmpty());
    assertTrue(stateStore.saved().isEmpty());
  }

  @Test
  void shouldMaterializeExpiration_WhenStatusLookupFindsExpiredWaitingTrace() {
    // Arrange
    RepositoryStore<TraceEventEntity, String> eventStore =
        new RepositoryStore<>(TraceEventEntity::getEventId);
    RepositoryStore<TraceStateEntity, String> stateStore =
        new RepositoryStore<>(TraceStateEntity::getTraceId);
    stateStore.put(waitingState());
    EventWatchdogService service = service(eventStore, stateStore);

    // Act
    TraceStateSnapshot state = service.getTraceStatus("trace-123");

    // Assert
    assertEquals(TraceStatus.TTL_EXPIRED_FOR_EVENT, state.status());
    assertEquals("RULES_EVALUATED", state.nextExpectedEvent());

    assertEquals(1, stateStore.saved().size());
    TraceStateEntity savedState = stateStore.saved().getFirst();
    assertEquals(TraceStatus.TTL_EXPIRED_FOR_EVENT, savedState.getStatus());
    assertEquals("RULES_EVALUATED", savedState.getNextExpectedEvent());
    assertEquals(BASE_TIME, savedState.getWaitingSince());
    assertEquals(BASE_TIME.plusSeconds(120), savedState.getNextExpectedBefore());
    assertEquals(NOW, savedState.getUpdatedAt());
  }

  @Test
  void shouldThrowNotFound_WhenTraceStatusDoesNotExist() {
    // Arrange
    RepositoryStore<TraceEventEntity, String> eventStore =
        new RepositoryStore<>(TraceEventEntity::getEventId);
    RepositoryStore<TraceStateEntity, String> stateStore =
        new RepositoryStore<>(TraceStateEntity::getTraceId);
    EventWatchdogService service = service(eventStore, stateStore);

    // Act / Assert
    assertThrows(TraceNotFoundException.class, () -> service.getTraceStatus("missing-trace"));
    assertTrue(stateStore.saved().isEmpty());
  }

  private static EventWatchdogService service(
      RepositoryStore<TraceEventEntity, String> eventStore,
      RepositoryStore<TraceStateEntity, String> stateStore) {
    return new EventWatchdogService(
        eventStore.repository(TraceEventRepository.class),
        stateStore.repository(TraceStateRepository.class),
        new TraceStateTransitionService(),
        FIXED_CLOCK);
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

  private static final class RepositoryStore<T, ID> {

    private final Function<T, ID> idExtractor;
    private final Map<ID, T> rows = new LinkedHashMap<>();
    private final List<T> saved = new ArrayList<>();

    private RepositoryStore(Function<T, ID> idExtractor) {
      this.idExtractor = idExtractor;
    }

    private void put(T entity) {
      rows.put(idExtractor.apply(entity), entity);
    }

    private List<T> saved() {
      return saved;
    }

    private <R> R repository(Class<R> repositoryType) {
      InvocationHandler handler =
          (proxy, method, args) -> {
            if (method.getDeclaringClass() == Object.class) {
              return method.invoke(this, args);
            }
            if ("findById".equals(method.getName())) {
              return Optional.ofNullable(rows.get(args[0]));
            }
            if ("save".equals(method.getName())) {
              T entity = castEntity(args[0]);
              rows.put(idExtractor.apply(entity), entity);
              saved.add(entity);
              return entity;
            }
            throw new UnsupportedOperationException("Unsupported repository method: " + method);
          };
      Object proxy =
          Proxy.newProxyInstance(
              repositoryType.getClassLoader(), new Class<?>[] {repositoryType}, handler);
      return repositoryType.cast(proxy);
    }

    @SuppressWarnings("unchecked")
    private T castEntity(Object value) {
      return (T) value;
    }
  }
}
