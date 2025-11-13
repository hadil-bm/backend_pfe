package com.project.authetification.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

@Document(collection = "vms")
@Data
public class VM {
    
    @Id
    private String id;
    
    private String vmName;
    private String vmId; // ID dans vSphere/vCenter
    
    @DBRef
    private Demande demande; // Référence à la demande d'origine
    
    // Configuration technique
    private String ram;
    private String cpu;
    private String typeVm;
    private String disque;
    private String typeDisque;
    private String os;
    private String versionOs;
    
    // Configuration réseau
    private String adresseIp;
    private String reseau;
    private String datastore;
    
    // Statut de la VM
    private String status; // CREATED, RUNNING, STOPPED, ERROR, DELETED
    
    // Dates
    private LocalDateTime dateCreation;
    private LocalDateTime dateDerniereModification;
    
    // Informations de monitoring
    private Map<String, Object> metrics; // Métriques de performance
    private Boolean monitored = false;
    
    public enum VMStatus {
        CREATED,
        RUNNING,
        STOPPED,
        ERROR,
        DELETED
    }
}

