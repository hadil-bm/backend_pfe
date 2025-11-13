package com.project.authetification.repository;

import com.project.authetification.model.Demande;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DemandeRepository extends MongoRepository<Demande, String> {
    List<Demande> findByDemandeur_Id(String demandeurId);
    List<Demande> findByStatus(String status);
    List<Demande> findByValidateurCloud_Id(String validateurCloudId);
    List<Demande> findByAssigneSupport_Id(String assigneSupportId);
}