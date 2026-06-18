-- ============================================================
-- Clarops Challenge — Initial Schema
-- Distributed event tracking, TTL expiration, and operational
-- flow analysis.
-- Idempotent — safe to re-execute.
-- UUIDs must be provided by the application layer.
-- ============================================================
CREATE
  SCHEMA IF NOT EXISTS clarops_challenge_schema;
SET
search_path TO clarops_challenge_schema;

-- -------------------------
-- health
-- Single-row table used by the health endpoint.
-- -------------------------
CREATE
  TABLE
    IF NOT EXISTS health(
      id BIGSERIAL PRIMARY KEY,
      message VARCHAR(255) NOT NULL
    );

INSERT
  INTO
    health(message) SELECT
      'clarops sr engineer challenge'
    WHERE
      NOT EXISTS(
        SELECT
          1
        FROM
          health
      );

-- -------------------------
-- trace_events
-- Immutable event history used for audit and idempotency.
-- -------------------------
CREATE
  TABLE
    IF NOT EXISTS clarops_challenge_schema.trace_events(
      event_id VARCHAR(100) PRIMARY KEY,
      trace_id VARCHAR(100) NOT NULL,
      event_name VARCHAR(150) NOT NULL,
      result VARCHAR(20) NOT NULL,
      occurred_at TIMESTAMPTZ NOT NULL,
      received_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
      next_expected_event VARCHAR(150),
      next_event_ttl_seconds INTEGER,
      final_event BOOLEAN NOT NULL DEFAULT FALSE,
      metadata JSONB,
      CONSTRAINT chk_trace_events_result CHECK(
        result IN(
          'SUCCESS',
          'ERROR'
        )
      ),
      CONSTRAINT chk_trace_events_ttl_positive CHECK(
        next_event_ttl_seconds IS NULL
        OR next_event_ttl_seconds > 0
      ),
      CONSTRAINT chk_trace_events_expected_pair CHECK(
        (
          next_expected_event IS NULL
          AND next_event_ttl_seconds IS NULL
        )
        OR(
          next_expected_event IS NOT NULL
          AND next_event_ttl_seconds IS NOT NULL
        )
      )
    );

CREATE
  INDEX IF NOT EXISTS idx_trace_events_trace_id_occurred_at ON
  clarops_challenge_schema.trace_events(
    trace_id,
    occurred_at
  );

CREATE
  INDEX IF NOT EXISTS idx_trace_events_trace_id_received_at ON
  clarops_challenge_schema.trace_events(
    trace_id,
    received_at
  );

-- -------------------------
-- trace_states
-- Current derived state for efficient trace status lookup.
-- -------------------------
CREATE
  TABLE
    IF NOT EXISTS clarops_challenge_schema.trace_states(
      trace_id VARCHAR(100) PRIMARY KEY,
      status VARCHAR(40) NOT NULL,
      last_event_id VARCHAR(100) NOT NULL,
      last_event_name VARCHAR(150) NOT NULL,
      last_event_result VARCHAR(20) NOT NULL,
      next_expected_event VARCHAR(150),
      waiting_since TIMESTAMPTZ,
      next_expected_before TIMESTAMPTZ,
      events_received INTEGER NOT NULL,
      completed_at TIMESTAMPTZ,
      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
      updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
      version BIGINT NOT NULL DEFAULT 0,
      CONSTRAINT fk_trace_states_last_event FOREIGN KEY(last_event_id) REFERENCES clarops_challenge_schema.trace_events(event_id),
      CONSTRAINT chk_trace_states_status CHECK(
        status IN(
          'STARTED',
          'WAITING_OTHER_EVENT',
          'TTL_EXPIRED_FOR_EVENT',
          'COMPLETED'
        )
      ),
      CONSTRAINT chk_trace_states_last_event_result CHECK(
        last_event_result IN(
          'SUCCESS',
          'ERROR'
        )
      ),
      CONSTRAINT chk_trace_states_events_received_positive CHECK(
        events_received > 0
      ),
      CONSTRAINT chk_trace_states_waiting_expected CHECK(
        status <> 'WAITING_OTHER_EVENT'
        OR(
          next_expected_event IS NOT NULL
          AND next_expected_before IS NOT NULL
        )
      )
    );

CREATE
  INDEX IF NOT EXISTS idx_trace_states_status ON
  clarops_challenge_schema.trace_states(status);

CREATE
  INDEX IF NOT EXISTS idx_trace_states_next_expected_before ON
  clarops_challenge_schema.trace_states(next_expected_before);
