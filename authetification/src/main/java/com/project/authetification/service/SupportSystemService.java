package com.project.authetification.service;

import com.project.authetification.model.*;
import com.project.authetification.repository.*;
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

    // --- METHODES DE LECTURE ---

    public List<WorkOrder> getWorkOrdersEnAttente() {
        return workOrderRepository.findByStatus("EN_ATTENTE");
    }

    public List<WorkOrder> getWorkOrdersByAssigne(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé: " + username));
        return workOrderRepository.findByAssigne_Id(user.getId());
    }

    public List<VM> getAllVMs() {
        return vmRepository.findAll();
    }

    public List<VM> getVMsByDemande(String demandeId) {
        return vmRepository.findByDemande_Id(demandeId);
    }

    // --- METHODES D'ACTION ---

    public WorkOrder assignerWorkOrder(String workOrderId, String assigneUsername) {
        WorkOrder workOrder = workOrderRepository.findById(workOrderId)
                .orElseThrow(() -> new RuntimeException("WorkOrder introuvable"));
        
        User assigne = userRepository.findByUsername(assigneUsername)
                .orElseThrow(() -> new RuntimeException("Utilisateur Support introuvable"));

        workOrder.setAssigne(assigne);
        workOrder.setStatus("EN_COURS");
        workOrder.setDateDebut(LocalDateTime.now());

        // Met à jour la demande liée
        if (workOrder.getDemande() != null) {
            workOrder.getDemande().setAssigneSupport(assigne);
            demandeRepository.save(workOrder.getDemande());
        }

        return workOrderRepository.save(workOrder);
    }

    public VM creerVM(String workOrderId, String vmName) {
        WorkOrder workOrder = workOrderRepository.findById(workOrderId)
                .orElseThrow(() -> new RuntimeException("WorkOrder non trouvé"));

        Demande demande = workOrder.getDemande();
        if (demande == null) throw new RuntimeException("Demande introuvable pour ce WorkOrder");

        // Création de l'objet VM dans la base de données
        VM vm = new VM();
        vm.setVmName(vmName);
        vm.setCpu(demande.getCpu());
        vm.setRam(demande.getRam());
        vm.setOs(demande.getOs());
        
        // Attention: Vérifie si c'est getDisque() ou getDisk() dans ton modèle Demande
        // Ici j'ai mis getDisque() car c'est ce que tu as écrit dans ton message
        if(demande.getDisque() != null) {
             vm.setDisque(demande.getDisque());
        }

        vm.setStatus("CREATING"); // On met CREATING en attendant Terraform
        vm.setDemande(demande);
        
        return vmRepository.save(vm);
    }

    public void updateVmIp(String vmId, String ipAddress) {
        VM vm = vmRepository.findById(vmId).orElse(null);
        if (vm != null) {
            vm.setAdresseIp(ipAddress);
            vm.setStatus("RUNNING");
            vmRepository.save(vm);
        }
    }
    
    // Autres méthodes nécessaires au controller (finaliser, erreurs, etc.)
    public WorkOrder demarrerProvisionnement(String id) {
        WorkOrder wo = workOrderRepository.findById(id).orElseThrow();
        wo.setStatus("EN_PROVISIONNEMENT");
        return workOrderRepository.save(wo);
    }

    public WorkOrder completerEtape(String id, String etape) {
        WorkOrder wo = workOrderRepository.findById(id).orElseThrow();
        // Logique simplifiée pour l'exemple
        wo.setResultat((wo.getResultat() == null ? "" : wo.getResultat()) + "\nÉtape: " + etape);
        return workOrderRepository.save(wo);
    }

    public WorkOrder finaliserProvisionnement(String id, String resultat) {
        WorkOrder wo = workOrderRepository.findById(id).orElseThrow();
        wo.setStatus("COMPLETE");
        wo.setResultat(resultat);
        wo.setDateCompletion(LocalDateTime.now());
        if(wo.getDemande() != null) {
            wo.getDemande().setStatus("PROVISIONNEE");
            demandeRepository.save(wo.getDemande());
        }
        return workOrderRepository.save(wo);
    }

    public WorkOrder marquerErreur(String id, String erreur) {
        WorkOrder wo = workOrderRepository.findById(id).orElseThrow();
        wo.setStatus("ERREUR");
        wo.setErreur(erreur);
        return workOrderRepository.save(wo);
    }

    public VM updateVMStatus(String id, String status) {
        VM vm = vmRepository.findById(id).orElseThrow();
        vm.setStatus(status);
        return vmRepository.save(vm);
    }
}
