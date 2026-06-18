package com.clara.challenge.eventwatchdog.application;

import com.clara.challenge.eventwatchdog.domain.TraceStateSnapshot;

public record EventIngestionOutcome(TraceStateSnapshot state, boolean duplicate) {

  public EventIngestionOutcome {
    if (state == null) {
      throw new IllegalArgumentException("state must not be null");
    }
  }
}
