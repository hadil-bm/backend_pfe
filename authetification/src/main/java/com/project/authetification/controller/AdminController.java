package com.project.authetification.controller;

import com.project.authetification.model.Demande;
import com.project.authetification.model.GovernanceRule;
import com.project.authetification.model.MonitoringMetrics;
import com.project.authetification.model.User;
import com.project.authetification.model.VM;
import com.project.authetification.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return null;
    }

    /**
     * Récupère les statistiques du tableau de bord
     */
    @GetMapping("/dashboard/stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        Map<String, Object> stats = adminService.getDashboardStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * Récupère toutes les règles de gouvernance
     */
    @GetMapping("/governance-rules")
    public ResponseEntity<List<GovernanceRule>> getAllGovernanceRules() {
        List<GovernanceRule> rules = adminService.getAllGovernanceRules();
        return ResponseEntity.ok(rules);
    }

    /**
     * Récupère les règles de gouvernance actives
     */
    @GetMapping("/governance-rules/active")
    public ResponseEntity<List<GovernanceRule>> getActiveGovernanceRules() {
        List<GovernanceRule> rules = adminService.getActiveGovernanceRules();
        return ResponseEntity.ok(rules);
    }

    /**
     * Crée une nouvelle règle de gouvernance
     */
    @PostMapping("/governance-rules")
    public ResponseEntity<GovernanceRule> createGovernanceRule(@RequestBody GovernanceRule rule) {
        try {
            String createdByUsername = getCurrentUsername();
            if (createdByUsername == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            GovernanceRule createdRule = adminService.createGovernanceRule(rule, createdByUsername);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdRule);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * Met à jour une règle de gouvernance
     */
    @PutMapping("/governance-rules/{id}")
    public ResponseEntity<GovernanceRule> updateGovernanceRule(
            @PathVariable String id,
            @RequestBody GovernanceRule ruleDetails) {
        try {
            GovernanceRule updatedRule = adminService.updateGovernanceRule(id, ruleDetails);
            return ResponseEntity.ok(updatedRule);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * Désactive une règle de gouvernance
     */
    @PutMapping("/governance-rules/{id}/disable")
    public ResponseEntity<GovernanceRule> disableGovernanceRule(@PathVariable String id) {
        try {
            GovernanceRule rule = adminService.disableGovernanceRule(id);
            return ResponseEntity.ok(rule);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * Récupère toutes les demandes
     */
    @GetMapping("/demandes")
    public ResponseEntity<List<Demande>> getAllDemandes() {
        List<Demande> demandes = adminService.getAllDemandes();
        return ResponseEntity.ok(demandes);
    }

    /**
     * Récupère toutes les VMs
     */
    @GetMapping("/vms")
    public ResponseEntity<List<VM>> getAllVMs() {
        List<VM> vms = adminService.getAllVMs();
        return ResponseEntity.ok(vms);
    }

    /**
     * Récupère les métriques de monitoring récentes
     */
    @GetMapping("/monitoring/metrics/recent")
    public ResponseEntity<List<MonitoringMetrics>> getRecentMonitoringMetrics(
            @RequestParam(defaultValue = "50") int limit) {
        List<MonitoringMetrics> metrics = adminService.getRecentMonitoringMetrics(limit);
        return ResponseEntity.ok(metrics);
    }

    /**
     * Récupère les alertes de monitoring
     */
    @GetMapping("/monitoring/alerts")
    public ResponseEntity<List<MonitoringMetrics>> getMonitoringAlerts() {
        List<MonitoringMetrics> alerts = adminService.getMonitoringAlerts();
        return ResponseEntity.ok(alerts);
    }

    /**
     * Récupère tous les utilisateurs
     */
    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = adminService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    /**
     * Met à jour le rôle d'un utilisateur
     */
    @PutMapping("/users/{id}/roles")
    public ResponseEntity<User> updateUserRole(
            @PathVariable String id,
            @RequestBody Map<String, List<String>> request) {
        try {
            List<String> roles = request.get("roles");
            if (roles == null || roles.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }

            User user = adminService.updateUserRole(id, roles);
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
}

