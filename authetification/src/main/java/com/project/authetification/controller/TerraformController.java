package com.project.authetification.controller;

import com.project.authetification.model.TerraformExecution;
import com.project.authetification.service.TerraformService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/terraform")
@RequiredArgsConstructor
public class TerraformController {

    private final TerraformService terraformService;

    /**
     * Crée une exécution Terraform manuellement
     */
    @PostMapping("/workorders/{workOrderId}/execute")
    public ResponseEntity<TerraformExecution> executeTerraform(@PathVariable String workOrderId) {
        try {
            TerraformExecution execution = terraformService.createTerraformRun(workOrderId);
            return ResponseEntity.ok(execution);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * Vérifie le statut d'une exécution Terraform
     */
    @GetMapping("/executions/{executionId}/status")
    public ResponseEntity<TerraformExecution> checkStatus(@PathVariable String executionId) {
        try {
            TerraformExecution execution = terraformService.checkExecutionStatus(executionId);
            return ResponseEntity.ok(execution);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * Récupère toutes les exécutions Terraform d'un workorder
     */
    @GetMapping("/workorders/{workOrderId}/executions")
    public ResponseEntity<List<TerraformExecution>> getExecutionsByWorkOrder(@PathVariable String workOrderId) {
        // Cette méthode nécessiterait d'ajouter une méthode dans TerraformService
        return ResponseEntity.ok(List.of());
    }
}

