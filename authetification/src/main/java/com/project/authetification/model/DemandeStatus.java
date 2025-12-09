package com.project.authetification.model;

public enum DemandeStatus {
    PENDING_CLOUD,
    PENDING_SUPPORT,
    APPROVED,   // Prêt pour Terraform
    DEPLOYED,   // Terraform a fini
    ERROR       // Terraform a échoué
}
