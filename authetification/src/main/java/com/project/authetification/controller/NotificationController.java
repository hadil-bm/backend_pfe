package com.project.authetification.controller;

import com.project.authetification.model.Notification;
import com.project.authetification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return null;
    }

    @GetMapping
    public ResponseEntity<List<Notification>> getMyNotifications() {
        String username = getCurrentUsername();
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        List<Notification> notifications = notificationService.getNotificationsForCurrentUser(username);
        return ResponseEntity.ok(notifications);
    }

    // ✅ FIX: Return JSON object instead of plain number
    @GetMapping("/unread/count")
    public ResponseEntity<Map<String, Long>> getUnreadNotificationsCount() {
        String username = getCurrentUsername();
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        long count = notificationService.getUnreadNotificationsCount(username);

        // Return JSON: {"count": 1}
        return ResponseEntity.ok(Map.of("count", count));
    }

    @PutMapping("/mark-as-read/{id}")
    public ResponseEntity<Void> markNotificationAsRead(@PathVariable String id) {
        notificationService.markNotificationAsRead(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/mark-all-as-read")
    public ResponseEntity<Void> markAllNotificationsAsRead() {
        String username = getCurrentUsername();
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        notificationService.markAllNotificationsAsReadForCurrentUser(username);
        return ResponseEntity.noContent().build();
    }
}