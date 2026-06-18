package com.clara.challenge.eventwatchdog.application;

import com.clara.challenge.eventwatchdog.domain.TraceStateTransitionService;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EventWatchdogConfiguration {

  @Bean
  Clock clock() {
    return Clock.systemUTC();
  }

  @Bean
  TraceStateTransitionService traceStateTransitionService() {
    return new TraceStateTransitionService();
  }
}
