package com.clara.challenge.eventwatchdog.persistence;

import com.clara.challenge.eventwatchdog.domain.EventResult;
import com.clara.challenge.eventwatchdog.domain.TraceStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(schema = "clarops_challenge_schema", name = "trace_states")
public class TraceStateEntity {

  @Id
  @Column(name = "trace_id", nullable = false, length = 100)
  private String traceId;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 40)
  private TraceStatus status;

  @Column(name = "last_event_id", nullable = false, length = 100)
  private String lastEventId;

  @Column(name = "last_event_name", nullable = false, length = 150)
  private String lastEventName;

  @Enumerated(EnumType.STRING)
  @Column(name = "last_event_result", nullable = false, length = 20)
  private EventResult lastEventResult;

  @Column(name = "next_expected_event", length = 150)
  private String nextExpectedEvent;

  @Column(name = "waiting_since")
  private Instant waitingSince;

  @Column(name = "next_expected_before")
  private Instant nextExpectedBefore;

  @Column(name = "events_received", nullable = false)
  private Integer eventsReceived;

  @Column(name = "completed_at")
  private Instant completedAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Version
  @Column(name = "version", nullable = false)
  private Long version;
}
