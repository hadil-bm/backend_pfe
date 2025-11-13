package com.project.authetification.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "workorders")
@Data
public class WorkOrder {
    
    @Id
    private String id;
    
    @DBRef
    private Demande demande; // Référence à la demande
    
    @DBRef
    private User assigne; // Utilisateur de l'équipe Support assigné
    
    private String titre;
    private String description;
    private String status; // EN_ATTENTE, EN_COURS, COMPLETE, ERREUR
    
    // Étapes de provisionnement
    private List<String> etapes = new ArrayList<>(); // Liste des étapes à effectuer
    private List<String> etapesCompletees = new ArrayList<>(); // Étapes complétées
    
    // Dates
    private LocalDateTime dateCreation;
    private LocalDateTime dateDebut;
    private LocalDateTime dateCompletion;
    
    // Résultat
    private String resultat; // Résultat du provisionnement
    private String erreur; // Message d'erreur si échec
    
    @DBRef
    private List<VM> vmsCreees = new ArrayList<>(); // Liste des VMs créées par ce workorder
    
    public enum WorkOrderStatus {
        EN_ATTENTE,
        EN_COURS,
        COMPLETE,
        ERREUR
    }
}

