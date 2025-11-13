package com.project.authetification.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "notifications")
@Data
public class Notification {

    @Id
    private String id;

    @DBRef
    private User user; // The user who receives the notification

    private String titre;
    private String message;
    private LocalDateTime dateCreation = LocalDateTime.now();
    private boolean isRead = false;
}