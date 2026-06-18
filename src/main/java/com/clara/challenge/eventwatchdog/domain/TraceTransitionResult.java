package com.clara.challenge.eventwatchdog.domain;

public sealed interface TraceTransitionResult
    permits TraceTransitionResult.Accepted, TraceTransitionResult.Conflict {

  record Accepted(TraceStateSnapshot state) implements TraceTransitionResult {

    public Accepted {
      if (state == null) {
        throw new IllegalArgumentException("state must not be null");
      }
    }
  }

  record Conflict(TraceConflictReason reason, String message) implements TraceTransitionResult {

    public Conflict {
      if (reason == null) {
        throw new IllegalArgumentException("reason must not be null");
      }
      if (message == null || message.isBlank()) {
        throw new IllegalArgumentException("message must not be blank");
      }
    }
  }
}
