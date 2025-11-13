package com.project.authetification.repository;

import com.project.authetification.model.Notification;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends MongoRepository<Notification, String> {
    List<Notification> findByUser_IdOrderByDateCreationDesc(String userId);
    long countByUser_IdAndIsReadFalse(String userId);
    List<Notification> findByUser_Id(String userId);
}