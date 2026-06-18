package com.clara.challenge.eventwatchdog.domain;

public enum TraceConflictReason {
  TRACE_ALREADY_COMPLETED,
  TRACE_ALREADY_EXPIRED,
  UNEXPECTED_EVENT,
  LATE_EVENT
}
