package com.project.authetification.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

// Pas d'@Entity, pas de @Document. Juste un DTO.
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DemandeVM {

    private Long id; // ID numérique pour Terraform (ex: hashcode de l'ID String)
    private int cpu;
    private int ram;
    private String osType;
    private DemandeStatus status;
    private String ipAddress;
}
