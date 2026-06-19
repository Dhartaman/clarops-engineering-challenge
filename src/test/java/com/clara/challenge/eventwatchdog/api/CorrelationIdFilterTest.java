package com.clara.challenge.eventwatchdog.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CorrelationIdFilterTest {

  private final CorrelationIdFilter filter = new CorrelationIdFilter();

  @AfterEach
  void clearMdc() {
    MDC.clear();
  }

  @Test
  void shouldReturnProvidedCorrelationIdAndClearMdc_WhenHeaderIsPresent() throws Exception {
    // Arrange
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, "correlation-123");
    MockHttpServletResponse response = new MockHttpServletResponse();
    AtomicReference<String> correlationIdInsideChain = new AtomicReference<>();

    // Act
    filter.doFilter(
        request,
        response,
        (servletRequest, servletResponse) ->
            correlationIdInsideChain.set(MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY)));

    // Assert
    assertEquals("correlation-123", response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER));
    assertEquals("correlation-123", correlationIdInsideChain.get());
    assertNull(MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY));
  }

  @Test
  void shouldGenerateCorrelationIdAndClearMdc_WhenHeaderIsMissing() throws Exception {
    // Arrange
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    AtomicReference<String> correlationIdInsideChain = new AtomicReference<>();

    // Act
    filter.doFilter(
        request,
        response,
        (servletRequest, servletResponse) ->
            correlationIdInsideChain.set(MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY)));

    // Assert
    String generatedCorrelationId = response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER);
    assertFalse(generatedCorrelationId.isBlank());
    UUID.fromString(generatedCorrelationId);
    assertEquals(generatedCorrelationId, correlationIdInsideChain.get());
    assertNull(MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY));
  }

  @Test
  void shouldGenerateCorrelationId_WhenHeaderIsBlank() throws Exception {
    // Arrange
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, "   ");
    MockHttpServletResponse response = new MockHttpServletResponse();

    // Act
    filter.doFilter(request, response, (servletRequest, servletResponse) -> {});

    // Assert
    String generatedCorrelationId = response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER);
    assertFalse(generatedCorrelationId.isBlank());
    UUID.fromString(generatedCorrelationId);
    assertNull(MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY));
  }
}
