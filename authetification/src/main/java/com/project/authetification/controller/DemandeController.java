package com.project.authetification.controller;

import com.project.authetification.model.Demande;
import com.project.authetification.service.DemandeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/demandes/demandeur")
@RequiredArgsConstructor
public class DemandeController {

    private final DemandeService demandeService;

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return null;
    }

    @PostMapping("/create")
    public ResponseEntity<Demande> createDemande(@RequestBody Demande demandeDetails) {
        String demandeurUsername = getCurrentUsername();
        if (demandeurUsername == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        Demande nouvelleDemande = demandeService.createDemande(demandeurUsername, demandeDetails);
        return new ResponseEntity<>(nouvelleDemande, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Demande> getDemandeById(@PathVariable String id) {
        Optional<Demande> demande = demandeService.getDemandeById(id);
        return demande.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/mes-demandes")
    public ResponseEntity<List<Demande>> getMesDemandes() {
        String demandeurUsername = getCurrentUsername();
        if (demandeurUsername == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        List<Demande> mesDemandes = demandeService.getDemandesByDemandeur(demandeurUsername);
        return ResponseEntity.ok(mesDemandes);
    }

    @PutMapping("/modifier/{id}")
    public ResponseEntity<Demande> modifierDemande(@PathVariable String id, @RequestBody Demande demandeDetails) {
        String demandeurUsername = getCurrentUsername();
        if (demandeurUsername == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        try {
            Demande updatedDemande = demandeService.updateDemande(id, demandeurUsername, demandeDetails);
            return ResponseEntity.ok(updatedDemande);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null); // Or another appropriate status
        }
    }

    // Endpoint for updating the status of a demande (likely for an admin role)
    @PutMapping("/status/{id}")
    public ResponseEntity<Demande> updateDemandeStatus(@PathVariable String id, @RequestParam String status) {
        try {
            Demande updatedDemande = demandeService.updateDemandeStatus(id, status);
            return ResponseEntity.ok(updatedDemande);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/supprimer/{id}")
    public ResponseEntity<Void> supprimerDemande(@PathVariable String id) {
        String demandeurUsername = getCurrentUsername();
        if (demandeurUsername == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        try {
            demandeService.deleteDemande(id, demandeurUsername);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build(); // Or another appropriate status
        }
    }
}