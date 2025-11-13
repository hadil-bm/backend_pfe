package com.project.authetification.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "demandes")
@Data
public class Demande {

    @Id
    private String id;

    @DBRef
    private User demandeur; // Référence à l'utilisateur qui a fait la demande

    // Champs du formulaire (remplis par le client)
    private String name;
    private String description;
    private String ram;
    private String cpu;
    private String typeVm;
    private String disque;
    private String typeDisque;
    private String os;
    private String versionOs;
    private String besoinsReseau; // Besoins réseau spécifiques
    private String besoinsPareFeu; // Besoins pare-feu
    private String besoinsStockage; // Besoins de stockage particuliers

    // Informations ajoutées par l'équipe Cloud (après validation)
    private String adresseIp; // Ajoutée par l'équipe Cloud
    private String reseau; // Configuration réseau
    private String datastore; // Datastore assigné
    private String justificationRefus; // Justification en cas de refus
    
    @DBRef
    private User validateurCloud; // Utilisateur de l'équipe Cloud qui a validé
    private LocalDateTime dateValidation;
    
    @DBRef
    private User assigneSupport; // Utilisateur de l'équipe Support assigné
    private String workorderId; // Référence au workorder
    
    // Infos système
    private LocalDateTime dateCreation = LocalDateTime.now();
    private LocalDateTime dateModification;
    private LocalDateTime dateProvisionnement; // Date de début de provisionnement
    private LocalDateTime dateCompletion; // Date de fin de provisionnement

    // Statut de la demande
    private String status = "EN_ATTENTE"; // Valeur par défaut

    public enum Status {
        EN_ATTENTE,      // En attente de validation Cloud
        EN_VALIDATION,   // En cours de validation par l'équipe Cloud
        A_MODIFIER,      // Demande à modifier par le client
        VALIDE,          // Validée, en attente de provisionnement
        EN_PROVISIONNEMENT, // En cours de provisionnement par le support
        PROVISIONNEE,    // Provisionnée et prête
        REFUSEE,         // Refusée par l'équipe Cloud
        COMPLETEE        // Complétée et en supervision
    }
}
