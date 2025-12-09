package com.project.authetification.controller;

import com.project.authetification.model.*;
import com.project.authetification.service.SupportSystemService;
import com.project.authetification.service.TerraformService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/support-system")
@RequiredArgsConstructor
public class SupportSystemController {

    private final SupportSystemService supportSystemService;
    private final TerraformService terraformService;

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return null;
    }

    @GetMapping("/workorders/en-attente")
    public ResponseEntity<List<WorkOrder>> getWorkOrdersEnAttente() {
        return ResponseEntity.ok(supportSystemService.getWorkOrdersEnAttente());
    }

    @GetMapping("/workorders/mes-workorders")
    public ResponseEntity<List<WorkOrder>> getMesWorkOrders() {
        String username = getCurrentUsername();
        if (username == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(supportSystemService.getWorkOrdersByAssigne(username));
    }

    @PostMapping("/workorders/{id}/assigner")
    public ResponseEntity<WorkOrder> assignerWorkOrder(@PathVariable String id) {
        try {
            String username = getCurrentUsername();
            if (username == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            return ResponseEntity.ok(supportSystemService.assignerWorkOrder(id, username));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    // --- LE PLUS IMPORTANT : CREATION VM + TERRAFORM ---
    @PostMapping("/workorders/{id}/creer-vm")
    public ResponseEntity<?> creerVM(@PathVariable String id, @RequestBody Map<String, String> request) {
        try {
            String vmName = request.get("vmName");
            if (vmName == null || vmName.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Le nom de la VM est obligatoire");
            }

            log.info(">>> [Controller] Demande création VM pour WorkOrder: {}", id);

            // 1. Créer la VM en base de données (Status: RUNNING ou PENDING)
            VM vm = supportSystemService.creerVM(id, vmName);

            // 2. Préparer l'objet pour Terraform
            DemandeVM terraformData = new DemandeVM();
            // On utilise le hashCode de l'ID string pour avoir un Long (car Terraform préfère des IDs simples parfois)
            // Ou mieux: Utilise directement l'ID de la demande associée si disponible
            terraformData.setId((long) Math.abs(vm.getId().hashCode())); 
            terraformData.setCpu(parseInteger(vm.getCpu()));
            terraformData.setRam(parseInteger(vm.getRam()));
            terraformData.setOsType(vm.getOs());
            terraformData.setStatus(DemandeStatus.APPROVED);

            log.info(">>> [Controller] Déclenchement Terraform Async...");
            
            // 3. Lancer Terraform en arrière-plan
            terraformService.triggerVmCreation(terraformData);

            return ResponseEntity.ok(vm);

        } catch (Exception e) {
            log.error("Erreur Création VM", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    private int parseInteger(String value) {
        try {
            if (value == null) return 1;
            String numbers = value.replaceAll("[^0-9]", "");
            return numbers.isEmpty() ? 1 : Integer.parseInt(numbers);
        } catch (Exception e) {
            return 1;
        }
    }
}
