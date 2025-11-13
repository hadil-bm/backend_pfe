package com.project.authetification.repository;

import com.project.authetification.model.VM;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VMRepository extends MongoRepository<VM, String> {
    List<VM> findByDemande_Id(String demandeId);
    Optional<VM> findByVmId(String vmId);
    List<VM> findByStatus(String status);
    List<VM> findByMonitored(Boolean monitored);
}

