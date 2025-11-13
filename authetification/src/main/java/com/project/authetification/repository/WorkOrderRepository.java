package com.project.authetification.repository;

import com.project.authetification.model.WorkOrder;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkOrderRepository extends MongoRepository<WorkOrder, String> {
    List<WorkOrder> findByDemande_Id(String demandeId);
    List<WorkOrder> findByAssigne_Id(String assigneId);
    List<WorkOrder> findByStatus(String status);
    Optional<WorkOrder> findById(String id);
}

