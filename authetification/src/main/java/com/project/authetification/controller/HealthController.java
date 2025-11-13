package com.project.authetification.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*", maxAge = 3600)
public class HealthController {

    /**
     * Endpoint de santé pour vérifier que l'API fonctionne
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("message", "API is running");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint pour obtenir les informations de l'API
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> info() {
        Map<String, Object> response = new HashMap<>();
        response.put("name", "VM Provisioning API");
        response.put("version", "1.0.0");
        response.put("description", "API de gestion de provisionnement de machines virtuelles");
        response.put("endpoints", Map.of(
            "auth", "/api/auth",
            "demandes", "/api/demandes",
            "cloud-team", "/api/cloud-team",
            "support-system", "/api/support-system",
            "admin", "/api/admin",
            "monitoring", "/api/monitoring",
            "notifications", "/api/notifications"
        ));
        return ResponseEntity.ok(response);
    }
}

