package com.clara.challenge.eventwatchdog.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TraceEventRepository extends JpaRepository<TraceEventEntity, String> {}
