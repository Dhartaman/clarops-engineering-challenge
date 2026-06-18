package com.clara.challenge.eventwatchdog.error;

public class BusinessConflictException extends RuntimeException {

  private final String code;

  public BusinessConflictException(String code, String message) {
    super(message);
    if (code == null || code.isBlank()) {
      throw new IllegalArgumentException("code must not be blank");
    }
    this.code = code;
  }

  public String code() {
    return code;
  }
}
