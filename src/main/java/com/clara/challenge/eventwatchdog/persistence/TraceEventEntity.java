package com.clara.challenge.eventwatchdog.persistence;

import com.clara.challenge.eventwatchdog.domain.EventResult;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(schema = "clarops_challenge_schema", name = "trace_events")
public class TraceEventEntity {

  @Id
  @Column(name = "event_id", nullable = false, length = 100)
  private String eventId;

  @Column(name = "trace_id", nullable = false, length = 100)
  private String traceId;

  @Column(name = "event_name", nullable = false, length = 150)
  private String eventName;

  @Enumerated(EnumType.STRING)
  @Column(name = "event_result", nullable = false, length = 20)
  private EventResult result;

  @Column(name = "occurred_at", nullable = false)
  private Instant occurredAt;

  @Column(name = "received_at", nullable = false)
  private Instant receivedAt;

  @Column(name = "next_expected_event", length = 150)
  private String nextExpectedEvent;

  @Column(name = "next_event_ttl_seconds")
  private Integer nextEventTtlSeconds;

  @Column(name = "final_event", nullable = false)
  private boolean finalEvent;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "metadata", columnDefinition = "jsonb")
  private Map<String, Object> metadata;
}
