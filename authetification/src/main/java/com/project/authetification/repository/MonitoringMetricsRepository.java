package com.project.authetification.repository;

import com.project.authetification.model.MonitoringMetrics;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MonitoringMetricsRepository extends MongoRepository<MonitoringMetrics, String> {
    List<MonitoringMetrics> findByVm_Id(String vmId);
    List<MonitoringMetrics> findByVm_IdAndTimestampBetween(String vmId, LocalDateTime start, LocalDateTime end);
    List<MonitoringMetrics> findBySource(String source);
    List<MonitoringMetrics> findByHasAlertsTrue();
}

