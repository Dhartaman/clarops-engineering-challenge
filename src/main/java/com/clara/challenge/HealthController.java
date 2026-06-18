package com.clara.challenge;

import com.clara.challenge.health.HealthService;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Hidden
@RestController
@RequiredArgsConstructor
public class HealthController {

  private final HealthService healthService;

  @GetMapping("/health")
  public String index() {
    return healthService.getMessage();
  }
}
