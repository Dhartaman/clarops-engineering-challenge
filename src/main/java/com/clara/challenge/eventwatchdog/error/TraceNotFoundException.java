package com.clara.challenge.eventwatchdog.error;

public class TraceNotFoundException extends RuntimeException {

  private final String traceId;

  public TraceNotFoundException(String traceId) {
    super("Trace not found: " + traceId);
    this.traceId = traceId;
  }

  public String traceId() {
    return traceId;
  }
}
