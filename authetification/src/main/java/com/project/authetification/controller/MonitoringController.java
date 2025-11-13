package com.project.authetification.controller;

import com.project.authetification.model.MonitoringMetrics;
import com.project.authetification.model.VM;
import com.project.authetification.service.MonitoringService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/monitoring")
@RequiredArgsConstructor
public class MonitoringController {

    private final MonitoringService monitoringService;

    /**
     * Collecte les métriques depuis vRops, Prometheus, ou OSS Agents
     */
    @PostMapping("/metrics/collect")
    public ResponseEntity<MonitoringMetrics> collectMetrics(
            @RequestBody Map<String, Object> request) {
        try {
            String vmId = (String) request.get("vmId");
            String source = (String) request.get("source");
            Map<String, Object> metricsData = (Map<String, Object>) request.get("metricsData");

            if (vmId == null || source == null || metricsData == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }

            MonitoringMetrics metrics = monitoringService.collectMetrics(vmId, source, metricsData);
            return ResponseEntity.ok(metrics);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * Récupère les métriques d'une VM
     */
    @GetMapping("/metrics/vm/{vmId}")
    public ResponseEntity<List<MonitoringMetrics>> getMetricsByVM(@PathVariable String vmId) {
        List<MonitoringMetrics> metrics = monitoringService.getMetricsByVM(vmId);
        return ResponseEntity.ok(metrics);
    }

    /**
     * Récupère les métriques d'une VM dans une plage de dates
     */
    @GetMapping("/metrics/vm/{vmId}/range")
    public ResponseEntity<List<MonitoringMetrics>> getMetricsByVMAndDateRange(
            @PathVariable String vmId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        List<MonitoringMetrics> metrics = monitoringService.getMetricsByVMAndDateRange(vmId, start, end);
        return ResponseEntity.ok(metrics);
    }

    /**
     * Récupère les métriques par source
     */
    @GetMapping("/metrics/source/{source}")
    public ResponseEntity<List<MonitoringMetrics>> getMetricsBySource(@PathVariable String source) {
        List<MonitoringMetrics> metrics = monitoringService.getMetricsBySource(source);
        return ResponseEntity.ok(metrics);
    }

    /**
     * Récupère toutes les alertes
     */
    @GetMapping("/alerts")
    public ResponseEntity<List<MonitoringMetrics>> getAllAlerts() {
        List<MonitoringMetrics> alerts = monitoringService.getAllAlerts();
        return ResponseEntity.ok(alerts);
    }

    /**
     * Récupère les métriques récentes pour le tableau de bord
     */
    @GetMapping("/metrics/recent")
    public ResponseEntity<List<MonitoringMetrics>> getRecentMetrics(
            @RequestParam(defaultValue = "50") int limit) {
        List<MonitoringMetrics> metrics = monitoringService.getRecentMetrics(limit);
        return ResponseEntity.ok(metrics);
    }

    /**
     * Active le monitoring pour une VM
     */
    @PostMapping("/vms/{vmId}/enable")
    public ResponseEntity<VM> enableMonitoring(@PathVariable String vmId) {
        try {
            VM vm = monitoringService.enableMonitoring(vmId);
            return ResponseEntity.ok(vm);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * Désactive le monitoring pour une VM
     */
    @PostMapping("/vms/{vmId}/disable")
    public ResponseEntity<VM> disableMonitoring(@PathVariable String vmId) {
        try {
            VM vm = monitoringService.disableMonitoring(vmId);
            return ResponseEntity.ok(vm);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
}

