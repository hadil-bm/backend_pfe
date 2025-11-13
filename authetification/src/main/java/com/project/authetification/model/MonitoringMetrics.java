package com.project.authetification.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

@Document(collection = "monitoring_metrics")
@Data
public class MonitoringMetrics {
    
    @Id
    private String id;
    
    @DBRef
    private VM vm; // VM concernée
    
    private LocalDateTime timestamp;
    private String source; // vRops, Prometheus, OSS Agents
    
    // Métriques de performance
    private Double cpuUsage; // Pourcentage d'utilisation CPU
    private Double ramUsage; // Pourcentage d'utilisation RAM
    private Double diskUsage; // Pourcentage d'utilisation disque
    private Double networkLatency; // Latence réseau en ms
    private Double networkThroughput; // Débit réseau en MB/s
    
    // Métriques système
    private String vmStatus; // État de la VM
    private Boolean isAvailable; // Disponibilité
    private Integer uptime; // Temps de fonctionnement en secondes
    
    // Métriques supplémentaires (format clé-valeur)
    private Map<String, Object> additionalMetrics;
    
    // Alertes
    private Boolean hasAlerts;
    private String alertMessage;
}

