package com.project.authetification.repository;

import com.project.authetification.model.TerraformExecution;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TerraformExecutionRepository extends MongoRepository<TerraformExecution, String> {
    List<TerraformExecution> findByWorkOrder_Id(String workOrderId);
    List<TerraformExecution> findByDemande_Id(String demandeId);
    List<TerraformExecution> findByStatus(String status);
    Optional<TerraformExecution> findByTerraformRunId(String terraformRunId);
}

