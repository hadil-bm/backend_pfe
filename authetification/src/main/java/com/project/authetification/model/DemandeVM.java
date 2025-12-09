package com.project.authetification.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "demande_vm")
@Getter
@Setter
@NoArgsConstructor
public class DemandeVM {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int cpu;
    private int ram;
    private String osType;

    @Enumerated(EnumType.STRING)
    private DemandeStatus status = DemandeStatus.PENDING_CLOUD;

    private String ipAddress;
}

