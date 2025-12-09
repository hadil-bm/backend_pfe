package com.project.authetification.repository;

import com.project.authetification.model.DemandeVM;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DemandeVMRepository extends JpaRepository<DemandeVM, Long> {
}

