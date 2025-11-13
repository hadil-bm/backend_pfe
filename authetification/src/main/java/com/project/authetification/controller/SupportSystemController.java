package com.project.authetification.controller;

import com.project.authetification.model.VM;
import com.project.authetification.model.WorkOrder;
import com.project.authetification.service.SupportSystemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/support-system")
@RequiredArgsConstructor
public class SupportSystemController {

    private final SupportSystemService supportSystemService;

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return null;
    }

    /**
     * Récupère tous les workorders en attente
     */
    @GetMapping("/workorders/en-attente")
    public ResponseEntity<List<WorkOrder>> getWorkOrdersEnAttente() {
        List<WorkOrder> workOrders = supportSystemService.getWorkOrdersEnAttente();
        return ResponseEntity.ok(workOrders);
    }

    /**
     * Récupère tous les workorders assignés à l'utilisateur actuel
     */
    @GetMapping("/workorders/mes-workorders")
    public ResponseEntity<List<WorkOrder>> getMesWorkOrders() {
        String username = getCurrentUsername();
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        List<WorkOrder> workOrders = supportSystemService.getWorkOrdersByAssigne(username);
        return ResponseEntity.ok(workOrders);
    }

    /**
     * Assign un workorder à un membre de l'équipe Support
     */
    @PostMapping("/workorders/{id}/assigner")
    public ResponseEntity<WorkOrder> assignerWorkOrder(@PathVariable String id) {
        try {
            String assigneUsername = getCurrentUsername();
            if (assigneUsername == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            WorkOrder workOrder = supportSystemService.assignerWorkOrder(id, assigneUsername);
            return ResponseEntity.ok(workOrder);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * Démarre le provisionnement
     */
    @PostMapping("/workorders/{id}/demarrer")
    public ResponseEntity<WorkOrder> demarrerProvisionnement(@PathVariable String id) {
        try {
            WorkOrder workOrder = supportSystemService.demarrerProvisionnement(id);
            return ResponseEntity.ok(workOrder);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * Complète une étape de provisionnement
     */
    @PostMapping("/workorders/{id}/completer-etape")
    public ResponseEntity<WorkOrder> completerEtape(
            @PathVariable String id,
            @RequestBody Map<String, String> request) {
        try {
            String etape = request.get("etape");
            if (etape == null || etape.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }

            WorkOrder workOrder = supportSystemService.completerEtape(id, etape);
            return ResponseEntity.ok(workOrder);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * Crée une VM
     */
    @PostMapping("/workorders/{id}/creer-vm")
    public ResponseEntity<VM> creerVM(
            @PathVariable String id,
            @RequestBody Map<String, String> request) {
        try {
            String vmName = request.get("vmName");
            if (vmName == null || vmName.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }

            VM vm = supportSystemService.creerVM(id, vmName);
            return ResponseEntity.ok(vm);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * Finalise le provisionnement
     */
    @PostMapping("/workorders/{id}/finaliser")
    public ResponseEntity<WorkOrder> finaliserProvisionnement(
            @PathVariable String id,
            @RequestBody Map<String, String> request) {
        try {
            String resultat = request.get("resultat");
            if (resultat == null) {
                resultat = "Provisionnement terminé avec succès";
            }

            WorkOrder workOrder = supportSystemService.finaliserProvisionnement(id, resultat);
            return ResponseEntity.ok(workOrder);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * Marque une erreur dans le provisionnement
     */
    @PostMapping("/workorders/{id}/erreur")
    public ResponseEntity<WorkOrder> marquerErreur(
            @PathVariable String id,
            @RequestBody Map<String, String> request) {
        try {
            String erreur = request.get("erreur");
            if (erreur == null || erreur.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }

            WorkOrder workOrder = supportSystemService.marquerErreur(id, erreur);
            return ResponseEntity.ok(workOrder);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * Récupère toutes les VMs
     */
    @GetMapping("/vms")
    public ResponseEntity<List<VM>> getAllVMs() {
        List<VM> vms = supportSystemService.getAllVMs();
        return ResponseEntity.ok(vms);
    }

    /**
     * Récupère les VMs d'une demande
     */
    @GetMapping("/vms/demande/{demandeId}")
    public ResponseEntity<List<VM>> getVMsByDemande(@PathVariable String demandeId) {
        List<VM> vms = supportSystemService.getVMsByDemande(demandeId);
        return ResponseEntity.ok(vms);
    }

    /**
     * Met à jour le statut d'une VM
     */
    @PutMapping("/vms/{id}/status")
    public ResponseEntity<VM> updateVMStatus(
            @PathVariable String id,
            @RequestBody Map<String, String> request) {
        try {
            String status = request.get("status");
            if (status == null || status.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }

            VM vm = supportSystemService.updateVMStatus(id, status);
            return ResponseEntity.ok(vm);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
}

