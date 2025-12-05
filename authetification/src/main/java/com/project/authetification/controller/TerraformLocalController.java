package com.project.authetification.controller;

import com.project.authetification.model.TerraformExecution;
import com.project.authetification.service.TerraformLocalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/terraform/local")
@RequiredArgsConstructor
public class TerraformLocalController {

    private final TerraformLocalService terraformLocalService;

    /**
     * Crée une VM localement via Terraform
     */
    @PostMapping("/workorders/{workOrderId}/create-vm")
    public ResponseEntity<Map<String, Object>> createVMLocally(@PathVariable String workOrderId) {
        try {
            TerraformExecution execution = terraformLocalService.createVMLocally(workOrderId);
            return ResponseEntity.ok(Map.of(
                    "status", "started",
                    "executionId", execution.getId(),
                    "message", "Création de VM démarrée. L'exécution Terraform est en cours."
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Vérifie le statut d'une exécution Terraform
     */
    @GetMapping("/executions/{executionId}/status")
    public ResponseEntity<TerraformExecution> checkStatus(@PathVariable String executionId) {
        // Cette méthode nécessiterait d'ajouter une méthode dans TerraformLocalService
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }
}

