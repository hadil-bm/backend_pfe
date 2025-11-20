package com.project.authetification.service;


import com.project.authetification.model.User;
import com.project.authetification.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.stream.Collectors;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Date;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.beans.factory.annotation.Value;

@Service
@RequiredArgsConstructor
public class AuthService {
    private static final long EXPIRATION_TIME = 3600000; // 1 heure en millisecondes

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;


    private String baseUrl;



    @Value("${spring.mail.username}") // application.properties
    private String mailFrom;

    public Optional<User> register(String username, String email, String password ,List<String> roles) {
        if (userRepository.existsByEmail(email)) {
            return Optional.empty();
        }

        User newUser = new User();
        newUser.setUsername(username);
        // Formater les rôles pour commencer par "ROLE_"
        List<String> formattedRoles = roles.stream()
                .map(role -> "ROLE_" + role.toUpperCase())
                .collect(Collectors.toList());
        newUser.setRoles(formattedRoles);
        newUser.setEmail(email);
        newUser.setPassword(passwordEncoder.encode(password));

        return Optional.of(userRepository.save(newUser));
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public boolean checkPassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    public void processForgotPasswordRequest(String email) {
        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            String token = UUID.randomUUID().toString();
            user.setResetToken(token);
            user.setResetTokenExpiry(new Date(System.currentTimeMillis() + 3600000)); // Token expires in 1 hour (3600000 ms)
            userRepository.save(user);

            // Send email with reset link
            sendPasswordResetEmail(user.getEmail(), token);
        }
        //  Important:  Do NOT tell the user if the email doesn't exist.  This is a security risk.
    }

    public boolean resetPassword(String token, String newPassword) {
        Optional<User> userOptional = userRepository.findByResetToken(token);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            if (user.getResetTokenExpiry().after(new Date())) {
                user.setPassword(passwordEncoder.encode(newPassword));
                user.setResetToken(null); // Clear the token
                user.setResetTokenExpiry(null);
                userRepository.save(user);
                return true;
            }
        }
        return false;
    }

    private void sendPasswordResetEmail(String email, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(email);
        message.setSubject("Password Reset Request");
        message.setText("To reset your password, please click on the following link:\n" +
                "http://yourdomain.com/api/auth/reset-password?token=" + token + "\n\n" + //  Include the full URL
                "This link will expire in 1 hour.");  //  IMPORTANT:  Tell the user about the expiration
        mailSender.send(message);
    }
}