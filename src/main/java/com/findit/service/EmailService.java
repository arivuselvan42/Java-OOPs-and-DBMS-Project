package com.findit.service;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import javax.swing.SwingWorker;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

/**
 * Reusable, secure Gmail SMTP email service for FindIt.
 * Reads credentials dynamically from environment variables (FINDIT_EMAIL, FINDIT_EMAIL_PASSWORD).
 * Never hardcodes or logs credentials or OTP codes.
 */
public class EmailService {

    public interface EmailCallback {
        void onSuccess();
        void onFailure(String friendlyErrorMessage, Exception cause);
    }

    /**
     * Sends a plain-text email synchronously.
     */
    public static boolean sendEmail(String toEmail, String subject, String body) throws Exception {
        return sendEmail(toEmail, subject, body, false);
    }

    /**
     * Sends an email synchronously with HTML option.
     */
    public static boolean sendEmail(String toEmail, String subject, String content, boolean isHtml) throws Exception {
        EmailConfig.loadConfig();

        if (!EmailConfig.isConfigured()) {
            throw new IllegalStateException(
                    "Gmail credentials not found. Please ensure the environment variables " +
                    "FINDIT_EMAIL and FINDIT_EMAIL_PASSWORD are set."
            );
        }

        if (toEmail == null || !toEmail.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("Invalid recipient email address: " + toEmail);
        }

        Properties props = new Properties();
        props.put("mail.smtp.host", EmailConfig.getHost());
        props.put("mail.smtp.port", String.valueOf(EmailConfig.getPort()));
        props.put("mail.smtp.auth", String.valueOf(EmailConfig.isAuth()));
        props.put("mail.smtp.starttls.enable", String.valueOf(EmailConfig.isStarttls()));
        props.put("mail.smtp.connectiontimeout", "10000"); // 10s connection timeout
        props.put("mail.smtp.timeout", "10000");           // 10s socket timeout
        props.put("mail.smtp.writetimeout", "10000");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(
                        EmailConfig.getUsername(),
                        EmailConfig.getPassword()
                );
            }
        });

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(EmailConfig.getUsername(), "FindIt Security"));
        message.setRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));
        message.setSubject(subject);

        if (isHtml) {
            message.setContent(content, "text/html; charset=utf-8");
        } else {
            message.setText(content);
        }

        Transport.send(message);
        return true;
    }

    /**
     * Helper method to send OTP password recovery emails.
     */
    public static boolean sendOtpEmail(String toEmail, String otpCode) throws Exception {
        String subject = "FindIt - Password Reset Verification Code";
        String body = "Hello,\n\n"
                + "A password reset request was received for your FindIt account.\n\n"
                + "Your 6-digit verification code is:\n\n"
                + "   " + otpCode + "\n\n"
                + "Security Notice:\n"
                + "- This code is valid for 5 minutes only.\n"
                + "- This code can only be used once.\n"
                + "- Never share this code with anyone.\n\n"
                + "If you did not request this, please ignore this email.\n\n"
                + "- FindIt Smart Lost & Found Management System";

        return sendEmail(toEmail, subject, body, false);
    }

    /**
     * Dispatches an email asynchronously in a background thread using SwingWorker.
     * Invokes the callback on the Event Dispatch Thread (EDT) upon completion.
     */
    public static SwingWorker<Boolean, Void> sendEmailAsync(
            String toEmail, String subject, String body, boolean isHtml, EmailCallback callback) {

        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                return sendEmail(toEmail, subject, body, isHtml);
            }

            @Override
            protected void done() {
                try {
                    boolean success = get();
                    if (success && callback != null) {
                        callback.onSuccess();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    if (callback != null) {
                        callback.onFailure("Email sending was interrupted.", e);
                    }
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    String friendlyMessage = getFriendlyErrorMessage(cause);
                    if (callback != null) {
                        callback.onFailure(friendlyMessage, cause instanceof Exception ? (Exception) cause : new Exception(cause));
                    }
                }
            }
        };

        worker.execute();
        return worker;
    }

    /**
     * Translates raw technical and network exceptions into friendly, actionable error messages.
     */
    public static String getFriendlyErrorMessage(Throwable t) {
        if (t == null) {
            return "An unknown error occurred while sending email.";
        }

        if (t instanceof IllegalStateException) {
            return t.getMessage();
        }

        if (t instanceof AuthenticationFailedException) {
            return "Gmail SMTP authentication failed. Please verify your Gmail address and ensure you are using a 16-character Google App Password (not your primary password).";
        }

        String msg = t.getMessage() != null ? t.getMessage() : "";

        if (t instanceof java.net.ConnectException || t instanceof java.net.UnknownHostException || msg.contains("Connection refused") || msg.contains("timed out")) {
            return "Could not connect to the Gmail SMTP server (smtp.gmail.com:587). Please check your internet connection.";
        }

        if (t instanceof MessagingException) {
            if (msg.contains("535") || msg.contains("Authentication") || msg.contains("Username and Password not accepted")) {
                return "Gmail SMTP authentication failed. Please verify your 16-character Google App Password in FINDIT_EMAIL_PASSWORD.";
            }
            if (msg.contains("Invalid Addresses") || msg.contains("550")) {
                return "The recipient email address could not be delivered to.";
            }
        }

        return "Email dispatch failed: " + t.getMessage();
    }

    /**
     * Sends a test verification email to confirm SMTP connectivity and credentials.
     */
    public static boolean testEmail(String toEmail) throws Exception {
        String subject = "FindIt - SMTP Connection Test";
        String body = "Hello,\n\n"
                + "This is a test email sent from the FindIt Desktop Application via Gmail SMTP.\n\n"
                + "Configuration Details:\n"
                + "- Host: " + EmailConfig.getHost() + "\n"
                + "- Port: " + EmailConfig.getPort() + "\n"
                + "- STARTTLS: Enabled\n"
                + "- Sender: " + EmailConfig.getUsername() + "\n"
                + "- Status: Operational\n\n"
                + "- FindIt Development Team";

        return sendEmail(toEmail, subject, body, false);
    }

    /**
     * Standalone command-line test runner for testing email sending.
     */
    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("FINDIT - GMAIL SMTP EMAIL SERVICE TEST");
        System.out.println("=================================================\n");

        EmailConfig.loadConfig();
        System.out.println("SMTP Configured: " + EmailConfig.isConfigured());
        System.out.println("SMTP Host: " + EmailConfig.getHost() + ":" + EmailConfig.getPort());
        System.out.println("Sender Email: " + (EmailConfig.getUsername().isEmpty() ? "(Not Set)" : EmailConfig.getUsername()));

        if (!EmailConfig.isConfigured()) {
            System.err.println("\n[ERROR] Environment variables FINDIT_EMAIL or FINDIT_EMAIL_PASSWORD are not set.");
            System.err.println("Please configure Windows User environment variables:");
            System.err.println("  FINDIT_EMAIL = your.email@gmail.com");
            System.err.println("  FINDIT_EMAIL_PASSWORD = your-16-char-app-password");
            return;
        }

        String recipient = args.length > 0 ? args[0] : EmailConfig.getUsername();
        System.out.println("\nSending test email to: " + recipient + " ...");

        try {
            boolean sent = testEmail(recipient);
            if (sent) {
                System.out.println("\n[SUCCESS] Test email dispatched successfully to: " + recipient);
                System.out.println("Please check your Gmail inbox/spam folder.");
            }
        } catch (Exception e) {
            System.err.println("\n[FAILURE] Email dispatch failed.");
            System.err.println("Friendly Reason: " + getFriendlyErrorMessage(e));
            System.err.println("Technical Details: " + e.getClass().getName() + ": " + e.getMessage());
        }
    }
}
