package com.project.authetification.controller;

import com.project.authetification.model.Demande;
// Import de ton entité utilisée par TerraformService
import com.project.authetification.model.DemandeVM; 
import com.project.authetification.model.DemandeStatus; // Si tu as cet enum
import com.project.authetification.service.DemandeService;
// Import du service Terraform (celui qu'on a validé ensemble)
import com.project.authetification.service.TerraformService; 

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/demandes") // J'ai généralisé un peu le path, tu peux garder /demandeur si tu veux
@RequiredArgsConstructor
public class DemandeController {

    private final DemandeService demandeService;
    private final TerraformService terraformService; // <--- L'ajout CRUCIAL

    // --- Méthodes utilitaires ---
    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return null;
    }

    // --- Endpoints Demandeur ---

    @PostMapping("/demandeur/create")
    public ResponseEntity<Demande> createDemande(@RequestBody Demande demandeDetails) {
        String demandeurUsername = getCurrentUsername();
        if (demandeurUsername == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        Demande nouvelleDemande = demandeService.createDemande(demandeurUsername, demandeDetails);
        return new ResponseEntity<>(nouvelleDemande, HttpStatus.CREATED);
    }

    @GetMapping("/demandeur/{id}")
    public ResponseEntity<Demande> getDemandeById(@PathVariable String id) {
        Optional<Demande> demande = demandeService.getDemandeById(id);
        return demande.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/demandeur/mes-demandes")
    public ResponseEntity<List<Demande>> getMesDemandes() {
        String demandeurUsername = getCurrentUsername();
        if (demandeurUsername == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        List<Demande> mesDemandes = demandeService.getDemandesByDemandeur(demandeurUsername);
        return ResponseEntity.ok(mesDemandes);
    }

    @PutMapping("/demandeur/modifier/{id}")
    public ResponseEntity<Demande> modifierDemande(@PathVariable String id, @RequestBody Demande demandeDetails) {
        String demandeurUsername = getCurrentUsername();
        if (demandeurUsername == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        try {
            Optional<Demande> existingDemande = demandeService.getDemandeById(id);
            if (existingDemande.isPresent()) {
                String status = existingDemande.get().getStatus();
                if (!"EN_ATTENTE".equals(status) && !"A_MODIFIER".equals(status)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
                }
            }
            Demande updatedDemande = demandeService.updateDemande(id, demandeurUsername, demandeDetails);
            return ResponseEntity.ok(updatedDemande);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }
    }

    @DeleteMapping("/demandeur/supprimer/{id}")
    public ResponseEntity<Void> supprimerDemande(@PathVariable String id) {
        String demandeurUsername = getCurrentUsername();
        if (demandeurUsername == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        try {
            demandeService.deleteDemande(id, demandeurUsername);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    // --- NOUVEAU : Endpoint pour le SUPPORT (Validation + Terraform) ---

    @PutMapping("/support/validate-support/{id}")
    public ResponseEntity<?> validateAndProvision(@PathVariable String id) {
        log.info("Validation Support demandée pour l'ID : {}", id);

        Optional<Demande> demandeOpt = demandeService.getDemandeById(id);
        
        if (demandeOpt.isPresent()) {
            Demande demande = demandeOpt.get();
            
            // 1. Mise à jour du statut dans ta base principale
            demande.setStatus("APPROVED"); 
            // Si tu as une méthode updateStatus dans service, utilise-la, sinon save direct
            // demandeService.updateDemandeStatus(id, "APPROVED"); 
            // Pour l'instant on suppose que l'objet est mis à jour :
            demandeRepositoryUpdateSimul(demande); // Adapte selon ton Service

            // 2. Préparation des données pour Terraform
            // On convertit 'Demande' (String ID) vers 'DemandeVM' (Long ID) que TerraformService attend
            // Si ton ID est un UUID String, on va utiliser un hash ou un ID temporaire pour le nom de VM
            
            DemandeVM vmData = new DemandeVM();
            // On utilise le hashcode de l'ID String pour avoir un nombre, ou 1 par défaut
            vmData.setId((long) Math.abs(id.hashCode()));
            
            // Conversion des valeurs (Adapte selon tes types réels dans Demande)
            vmData.setCpu(parseInteger(demande.getCpu()));
            vmData.setRam(parseInteger(demande.getRam()));
            vmData.setOsType(demande.getOs()); // ou demande.getOsType()
            vmData.setStatus(DemandeStatus.APPROVED);

            // 3. Lancement de Terraform (Async)
            terraformService.triggerVmCreation(vmData);

            return ResponseEntity.ok("Demande validée. Provisioning VM lancé en arrière-plan.");
        }
        
        return ResponseEntity.notFound().build();
    }

    // --- Méthodes Helpers pour la conversion ---
    
    // Juste pour simuler la sauvegarde si ta méthode n'est pas publique
    private void demandeRepositoryUpdateSimul(Demande d) {
        try {
            demandeService.updateDemandeStatus(d.getId(), d.getStatus());
        } catch (Exception e) {
            log.warn("Erreur update status via service: " + e.getMessage());
        }
    }

    private int parseInteger(String value) {
        try {
            if (value == null) return 1;
            // Extrait les chiffres d'une chaine comme "4GB" -> 4
            return Integer.parseInt(value.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return 1; // Valeur par défaut
        }
    }
}
