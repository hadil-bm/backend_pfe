package com.project.authetification.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

@Document(collection = "terraform_executions")
@Data
public class TerraformExecution {
    
    @Id
    private String id;
    
    @DBRef
    private WorkOrder workOrder; // Référence au workorder
    
    @DBRef
    private Demande demande; // Référence à la demande
    
    private String terraformRunId; // ID de l'exécution Terraform Cloud
    private String workspaceId; // ID du workspace Terraform Cloud
    private String status; // PENDING, RUNNING, APPLIED, ERROR, CANCELLED
    
    // Configuration Terraform générée
    private String terraformConfig; // Configuration Terraform en JSON/HCL
    private Map<String, String> terraformVariables; // Variables Terraform
    
    // Résultats
    private String output; // Output de Terraform
    private String errorMessage; // Message d'erreur si échec
    private Map<String, Object> terraformOutputs; // Outputs de Terraform
    
    // Dates
    private LocalDateTime dateCreation;
    private LocalDateTime dateDebut;
    private LocalDateTime dateCompletion;
    
    // Informations de la VM créée
    private String vmId; // ID de la VM créée par Terraform
    private String vmIpAddress; // Adresse IP de la VM créée
    
    public enum TerraformStatus {
        PENDING,
        RUNNING,
        APPLIED,
        ERROR,
        CANCELLED
    }
}

