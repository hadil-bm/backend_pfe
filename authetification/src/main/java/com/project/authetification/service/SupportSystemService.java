package com.project.authetification.service;

import com.project.authetification.model.Demande;
import com.project.authetification.model.User;
import com.project.authetification.model.VM;
import com.project.authetification.model.WorkOrder;
import com.project.authetification.repository.DemandeRepository;
import com.project.authetification.repository.UserRepository;
import com.project.authetification.repository.VMRepository;
import com.project.authetification.repository.WorkOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SupportSystemService {

    private final WorkOrderRepository workOrderRepository;
    private final DemandeRepository demandeRepository;
    private final VMRepository vmRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    /**
     * Récupère tous les workorders en attente
     */
    public List<WorkOrder> getWorkOrdersEnAttente() {
        return workOrderRepository.findByStatus("EN_ATTENTE");
    }

    /**
     * Récupère tous les workorders assignés à un utilisateur
     */
    public List<WorkOrder> getWorkOrdersByAssigne(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        return workOrderRepository.findByAssigne_Id(user.getId());
    }

    /**
     * Assign un workorder à un membre de l'équipe Support
     */
    public WorkOrder assignerWorkOrder(String workOrderId, String assigneUsername) {
        WorkOrder workOrder = workOrderRepository.findById(workOrderId)
                .orElseThrow(() -> new RuntimeException("WorkOrder non trouvé: " + workOrderId));

        User assigne = userRepository.findByUsername(assigneUsername)
                .orElseThrow(() -> new RuntimeException("User non trouvé: " + assigneUsername));

        workOrder.setAssigne(assigne);
        workOrder.setStatus("EN_COURS");
        workOrder.setDateDebut(LocalDateTime.now());

        // Mettre à jour la demande associée
        Demande demande = workOrder.getDemande();
        if (demande != null) {
            demande.setAssigneSupport(assigne);
            demande.setStatus("EN_PROVISIONNEMENT");
            demande.setDateProvisionnement(LocalDateTime.now());
            demandeRepository.save(demande);
        }

        return workOrderRepository.save(workOrder);
    }

    /**
     * Démarre le provisionnement d'une VM
     */
    public WorkOrder demarrerProvisionnement(String workOrderId) {
        WorkOrder workOrder = workOrderRepository.findById(workOrderId)
                .orElseThrow(() -> new RuntimeException("WorkOrder non trouvé: " + workOrderId));

        workOrder.setStatus("EN_COURS");
        workOrder.setDateDebut(LocalDateTime.now());

        return workOrderRepository.save(workOrder);
    }

    /**
     * Complète une étape de provisionnement
     */
    public WorkOrder completerEtape(String workOrderId, String etape) {
        WorkOrder workOrder = workOrderRepository.findById(workOrderId)
                .orElseThrow(() -> new RuntimeException("WorkOrder non trouvé: " + workOrderId));

        List<String> etapesCompletees = workOrder.getEtapesCompletees();
        if (!etapesCompletees.contains(etape)) {
            etapesCompletees.add(etape);
            workOrder.setEtapesCompletees(etapesCompletees);
        }

        return workOrderRepository.save(workOrder);
    }

    /**
     * Crée une VM après provisionnement
     */
    public VM creerVM(String workOrderId, String vmName) {
        WorkOrder workOrder = workOrderRepository.findById(workOrderId)
                .orElseThrow(() -> new RuntimeException("WorkOrder non trouvé: " + workOrderId));

        Demande demande = workOrder.getDemande();

        VM vm = new VM();
        vm.setVmName(vmName);
        vm.setVmId(UUID.randomUUID().toString()); // Dans un vrai système, ce serait l'ID vSphere
        vm.setDemande(demande);
        vm.setRam(demande.getRam());
        vm.setCpu(demande.getCpu());
        vm.setTypeVm(demande.getTypeVm());
        vm.setDisque(demande.getDisque());
        vm.setTypeDisque(demande.getTypeDisque());
        vm.setOs(demande.getOs());
        vm.setVersionOs(demande.getVersionOs());
        vm.setAdresseIp(demande.getAdresseIp());
        vm.setReseau(demande.getReseau());
        vm.setDatastore(demande.getDatastore());
        vm.setStatus("CREATED");
        vm.setDateCreation(LocalDateTime.now());
        vm.setMonitored(false);

        VM savedVM = vmRepository.save(vm);

        // Ajouter la VM à la liste des VMs créées dans le workorder
        // La liste est déjà initialisée dans le modèle WorkOrder, donc on peut l'utiliser directement
        workOrder.getVmsCreees().add(savedVM);
        workOrderRepository.save(workOrder);

        return savedVM;
    }

    /**
     * Finalise le provisionnement
     */
    public WorkOrder finaliserProvisionnement(String workOrderId, String resultat) {
        WorkOrder workOrder = workOrderRepository.findById(workOrderId)
                .orElseThrow(() -> new RuntimeException("WorkOrder non trouvé: " + workOrderId));

        workOrder.setStatus("COMPLETE");
        workOrder.setResultat(resultat);
        workOrder.setDateCompletion(LocalDateTime.now());

        // Mettre à jour la demande
        Demande demande = workOrder.getDemande();
        if (demande != null) {
            demande.setStatus("PROVISIONNEE");
            demande.setDateCompletion(LocalDateTime.now());
            demandeRepository.save(demande);
        }

        WorkOrder savedWorkOrder = workOrderRepository.save(workOrder);

        // Notifier le client
        if (demande != null && demande.getDemandeur() != null) {
            notificationService.sendNotification(
                    demande.getDemandeur(),
                    "Provisionnement terminé",
                    "Le provisionnement de votre demande '" + demande.getName() + "' est terminé. Les VMs sont prêtes."
            );
        }

        return savedWorkOrder;
    }

    /**
     * Marque une erreur dans le provisionnement
     */
    public WorkOrder marquerErreur(String workOrderId, String erreur) {
        WorkOrder workOrder = workOrderRepository.findById(workOrderId)
                .orElseThrow(() -> new RuntimeException("WorkOrder non trouvé: " + workOrderId));

        workOrder.setStatus("ERREUR");
        workOrder.setErreur(erreur);

        // Mettre à jour la demande
        Demande demande = workOrder.getDemande();
        if (demande != null) {
            demande.setStatus("EN_ATTENTE"); // Remettre en attente pour correction
            demandeRepository.save(demande);
        }

        return workOrderRepository.save(workOrder);
    }

    /**
     * Récupère toutes les VMs
     */
    public List<VM> getAllVMs() {
        return vmRepository.findAll();
    }

    /**
     * Récupère les VMs d'une demande
     */
    public List<VM> getVMsByDemande(String demandeId) {
        return vmRepository.findByDemande_Id(demandeId);
    }

    /**
     * Met à jour le statut d'une VM
     */
    public VM updateVMStatus(String vmId, String status) {
        VM vm = vmRepository.findById(vmId)
                .orElseThrow(() -> new RuntimeException("VM non trouvée: " + vmId));

        vm.setStatus(status);
        vm.setDateDerniereModification(LocalDateTime.now());

        return vmRepository.save(vm);
    }
}

