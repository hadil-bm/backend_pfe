package com.project.authetification.service;

import com.project.authetification.model.*;
import com.project.authetification.repository.DemandeRepository;
import com.project.authetification.repository.UserRepository;
import com.project.authetification.repository.VMRepository;
import com.project.authetification.repository.WorkOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupportSystemService {

    private final WorkOrderRepository workOrderRepository;
    private final DemandeRepository demandeRepository;
    private final VMRepository vmRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    // 1. Zid l service Terraform s7i7
    private final TerraformService terraformService; 

    public List<WorkOrder> getWorkOrdersEnAttente() {
        return workOrderRepository.findByStatus("EN_ATTENTE");
    }

    public List<WorkOrder> getWorkOrdersByAssigne(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        return workOrderRepository.findByAssigne_Id(user.getId());
    }

    public WorkOrder assignerWorkOrder(String workOrderId, String assigneUsername) {
        WorkOrder workOrder = workOrderRepository.findById(workOrderId)
                .orElseThrow(() -> new RuntimeException("WorkOrder non trouvé: " + workOrderId));

        User assigne = userRepository.findByUsername(assigneUsername)
                .orElseThrow(() -> new RuntimeException("User non trouvé: " + assigneUsername));

        workOrder.setAssigne(assigne);
        workOrder.setStatus("EN_COURS");
        workOrder.setDateDebut(LocalDateTime.now());

        Demande demande = workOrder.getDemande();
        if (demande != null) {
            demande.setAssigneSupport(assigne);
            demande.setStatus("EN_PROVISIONNEMENT");
            demande.setDateProvisionnement(LocalDateTime.now());
            demandeRepository.save(demande);
        }

        return workOrderRepository.save(workOrder);
    }

    // ---------------------------------------------------------
    // C'EST LA METHODE LA PLUS IMPORTANTE (LE DECLENCHEUR)
    // ---------------------------------------------------------
    public WorkOrder demarrerProvisionnement(String workOrderId) {
        WorkOrder workOrder = workOrderRepository.findById(workOrderId)
                .orElseThrow(() -> new RuntimeException("WorkOrder non trouvé: " + workOrderId));

        Demande demande = workOrder.getDemande();
        if (demande == null) {
            throw new RuntimeException("Demande associée au WorkOrder non trouvée !");
        }

        workOrder.setStatus("EN_COURS");
        workOrder.setDateDebut(LocalDateTime.now());

        // 2. Préparer les données pour Terraform à partir de l'objet 'Demande'
        DemandeVM terraformData = new DemandeVM();
        terraformData.setId((long) Math.abs(demande.getId().hashCode()));
        terraformData.setCpu(parseInteger(demande.getCpu()));
        terraformData.setRam(parseInteger(demande.getRam()));
        terraformData.setOsType(demande.getOs());
        
        log.info(">>> Déclenchement de Terraform pour demande ID: {}", demande.getId());

        // 3. Appeler le service Terraform s7i7 (celui avec ProcessBuilder)
        terraformService.triggerVmCreation(terraformData)
            .thenAccept(ipAddress -> {
                // HEDHA CODE YETLança ki Terraform YENJA7
                log.info("Terraform a terminé avec succès! IP: {}", ipAddress);
                
                // On met à jour la demande et on crée la VM dans la base
                demande.setAdresseIp(ipAddress);
                demande.setStatus("PROVISIONNEE");
                demande.setDateCompletion(LocalDateTime.now());
                demandeRepository.save(demande);

                workOrder.setStatus("COMPLETE");
                workOrder.setResultat("VM créée avec IP: " + ipAddress);
                workOrder.setDateCompletion(LocalDateTime.now());
                workOrderRepository.save(workOrder);

                // Optionnel: Créer un objet VM détaillé aussi
                VM vm = new VM();
                vm.setVmName(demande.getName());
                vm.setDemande(demande);
                vm.setAdresseIp(ipAddress);
                vm.setStatus("RUNNING");
                vmRepository.save(vm);
                
                log.info("Base de données mise à jour pour la demande {}", demande.getId());
            })
            .exceptionally(ex -> {
                // HEDHA CODE YETLança ki Terraform YECHLEK (échoue)
                log.error("Terraform a échoué pour la demande ID: {}", demande.getId(), ex);
                
                demande.setStatus("ERROR");
                demandeRepository.save(demande);
                
                workOrder.setStatus("ERREUR");
                workOrder.setErreur(ex.getMessage());
                workOrderRepository.save(workOrder);
                return null;
            });
        
        return workOrderRepository.save(workOrder);
    }
    
    // ... Garde les autres méthodes (creerVM, finaliserProvisionnement, etc.) ...
    // Tu peux garder 'creerVM' pour une création manuelle sans Terraform si tu veux.
    
    private int parseInteger(String value) {
        if (value == null || value.isBlank()) return 1;
        try {
            return Integer.parseInt(value.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return 1;
        }
    }
    
    // Garde le reste de tes méthodes ici (completerEtape, creerVM, finaliserProvisionnement, etc.)
}
