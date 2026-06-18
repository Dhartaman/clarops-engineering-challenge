package com.clara.challenge.eventwatchdog.api;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info =
        @Info(
            title = "ClarOps Distributed Event Watchdog API",
            description = "API for receiving distributed flow events and querying trace status.",
            version = "0.0.1"))
public class OpenApiConfiguration {}
