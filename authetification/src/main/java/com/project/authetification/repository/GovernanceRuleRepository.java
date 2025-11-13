package com.project.authetification.repository;

import com.project.authetification.model.GovernanceRule;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GovernanceRuleRepository extends MongoRepository<GovernanceRule, String> {
    List<GovernanceRule> findByIsActiveTrue();
    List<GovernanceRule> findByType(String type);
}

