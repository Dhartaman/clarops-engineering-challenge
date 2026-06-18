package com.clara.challenge.eventwatchdog.application;

import com.clara.challenge.eventwatchdog.domain.IncomingEvent;
import com.clara.challenge.eventwatchdog.domain.TraceStateSnapshot;
import com.clara.challenge.eventwatchdog.domain.TraceStateTransitionService;
import com.clara.challenge.eventwatchdog.domain.TraceStatus;
import com.clara.challenge.eventwatchdog.domain.TraceTransitionResult;
import com.clara.challenge.eventwatchdog.error.BusinessConflictException;
import com.clara.challenge.eventwatchdog.error.TraceNotFoundException;
import com.clara.challenge.eventwatchdog.persistence.TraceEventEntity;
import com.clara.challenge.eventwatchdog.persistence.TraceEventRepository;
import com.clara.challenge.eventwatchdog.persistence.TraceStateEntity;
import com.clara.challenge.eventwatchdog.persistence.TraceStateRepository;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventWatchdogService {

  private static final String DUPLICATE_EVENT_TRACE_MISMATCH = "DUPLICATE_EVENT_TRACE_MISMATCH";

  private final TraceEventRepository traceEventRepository;
  private final TraceStateRepository traceStateRepository;
  private final TraceStateTransitionService transitionService;
  private final Clock clock;

  @Transactional
  public EventIngestionOutcome ingestEvent(IncomingEvent event) {
    return traceEventRepository
        .findById(event.eventId())
        .map(existingEvent -> handleDuplicateEvent(existingEvent, event))
        .orElseGet(() -> handleNewEvent(event));
  }

  @Transactional
  public TraceStateSnapshot getTraceStatus(String traceId) {
    TraceStateEntity stateEntity =
        traceStateRepository
            .findById(traceId)
            .orElseThrow(() -> new TraceNotFoundException(traceId));

    TraceStateSnapshot currentState = toSnapshot(stateEntity);
    TraceStateSnapshot evaluatedState =
        transitionService.evaluateExpiration(currentState, Instant.now(clock));

    if (evaluatedState.status() != currentState.status()) {
      applySnapshot(stateEntity, evaluatedState, stateEntity.getCreatedAt(), Instant.now(clock));
      traceStateRepository.save(stateEntity);
      log.info("Lazy TTL expiration materialized for traceId={}", traceId);
    }

    return evaluatedState;
  }

  private EventIngestionOutcome handleDuplicateEvent(
      TraceEventEntity existingEvent, IncomingEvent incomingEvent) {
    if (!existingEvent.getTraceId().equals(incomingEvent.traceId())) {
      log.warn(
          "Duplicate eventId={} belongs to traceId={} but replay used traceId={}",
          incomingEvent.eventId(),
          existingEvent.getTraceId(),
          incomingEvent.traceId());
      throw new BusinessConflictException(
          DUPLICATE_EVENT_TRACE_MISMATCH, "eventId already exists for a different traceId");
    }

    TraceStateSnapshot state =
        traceStateRepository
            .findById(existingEvent.getTraceId())
            .map(this::toSnapshot)
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Trace state not found for existing eventId="
                            + existingEvent.getEventId()));

    log.info(
        "Duplicate event replay handled for eventId={} traceId={}",
        incomingEvent.eventId(),
        incomingEvent.traceId());
    return new EventIngestionOutcome(state, true);
  }

  private EventIngestionOutcome handleNewEvent(IncomingEvent event) {
    TraceStateEntity existingState = traceStateRepository.findById(event.traceId()).orElse(null);
    TraceTransitionResult transitionResult =
        existingState == null
            ? transitionService.applyFirstEvent(event)
            : transitionService.applyNextEvent(toSnapshot(existingState), event);

    if (transitionResult instanceof TraceTransitionResult.Conflict conflict) {
      log.warn(
          "Event conflict for eventId={} traceId={} reason={}",
          event.eventId(),
          event.traceId(),
          conflict.reason());
      throw new BusinessConflictException(conflict.reason().name(), conflict.message());
    }

    TraceStateSnapshot acceptedState = ((TraceTransitionResult.Accepted) transitionResult).state();
    Instant now = Instant.now(clock);

    traceEventRepository.save(toEntity(event, now));

    TraceStateEntity stateEntity = existingState == null ? new TraceStateEntity() : existingState;
    Instant createdAt = existingState == null ? now : existingState.getCreatedAt();
    applySnapshot(stateEntity, acceptedState, createdAt, now);
    traceStateRepository.save(stateEntity);

    log.info(
        "Event accepted eventId={} traceId={} status={}",
        event.eventId(),
        event.traceId(),
        acceptedState.status());
    if (acceptedState.status() == TraceStatus.COMPLETED) {
      log.info("Trace completed traceId={} eventId={}", event.traceId(), event.eventId());
    }

    return new EventIngestionOutcome(acceptedState, false);
  }

  private TraceEventEntity toEntity(IncomingEvent event, Instant receivedAt) {
    TraceEventEntity entity = new TraceEventEntity();
    entity.setEventId(event.eventId());
    entity.setTraceId(event.traceId());
    entity.setEventName(event.eventName());
    entity.setResult(event.result());
    entity.setOccurredAt(event.occurredAt());
    entity.setReceivedAt(receivedAt);
    entity.setNextExpectedEvent(event.nextExpectedEvent());
    entity.setNextEventTtlSeconds(event.nextEventTtlSeconds());
    entity.setFinalEvent(event.finalEvent());
    entity.setMetadata(event.metadata());
    return entity;
  }

  private TraceStateSnapshot toSnapshot(TraceStateEntity entity) {
    return new TraceStateSnapshot(
        entity.getTraceId(),
        entity.getStatus(),
        entity.getLastEventId(),
        entity.getLastEventName(),
        entity.getLastEventResult(),
        entity.getNextExpectedEvent(),
        entity.getWaitingSince(),
        entity.getNextExpectedBefore(),
        entity.getEventsReceived(),
        entity.getCompletedAt());
  }

  private void applySnapshot(
      TraceStateEntity entity, TraceStateSnapshot snapshot, Instant createdAt, Instant updatedAt) {
    entity.setTraceId(snapshot.traceId());
    entity.setStatus(snapshot.status());
    entity.setLastEventId(snapshot.lastEventId());
    entity.setLastEventName(snapshot.lastEventName());
    entity.setLastEventResult(snapshot.lastEventResult());
    entity.setNextExpectedEvent(snapshot.nextExpectedEvent());
    entity.setWaitingSince(snapshot.waitingSince());
    entity.setNextExpectedBefore(snapshot.nextExpectedBefore());
    entity.setEventsReceived(snapshot.eventsReceived());
    entity.setCompletedAt(snapshot.completedAt());
    entity.setCreatedAt(createdAt);
    entity.setUpdatedAt(updatedAt);
  }
}
