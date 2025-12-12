package com.project.authetification.service;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.monitor.query.MetricsQueryClient;
import com.azure.monitor.query.MetricsQueryClientBuilder;
import com.azure.monitor.query.models.MetricResult;
import com.azure.monitor.query.models.MetricsQueryResult;
import com.project.authetification.model.MonitoringMetrics;
import com.project.authetification.model.VM;
import com.project.authetification.repository.MonitoringMetricsRepository;
import com.project.authetification.repository.VMRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MonitoringService {

    private final MonitoringMetricsRepository monitoringMetricsRepository;
    private final VMRepository vmRepository;

    // Client Azure Monitor (utilise l'identité managée du Pod ou les variables d'env)
    private final MetricsQueryClient metricsClient = new MetricsQueryClientBuilder()
            .credential(new DefaultAzureCredentialBuilder().build())
            .buildClient();

    // Ton ID de souscription (récupéré de tes logs précédents)
    private final String SUBSCRIPTION_ID = "aad8d067-b9af-4460-bba0-218009aa1031";

    /**
     * TÂCHE AUTOMATIQUE : Récupère les métriques Azure toutes les 5 minutes (300000ms)
     */
    @Scheduled(fixedRate = 300000)
    public void fetchMetricsFromAzureTask() {
        log.info(">>> Début de la collecte automatique des métriques Azure...");
        
        List<VM> allVms = vmRepository.findAll();

        for (VM vm : allVms) {
            // On ne monitore que les VMs qui sont censées être RUNNING
            if ("RUNNING".equalsIgnoreCase(vm.getStatus())) {
                try {
                    String resourceId = constructResourceId(vm);
                    log.debug("Interrogation Azure pour VM: {}", vm.getVmName());

                    Map<String, Object> azureData = queryAzureMetrics(resourceId);
                    
                    // Sauvegarde en base
                    collectMetrics(vm.getId(), "AzureMonitor", azureData);
                    
                } catch (Exception e) {
                    log.error("Erreur monitoring pour la VM {}: {}", vm.getVmName(), e.getMessage());
                }
            }
        }
        log.info(">>> Fin de la collecte des métriques.");
    }

    /**
     * Interroge l'API Azure Monitor pour une ressource donnée
     */
    private Map<String, Object> queryAzureMetrics(String resourceId) {
        Map<String, Object> data = new HashMap<>();
        try {
            // On demande le % CPU et le Réseau (Entrant/Sortant)
            MetricsQueryResult result = metricsClient.queryResource(
                resourceId,
                Arrays.asList("Percentage CPU", "Network In", "Network Out", "Disk Read Bytes", "Disk Write Bytes"), 
                null
            );

            for (MetricResult metric : result.getMetrics()) {
                // On prend la dernière valeur moyenne disponible
                metric.getTimeSeries().stream()
                    .flatMap(ts -> ts.getValues().stream())
                    .reduce((first, second) -> second) // Prend le dernier élément
                    .ifPresent(val -> {
                        String name = metric.getMetricName();
                        Double value = val.getAverage();
                        
                        if ("Percentage CPU".equals(name)) {
                            data.put("cpuUsage", value);
                        } else if ("Network In".equals(name) || "Network Out".equals(name)) {
                            // On somme in/out pour avoir une idée du débit
                            data.merge("networkThroughput", value, (a, b) -> (Double)a + (Double)b);
                        } else if ("Disk Read Bytes".equals(name) || "Disk Write Bytes".equals(name)) {
                             // On utilise ça comme proxy pour l'usage disque (activité)
                             // Note: Azure Monitor ne donne pas le % d'espace disque libre sans agent spécifique
                            data.merge("diskUsage", value > 0 ? 10.0 : 0.0, (a,b) -> (Double)a); 
                        }
                    });
            }
            
            data.put("isAvailable", true);
            data.put("vmStatus", "RUNNING");
            // uptime simulé si la VM répond (Azure ne donne pas l'uptime en secondes directement via metrics)
            data.put("uptime", 300); 

        } catch (Exception e) {
            log.warn("Impossible de lire les métriques Azure pour {}: {}", resourceId, e.getMessage());
            data.put("isAvailable", false);
            data.put("vmStatus", "UNREACHABLE");
        }
        return data;
    }

    /**
     * Reconstruit l'ID de la ressource Azure à partir des infos de la VM
     */
    private String constructResourceId(VM vm) {
        // Logique de nommage basée sur ton code Terraform
        // rgName = rg-{vm_name}-{demande_id}
        String rgName = "rg-" + vm.getVmName() + "-" + vm.getDemande().getId();
        
        return String.format(
            "/subscriptions/%s/resourceGroups/%s/providers/Microsoft.Compute/virtualMachines/%s",
            SUBSCRIPTION_ID, rgName, vm.getVmName()
        );
    }

    // --- TES MÉTHODES EXISTANTES (INCHANGÉES) ---

    public MonitoringMetrics collectMetrics(String vmId, String source, Map<String, Object> metricsData) {
        VM vm = vmRepository.findById(vmId)
                .orElseThrow(() -> new RuntimeException("VM not found: " + vmId));

        MonitoringMetrics metrics = new MonitoringMetrics();
        metrics.setVm(vm);
        metrics.setTimestamp(LocalDateTime.now());
        metrics.setSource(source);

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
            // Gestion sûre du cast en Integer
            Object uptimeObj = metricsData.get("uptime");
            if (uptimeObj instanceof Number) {
                metrics.setUptime(((Number) uptimeObj).intValue());
            }
        }

        metrics.setAdditionalMetrics(metricsData);
        checkAlerts(metrics);

        if (vm.getMonitored() == null || !vm.getMonitored()) {
            vm.setMonitored(true);
            vmRepository.save(vm);
        }

        return monitoringMetricsRepository.save(metrics);
    }

    private void checkAlerts(MonitoringMetrics metrics) {
        boolean hasAlert = false;
        StringBuilder alertMessage = new StringBuilder();

        if (metrics.getCpuUsage() != null && metrics.getCpuUsage() > 90) {
            hasAlert = true;
            alertMessage.append("CPU usage élevé: ").append(String.format("%.2f", metrics.getCpuUsage())).append("%. ");
        }

        if (metrics.getRamUsage() != null && metrics.getRamUsage() > 90) {
            hasAlert = true;
            alertMessage.append("RAM usage élevé: ").append(String.format("%.2f", metrics.getRamUsage())).append("%. ");
        }

        if (metrics.getIsAvailable() != null && !metrics.getIsAvailable()) {
            hasAlert = true;
            alertMessage.append("VM non disponible. ");
        }

        metrics.setHasAlerts(hasAlert);
        if (hasAlert) {
            metrics.setAlertMessage(alertMessage.toString());
        }
    }

    public List<MonitoringMetrics> getMetricsByVM(String vmId) {
        return monitoringMetricsRepository.findByVm_Id(vmId);
    }

    public List<MonitoringMetrics> getMetricsByVMAndDateRange(String vmId, LocalDateTime start, LocalDateTime end) {
        return monitoringMetricsRepository.findByVm_IdAndTimestampBetween(vmId, start, end);
    }

    public List<MonitoringMetrics> getMetricsBySource(String source) {
        return monitoringMetricsRepository.findBySource(source);
    }

    public List<MonitoringMetrics> getAllAlerts() {
        return monitoringMetricsRepository.findByHasAlertsTrue();
    }

    public List<MonitoringMetrics> getRecentMetrics(int limit) {
        List<MonitoringMetrics> allMetrics = monitoringMetricsRepository.findAll();
        return allMetrics.stream()
                .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                .limit(limit)
                .toList();
    }

    public VM enableMonitoring(String vmId) {
        VM vm = vmRepository.findById(vmId)
                .orElseThrow(() -> new RuntimeException("VM not found: " + vmId));
        vm.setMonitored(true);
        return vmRepository.save(vm);
    }

    public VM disableMonitoring(String vmId) {
        VM vm = vmRepository.findById(vmId)
                .orElseThrow(() -> new RuntimeException("VM not found: " + vmId));
        vm.setMonitored(false);
        return vmRepository.save(vm);
    }
}
