package com.project.authetification.service;

import com.project.authetification.model.Demande;
import com.project.authetification.model.User;
import com.project.authetification.repository.DemandeRepository;
import com.project.authetification.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DemandeService {

    private final DemandeRepository demandeRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public Demande createDemande(String demandeurUsername, Demande demandeDetails) {
        User demandeur = userRepository.findByUsername(demandeurUsername)
                .orElseThrow(() -> new RuntimeException("Demandeur non trouvé: " + demandeurUsername));

        Demande nouvelleDemande = new Demande();
        nouvelleDemande.setDemandeur(demandeur);
        nouvelleDemande.setName(demandeDetails.getName());
        nouvelleDemande.setDescription(demandeDetails.getDescription());
        nouvelleDemande.setRam(demandeDetails.getRam());
        nouvelleDemande.setCpu(demandeDetails.getCpu());
        nouvelleDemande.setTypeVm(demandeDetails.getTypeVm());
        nouvelleDemande.setDisque(demandeDetails.getDisque());
        nouvelleDemande.setTypeDisque(demandeDetails.getTypeDisque());
        nouvelleDemande.setOs(demandeDetails.getOs());
        nouvelleDemande.setVersionOs(demandeDetails.getVersionOs());
        nouvelleDemande.setBesoinsReseau(demandeDetails.getBesoinsReseau());
        nouvelleDemande.setBesoinsPareFeu(demandeDetails.getBesoinsPareFeu());
        nouvelleDemande.setBesoinsStockage(demandeDetails.getBesoinsStockage());
        nouvelleDemande.setStatus("EN_ATTENTE");
        nouvelleDemande.setDateCreation(LocalDateTime.now());

        Demande savedDemande = demandeRepository.save(nouvelleDemande);
        
        // Envoyer une notification au demandeur
        notificationService.sendNotification(
                demandeur,
                "Nouvelle demande créée",
                "Votre demande '" + savedDemande.getName() + "' a été créée avec succès et est en attente de validation."
        );
        
        // Notifier l'équipe Cloud
        notificationService.sendNotificationToRole(
                "EQUIPECLOUD",
                "Nouvelle demande à valider",
                "Une nouvelle demande '" + savedDemande.getName() + "' a été créée par " + demandeur.getUsername() + " et nécessite une validation."
        );

        return savedDemande;
    }

    public Optional<Demande> getDemandeById(String id) {
        return demandeRepository.findById(id);
    }

    public List<Demande> getDemandesByDemandeur(String demandeurUsername) {
        User demandeur = userRepository.findByUsername(demandeurUsername)
                .orElseThrow(() -> new RuntimeException("Demandeur non trouvé: " + demandeurUsername));
        return demandeRepository.findByDemandeur_Id(demandeur.getId());
    }

    public Demande updateDemande(String id, String demandeurUsername, Demande demandeDetails) {
        Demande demande = demandeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Demande non trouvée avec l'ID: " + id));

        if (!demande.getDemandeur().getUsername().equals(demandeurUsername)) {
            throw new RuntimeException("Vous n'êtes pas autorisé à modifier cette demande.");
        }

        demande.setName(demandeDetails.getName());
        demande.setDescription(demandeDetails.getDescription());
        demande.setRam(demandeDetails.getRam());
        demande.setCpu(demandeDetails.getCpu());
        demande.setTypeVm(demandeDetails.getTypeVm());
        demande.setDisque(demandeDetails.getDisque());
        demande.setTypeDisque(demandeDetails.getTypeDisque());
        demande.setOs(demandeDetails.getOs());
        demande.setVersionOs(demandeDetails.getVersionOs());
        demande.setBesoinsReseau(demandeDetails.getBesoinsReseau());
        demande.setBesoinsPareFeu(demandeDetails.getBesoinsPareFeu());
        demande.setBesoinsStockage(demandeDetails.getBesoinsStockage());
        demande.setDateModification(LocalDateTime.now());

        return demandeRepository.save(demande);
    }

    public Demande updateDemandeStatus(String id, String newStatus) {
        Demande demande = demandeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Demande non trouvée avec l'ID: " + id));

        Demande.Status status;
        try {
            status = Demande.Status.valueOf(newStatus.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Statut de demande invalide: " + newStatus);
        }

        String oldStatus = demande.getStatus();
        demande.setStatus(status.toString());
        Demande updatedDemande = demandeRepository.save(demande);

        if (!oldStatus.equals(updatedDemande.getStatus())) {
            notificationService.sendNotification(
                    demande.getDemandeur(),
                    "Mise à jour de l'état de votre demande",
                    "L'état de votre demande '" + demande.getName() + "' a été mis à jour à : " + updatedDemande.getStatus()
            );
        }

        return updatedDemande;
    }

    public void deleteDemande(String id, String demandeurUsername) {
        Demande demande = demandeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Demande non trouvée avec l'ID: " + id));

        if (!demande.getDemandeur().getUsername().equals(demandeurUsername)) {
            throw new RuntimeException("Vous n'êtes pas autorisé à supprimer cette demande.");
        }

        demandeRepository.deleteById(id);
    }
}
