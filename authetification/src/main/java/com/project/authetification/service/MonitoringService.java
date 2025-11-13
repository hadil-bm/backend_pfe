package com.project.authetification.service;

import com.project.authetification.model.MonitoringMetrics;
import com.project.authetification.model.VM;
import com.project.authetification.repository.MonitoringMetricsRepository;
import com.project.authetification.repository.VMRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MonitoringService {

    private final MonitoringMetricsRepository monitoringMetricsRepository;
    private final VMRepository vmRepository;

    /**
     * Collecte les métriques depuis vRops, Prometheus, ou OSS Agents
     */
    public MonitoringMetrics collectMetrics(String vmId, String source, Map<String, Object> metricsData) {
        VM vm = vmRepository.findById(vmId)
                .orElseThrow(() -> new RuntimeException("VM not found: " + vmId));

        MonitoringMetrics metrics = new MonitoringMetrics();
        metrics.setVm(vm);
        metrics.setTimestamp(LocalDateTime.now());
        metrics.setSource(source);

        // Extraire les métriques depuis les données
        if (metricsData.containsKey("cpuUsage")) {
            metrics.setCpuUsage(Double.parseDouble(metricsData.get("cpuUsage").toString()));
        }
        if (metricsData.containsKey("ramUsage")) {
            metrics.setRamUsage(Double.parseDouble(metricsData.get("ramUsage").toString()));
        }
        if (metricsData.containsKey("diskUsage")) {
            metrics.setDiskUsage(Double.parseDouble(metricsData.get("diskUsage").toString()));
        }
        if (metricsData.containsKey("networkLatency")) {
            metrics.setNetworkLatency(Double.parseDouble(metricsData.get("networkLatency").toString()));
        }
        if (metricsData.containsKey("networkThroughput")) {
            metrics.setNetworkThroughput(Double.parseDouble(metricsData.get("networkThroughput").toString()));
        }
        if (metricsData.containsKey("vmStatus")) {
            metrics.setVmStatus(metricsData.get("vmStatus").toString());
        }
        if (metricsData.containsKey("isAvailable")) {
            metrics.setIsAvailable(Boolean.parseBoolean(metricsData.get("isAvailable").toString()));
        }
        if (metricsData.containsKey("uptime")) {
            metrics.setUptime(Integer.parseInt(metricsData.get("uptime").toString()));
        }

        // Stocker les métriques supplémentaires
        metrics.setAdditionalMetrics(metricsData);

        // Vérifier les alertes
        checkAlerts(metrics);

        // Activer le monitoring pour la VM si ce n'est pas déjà fait
        if (vm.getMonitored() == null || !vm.getMonitored()) {
            vm.setMonitored(true);
            vmRepository.save(vm);
        }

        return monitoringMetricsRepository.save(metrics);
    }

    /**
     * Vérifie les alertes basées sur les métriques
     */
    private void checkAlerts(MonitoringMetrics metrics) {
        boolean hasAlert = false;
        StringBuilder alertMessage = new StringBuilder();

        // Vérifier l'utilisation CPU
        if (metrics.getCpuUsage() != null && metrics.getCpuUsage() > 90) {
            hasAlert = true;
            alertMessage.append("CPU usage élevé: ").append(metrics.getCpuUsage()).append("%. ");
        }

        // Vérifier l'utilisation RAM
        if (metrics.getRamUsage() != null && metrics.getRamUsage() > 90) {
            hasAlert = true;
            alertMessage.append("RAM usage élevé: ").append(metrics.getRamUsage()).append("%. ");
        }

        // Vérifier l'utilisation disque
        if (metrics.getDiskUsage() != null && metrics.getDiskUsage() > 90) {
            hasAlert = true;
            alertMessage.append("Disk usage élevé: ").append(metrics.getDiskUsage()).append("%. ");
        }

        // Vérifier la disponibilité
        if (metrics.getIsAvailable() != null && !metrics.getIsAvailable()) {
            hasAlert = true;
            alertMessage.append("VM non disponible. ");
        }

        // Vérifier la latence réseau
        if (metrics.getNetworkLatency() != null && metrics.getNetworkLatency() > 100) {
            hasAlert = true;
            alertMessage.append("Latence réseau élevée: ").append(metrics.getNetworkLatency()).append("ms. ");
        }

        metrics.setHasAlerts(hasAlert);
        if (hasAlert) {
            metrics.setAlertMessage(alertMessage.toString());
        }
    }

    /**
     * Récupère les métriques d'une VM
     */
    public List<MonitoringMetrics> getMetricsByVM(String vmId) {
        return monitoringMetricsRepository.findByVm_Id(vmId);
    }

    /**
     * Récupère les métriques d'une VM dans une plage de dates
     */
    public List<MonitoringMetrics> getMetricsByVMAndDateRange(String vmId, LocalDateTime start, LocalDateTime end) {
        return monitoringMetricsRepository.findByVm_IdAndTimestampBetween(vmId, start, end);
    }

    /**
     * Récupère les métriques par source
     */
    public List<MonitoringMetrics> getMetricsBySource(String source) {
        return monitoringMetricsRepository.findBySource(source);
    }

    /**
     * Récupère toutes les alertes
     */
    public List<MonitoringMetrics> getAllAlerts() {
        return monitoringMetricsRepository.findByHasAlertsTrue();
    }

    /**
     * Récupère les métriques récentes pour le tableau de bord
     */
    public List<MonitoringMetrics> getRecentMetrics(int limit) {
        List<MonitoringMetrics> allMetrics = monitoringMetricsRepository.findAll();
        return allMetrics.stream()
                .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                .limit(limit)
                .toList();
    }

    /**
     * Active le monitoring pour une VM
     */
    public VM enableMonitoring(String vmId) {
        VM vm = vmRepository.findById(vmId)
                .orElseThrow(() -> new RuntimeException("VM not found: " + vmId));

        vm.setMonitored(true);
        return vmRepository.save(vm);
    }

    /**
     * Désactive le monitoring pour une VM
     */
    public VM disableMonitoring(String vmId) {
        VM vm = vmRepository.findById(vmId)
                .orElseThrow(() -> new RuntimeException("VM not found: " + vmId));

        vm.setMonitored(false);
        return vmRepository.save(vm);
    }
}

