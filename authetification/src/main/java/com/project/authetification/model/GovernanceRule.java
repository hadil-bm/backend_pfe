package com.project.authetification.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "governance_rules")
@Data
public class GovernanceRule {
    
    @Id
    private String id;
    
    private String nom;
    private String description;
    private String type; // RESOURCE_LIMIT, SECURITY, NETWORK, etc.
    
    // Règles de ressources
    private Integer maxRam; // RAM maximale autorisée (GB)
    private Integer maxCpu; // CPU maximal autorisé
    private Integer maxDisk; // Disque maximal autorisé (GB)
    
    // Règles de sécurité
    private Boolean requireFirewall;
    private Boolean requireEncryption;
    
    // Règles réseau
    private String allowedNetworks; // Réseaux autorisés
    private String restrictedNetworks; // Réseaux restreints
    
    // Statut
    private Boolean isActive;
    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;
    
    @DBRef
    private User createdBy; // Administrateur qui a créé la règle
}

