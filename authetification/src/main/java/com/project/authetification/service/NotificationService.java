package com.project.authetification.service;

import com.project.authetification.model.Notification;
import com.project.authetification.model.User;
import com.project.authetification.repository.NotificationRepository;
import com.project.authetification.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public void sendNotification(User user, String titre, String message) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setTitre(titre);
        notification.setMessage(message);
        notificationRepository.save(notification);
    }

    public List<Notification> getNotificationsForCurrentUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        return notificationRepository.findByUser_IdOrderByDateCreationDesc(user.getId());
    }

    public long getUnreadNotificationsCount(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        return notificationRepository.countByUser_IdAndIsReadFalse(user.getId());
    }

    public void markNotificationAsRead(String id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification not found with ID: " + id));
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    public void markAllNotificationsAsReadForCurrentUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        List<Notification> unreadNotifications = notificationRepository.findByUser_Id(user.getId());
        unreadNotifications.stream()
                .filter(n -> !n.isRead())
                .forEach(notification -> notification.setRead(true));
        notificationRepository.saveAll(unreadNotifications);
    }

    /**
     * Envoie une notification à tous les utilisateurs ayant un rôle spécifique
     */
    public void sendNotificationToRole(String role, String titre, String message) {
        List<User> users = userRepository.findAll();
        users.stream()
                .filter(user -> user.getRoles() != null && user.getRoles().contains("ROLE_" + role.toUpperCase()))
                .forEach(user -> sendNotification(user, titre, message));
    }
}