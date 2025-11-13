package com.project.authetification.service;

import com.project.authetification.model.Demande;
import com.project.authetification.model.GovernanceRule;
import com.project.authetification.model.MonitoringMetrics;
import com.project.authetification.model.User;
import com.project.authetification.model.VM;
import com.project.authetification.repository.DemandeRepository;
import com.project.authetification.repository.GovernanceRuleRepository;
import com.project.authetification.repository.MonitoringMetricsRepository;
import com.project.authetification.repository.UserRepository;
import com.project.authetification.repository.VMRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final DemandeRepository demandeRepository;
    private final VMRepository vmRepository;
    private final MonitoringMetricsRepository monitoringMetricsRepository;
    private final GovernanceRuleRepository governanceRuleRepository;
    private final UserRepository userRepository;

    /**
     * Récupère les statistiques du tableau de bord
     */
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        // Statistiques des demandes
        List<Demande> toutesDemandes = demandeRepository.findAll();
        stats.put("totalDemandes", toutesDemandes.size());
        stats.put("demandesEnAttente", demandeRepository.findByStatus("EN_ATTENTE").size());
        stats.put("demandesValidees", demandeRepository.findByStatus("VALIDE").size());
        stats.put("demandesEnProvisionnement", demandeRepository.findByStatus("EN_PROVISIONNEMENT").size());
        stats.put("demandesProvisionnees", demandeRepository.findByStatus("PROVISIONNEE").size());
        stats.put("demandesRefusees", demandeRepository.findByStatus("REFUSEE").size());

        // Statistiques des VMs
        List<VM> toutesVMs = vmRepository.findAll();
        stats.put("totalVMs", toutesVMs.size());
        stats.put("vmsRunning", vmRepository.findByStatus("RUNNING").size());
        stats.put("vmsStopped", vmRepository.findByStatus("STOPPED").size());
        stats.put("vmsMonitored", vmRepository.findByMonitored(true).size());

        // Statistiques de monitoring
        List<MonitoringMetrics> alerts = monitoringMetricsRepository.findByHasAlertsTrue();
        stats.put("totalAlerts", alerts.size());

        // Utilisation moyenne des ressources
        List<MonitoringMetrics> recentMetrics = monitoringMetricsRepository.findAll();
        if (!recentMetrics.isEmpty()) {
            double avgCpu = recentMetrics.stream()
                    .filter(m -> m.getCpuUsage() != null)
                    .mapToDouble(MonitoringMetrics::getCpuUsage)
                    .average()
                    .orElse(0.0);
            double avgRam = recentMetrics.stream()
                    .filter(m -> m.getRamUsage() != null)
                    .mapToDouble(MonitoringMetrics::getRamUsage)
                    .average()
                    .orElse(0.0);
            stats.put("avgCpuUsage", avgCpu);
            stats.put("avgRamUsage", avgRam);
        }

        return stats;
    }

    /**
     * Récupère toutes les règles de gouvernance
     */
    public List<GovernanceRule> getAllGovernanceRules() {
        return governanceRuleRepository.findAll();
    }

    /**
     * Récupère les règles de gouvernance actives
     */
    public List<GovernanceRule> getActiveGovernanceRules() {
        return governanceRuleRepository.findByIsActiveTrue();
    }

    /**
     * Crée une nouvelle règle de gouvernance
     */
    public GovernanceRule createGovernanceRule(GovernanceRule rule, String createdByUsername) {
        User createdBy = userRepository.findByUsername(createdByUsername)
                .orElseThrow(() -> new RuntimeException("User not found: " + createdByUsername));

        rule.setCreatedBy(createdBy);
        rule.setDateCreation(LocalDateTime.now());
        rule.setDateModification(LocalDateTime.now());
        if (rule.getIsActive() == null) {
            rule.setIsActive(true);
        }

        return governanceRuleRepository.save(rule);
    }

    /**
     * Met à jour une règle de gouvernance
     */
    public GovernanceRule updateGovernanceRule(String ruleId, GovernanceRule ruleDetails) {
        GovernanceRule rule = governanceRuleRepository.findById(ruleId)
                .orElseThrow(() -> new RuntimeException("Rule not found: " + ruleId));

        rule.setNom(ruleDetails.getNom());
        rule.setDescription(ruleDetails.getDescription());
        rule.setType(ruleDetails.getType());
        rule.setMaxRam(ruleDetails.getMaxRam());
        rule.setMaxCpu(ruleDetails.getMaxCpu());
        rule.setMaxDisk(ruleDetails.getMaxDisk());
        rule.setRequireFirewall(ruleDetails.getRequireFirewall());
        rule.setRequireEncryption(ruleDetails.getRequireEncryption());
        rule.setAllowedNetworks(ruleDetails.getAllowedNetworks());
        rule.setRestrictedNetworks(ruleDetails.getRestrictedNetworks());
        rule.setIsActive(ruleDetails.getIsActive());
        rule.setDateModification(LocalDateTime.now());

        return governanceRuleRepository.save(rule);
    }

    /**
     * Désactive une règle de gouvernance
     */
    public GovernanceRule disableGovernanceRule(String ruleId) {
        GovernanceRule rule = governanceRuleRepository.findById(ruleId)
                .orElseThrow(() -> new RuntimeException("Rule not found: " + ruleId));

        rule.setIsActive(false);
        rule.setDateModification(LocalDateTime.now());

        return governanceRuleRepository.save(rule);
    }

    /**
     * Récupère toutes les demandes
     */
    public List<Demande> getAllDemandes() {
        return demandeRepository.findAll();
    }

    /**
     * Récupère toutes les VMs
     */
    public List<VM> getAllVMs() {
        return vmRepository.findAll();
    }

    /**
     * Récupère les métriques de monitoring récentes
     */
    public List<MonitoringMetrics> getRecentMonitoringMetrics(int limit) {
        List<MonitoringMetrics> allMetrics = monitoringMetricsRepository.findAll();
        return allMetrics.stream()
                .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                .limit(limit)
                .toList();
    }

    /**
     * Récupère les alertes de monitoring
     */
    public List<MonitoringMetrics> getMonitoringAlerts() {
        return monitoringMetricsRepository.findByHasAlertsTrue();
    }

    /**
     * Récupère tous les utilisateurs
     */
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Met à jour le rôle d'un utilisateur
     */
    public User updateUserRole(String userId, List<String> roles) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        user.setRoles(roles);
        return userRepository.save(user);
    }
}

