package com.project.authetification.service;

import com.project.authetification.model.Demande;
import com.project.authetification.model.GovernanceRule;
import com.project.authetification.model.User;
import com.project.authetification.model.WorkOrder;
import com.project.authetification.repository.DemandeRepository;
import com.project.authetification.repository.GovernanceRuleRepository;
import com.project.authetification.repository.UserRepository;
import com.project.authetification.repository.WorkOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CloudTeamService {

    private final DemandeRepository demandeRepository;
    private final UserRepository userRepository;
    private final GovernanceRuleRepository governanceRuleRepository;
    private final NotificationService notificationService;
    private final WorkOrderRepository workOrderRepository;

    /**
     * Récupère toutes les demandes en attente de validation
     */
    public List<Demande> getDemandesEnAttente() {
        return demandeRepository.findByStatus("EN_ATTENTE");
    }

    /**
     * Récupère toutes les demandes en cours de validation
     */
    public List<Demande> getDemandesEnValidation() {
        return demandeRepository.findByStatus("EN_VALIDATION");
    }

    /**
     * Valide une demande et ajoute les informations nécessaires
     */
    public Demande validerDemande(String demandeId, String validateurUsername, 
                                  String adresseIp, String reseau, String datastore) {
        Demande demande = demandeRepository.findById(demandeId)
                .orElseThrow(() -> new RuntimeException("Demande non trouvée: " + demandeId));

        User validateur = userRepository.findByUsername(validateurUsername)
                .orElseThrow(() -> new RuntimeException("Validateur non trouvé: " + validateurUsername));

        // Vérifier les règles de gouvernance
        if (!checkGovernanceRules(demande)) {
            throw new RuntimeException("La demande ne respecte pas les règles de gouvernance");
        }

        // Vérifier les ressources disponibles (simplifié - à implémenter selon les besoins)
        if (!checkResourceAvailability(demande)) {
            throw new RuntimeException("Resources insuffisantes");
        }

        // Mettre à jour la demande
        demande.setStatus("VALIDE");
        demande.setAdresseIp(adresseIp);
        demande.setReseau(reseau);
        demande.setDatastore(datastore);
        demande.setValidateurCloud(validateur);
        demande.setDateValidation(LocalDateTime.now());

        Demande savedDemande = demandeRepository.save(demande);

        // Créer un workorder pour l'équipe Support
        WorkOrder workOrder = createWorkOrder(savedDemande);
        savedDemande.setWorkorderId(workOrder.getId());
        demandeRepository.save(savedDemande);

        // Notifier le client
        notificationService.sendNotification(
                demande.getDemandeur(),
                "Demande validée",
                "Votre demande '" + demande.getName() + "' a été validée par l'équipe Cloud. Le provisionnement va commencer."
        );

        return savedDemande;
    }

    /**
     * Refuse une demande avec une justification
     */
    public Demande refuserDemande(String demandeId, String validateurUsername, String justification) {
        Demande demande = demandeRepository.findById(demandeId)
                .orElseThrow(() -> new RuntimeException("Demande non trouvée: " + demandeId));

        User validateur = userRepository.findByUsername(validateurUsername)
                .orElseThrow(() -> new RuntimeException("Validateur non trouvé: " + validateurUsername));

        demande.setStatus("REFUSEE");
        demande.setJustificationRefus(justification);
        demande.setValidateurCloud(validateur);
        demande.setDateValidation(LocalDateTime.now());

        Demande savedDemande = demandeRepository.save(demande);

        // Notifier le client
        notificationService.sendNotification(
                demande.getDemandeur(),
                "Demande refusée",
                "Votre demande '" + demande.getName() + "' a été refusée. Raison: " + justification
        );

        return savedDemande;
    }

    /**
     * Demande des modifications à la demande
     */
    public Demande demanderModification(String demandeId, String validateurUsername, String justification) {
        Demande demande = demandeRepository.findById(demandeId)
                .orElseThrow(() -> new RuntimeException("Demande non trouvée: " + demandeId));

        User validateur = userRepository.findByUsername(validateurUsername)
                .orElseThrow(() -> new RuntimeException("Validateur non trouvé: " + validateurUsername));

        demande.setStatus("A_MODIFIER");
        demande.setJustificationRefus(justification);
        demande.setValidateurCloud(validateur);
        demande.setDateValidation(LocalDateTime.now());

        Demande savedDemande = demandeRepository.save(demande);

        // Notifier le client
        notificationService.sendNotification(
                demande.getDemandeur(),
                "Modification requise",
                "Votre demande '" + demande.getName() + "' nécessite des modifications. " + justification
        );

        return savedDemande;
    }

    /**
     * Vérifie si la demande respecte les règles de gouvernance
     */
    private boolean checkGovernanceRules(Demande demande) {
        List<GovernanceRule> activeRules = governanceRuleRepository.findByIsActiveTrue();
        
        for (GovernanceRule rule : activeRules) {
            // Vérifier les limites de RAM
            if (rule.getMaxRam() != null) {
                try {
                    int demandeRam = Integer.parseInt(demande.getRam().replaceAll("[^0-9]", ""));
                    if (demandeRam > rule.getMaxRam()) {
                        return false;
                    }
                } catch (NumberFormatException e) {
                    // Ignorer si le format n'est pas valide
                }
            }

            // Vérifier les limites de CPU
            if (rule.getMaxCpu() != null) {
                try {
                    int demandeCpu = Integer.parseInt(demande.getCpu().replaceAll("[^0-9]", ""));
                    if (demandeCpu > rule.getMaxCpu()) {
                        return false;
                    }
                } catch (NumberFormatException e) {
                    // Ignorer si le format n'est pas valide
                }
            }

            // Vérifier les règles de sécurité
            if (rule.getRequireFirewall() != null && rule.getRequireFirewall()) {
                if (demande.getBesoinsPareFeu() == null || demande.getBesoinsPareFeu().isEmpty()) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Vérifie la disponibilité des ressources (simplifié)
     */
    private boolean checkResourceAvailability(Demande demande) {
        // TODO: Implémenter la vérification réelle des ressources disponibles
        // Pour l'instant, on retourne toujours true
        return true;
    }

    /**
     * Crée un workorder pour l'équipe Support
     */
    private WorkOrder createWorkOrder(Demande demande) {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setDemande(demande);
        workOrder.setTitre("Provisionnement VM: " + demande.getName());
        workOrder.setDescription("Provisionnement de la VM selon la demande " + demande.getId());
        workOrder.setStatus("EN_ATTENTE");
        workOrder.setDateCreation(LocalDateTime.now());
        
        // Définir les étapes de provisionnement
        workOrder.getEtapes().addAll(List.of(
                "Création de la VM",
                "Configuration du réseau",
                "Configuration du stockage",
                "Installation de l'OS",
                "Configuration de la sécurité",
                "Configuration du pare-feu",
                "Tests de validation"
        ));

        return workOrderRepository.save(workOrder);
    }
}

