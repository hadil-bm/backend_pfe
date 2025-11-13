package com.project.authetification.controller;

import com.project.authetification.model.Demande;
import com.project.authetification.service.CloudTeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cloud-team")
@RequiredArgsConstructor
public class CloudTeamController {

    private final CloudTeamService cloudTeamService;

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return null;
    }

    /**
     * Récupère toutes les demandes en attente de validation
     */
    @GetMapping("/demandes/en-attente")
    public ResponseEntity<List<Demande>> getDemandesEnAttente() {
        List<Demande> demandes = cloudTeamService.getDemandesEnAttente();
        return ResponseEntity.ok(demandes);
    }

    /**
     * Récupère toutes les demandes en cours de validation
     */
    @GetMapping("/demandes/en-validation")
    public ResponseEntity<List<Demande>> getDemandesEnValidation() {
        List<Demande> demandes = cloudTeamService.getDemandesEnValidation();
        return ResponseEntity.ok(demandes);
    }

    /**
     * Valide une demande
     */
    @PostMapping("/demandes/{id}/valider")
    public ResponseEntity<Demande> validerDemande(
            @PathVariable String id,
            @RequestBody Map<String, String> validationData) {
        try {
            String validateurUsername = getCurrentUsername();
            if (validateurUsername == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            String adresseIp = validationData.get("adresseIp");
            String reseau = validationData.get("reseau");
            String datastore = validationData.get("datastore");

            Demande demande = cloudTeamService.validerDemande(id, validateurUsername, adresseIp, reseau, datastore);
            return ResponseEntity.ok(demande);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * Refuse une demande
     */
    @PostMapping("/demandes/{id}/refuser")
    public ResponseEntity<Demande> refuserDemande(
            @PathVariable String id,
            @RequestBody Map<String, String> refusalData) {
        try {
            String validateurUsername = getCurrentUsername();
            if (validateurUsername == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            String justification = refusalData.get("justification");
            if (justification == null || justification.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }

            Demande demande = cloudTeamService.refuserDemande(id, validateurUsername, justification);
            return ResponseEntity.ok(demande);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * Demande des modifications à une demande
     */
    @PostMapping("/demandes/{id}/demander-modification")
    public ResponseEntity<Demande> demanderModification(
            @PathVariable String id,
            @RequestBody Map<String, String> modificationData) {
        try {
            String validateurUsername = getCurrentUsername();
            if (validateurUsername == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            String justification = modificationData.get("justification");
            if (justification == null || justification.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }

            Demande demande = cloudTeamService.demanderModification(id, validateurUsername, justification);
            return ResponseEntity.ok(demande);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
}

