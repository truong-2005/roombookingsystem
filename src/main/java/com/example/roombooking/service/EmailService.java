package com.example.roombooking.service;

public interface EmailService {

    void sendPasswordResetEmail(String to, String fullName, String resetLink, int expiresInMinutes);

    void sendPasswordChangedEmail(String to, String fullName);

    void sendBookingNotificationEmail(String to, String fullName, String title, String message);
}