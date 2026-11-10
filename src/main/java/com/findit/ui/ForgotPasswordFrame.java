package com.findit.ui;

import com.findit.dao.PasswordResetDAO;
import com.findit.service.EmailConfig;
import com.findit.service.EmailService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

/**
 * Premium SaaS password recovery wizard with email OTP verification.
 * Designed with a lag-free, progressive two-step workflow and a modern dark & colorful aesthetic.
 */
public class ForgotPasswordFrame extends JFrame {

    private final PasswordResetDAO resetDAO = new PasswordResetDAO();

    // Containers for CardLayout (Step 1: Enter Email -> Step 2: Enter Code & New Password)
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel stepContainer = new JPanel(cardLayout) {
        @Override
        public Dimension getPreferredSize() {
            for (Component c : getComponents()) {
                if (c.isVisible()) {
                    return c.getPreferredSize();
                }
            }
            return super.getPreferredSize();
        }
    };

    // Step 1 Components
    private final JTextField emailField = new JTextField();
    private final JLabel step1StatusLabel = UiKit.label(" ", Font.PLAIN, 12, AppTheme.DANGER, SwingConstants.LEFT);
    private final RoundedButton sendCodeBtn = new RoundedButton("Send Verification Code", RoundedButton.Style.PRIMARY);

    // Step 2 Components
    private final JLabel step2EmailBadge = new JLabel();
    private final JTextField otpField = new JTextField();
    private final JPasswordField newPasswordField = new JPasswordField();
    private final JPasswordField confirmPasswordField = new JPasswordField();
    private final JLabel step2StatusLabel = UiKit.label(" ", Font.PLAIN, 12, AppTheme.DANGER, SwingConstants.LEFT);
    private final RoundedButton resetPasswordBtn = new RoundedButton("Reset Password", RoundedButton.Style.PRIMARY);
    private final JButton resendBtn = new JButton("Resend Code");

    // Live Cooldown Timer
    private Timer cooldownTimer;
    private int cooldownSeconds = 0;

    private String targetEmail = "";
    private boolean busy = false;

    public ForgotPasswordFrame() {
        setTitle("FINDIT - Account Recovery");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setResizable(false);

        // Preload email config in background so there is zero latency when user sends
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                EmailConfig.loadConfig();
                return null;
            }
        }.execute();

        UiKit.Column column = new UiKit.Column(380);

        // App Branding
        column.addRow(new UiKit.LogoBadge(), 0);

        JLabel brandLabel = UiKit.label("FINDIT", Font.BOLD, 24, AppTheme.PRIMARY_INDIGO, SwingConstants.CENTER);
        column.addRow(brandLabel, 8);

        JLabel heading = UiKit.label("Reset Your Password", Font.BOLD, 20, AppTheme.TEXT_PRIMARY, SwingConstants.CENTER);
        column.addRow(heading, 6);

        JLabel sub = UiKit.label("Secure email verification via 6-digit single-use code", Font.PLAIN, 13, AppTheme.TEXT_SECONDARY, SwingConstants.CENTER);
        column.addRow(sub, 3);

        // Build Step 1 and Step 2 panels
        stepContainer.setOpaque(false);
        stepContainer.add(buildStep1Panel(), "STEP_1");
        stepContainer.add(buildStep2Panel(), "STEP_2");

        column.addRow(stepContainer, 16);

        // Back to login at the very bottom
        RoundedButton backToLoginBtn = new RoundedButton("Back to Login", RoundedButton.Style.SECONDARY);
        backToLoginBtn.setPreferredSize(new Dimension(380, 42));
        backToLoginBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        backToLoginBtn.addActionListener(e -> dispose());
        column.addRow(backToLoginBtn, 12);

        // Card Panel & Gradient Background
        UiKit.CardPanel card = new UiKit.CardPanel(22, 28);
        card.add(column, BorderLayout.CENTER);

        UiKit.GradientPanel root = new UiKit.GradientPanel(new GridBagLayout());
        root.setBorder(new EmptyBorder(16, 16, 16, 16));
        root.add(card);
        setContentPane(root);

        // Wire Events
        sendCodeBtn.addActionListener(e -> onSendCodeClicked());
        resetPasswordBtn.addActionListener(e -> onResetPasswordClicked());
        resendBtn.addActionListener(e -> onResendClicked());

        UiKit.onChange(emailField, () -> step1StatusLabel.setText(" "));
        UiKit.onChange(otpField, () -> step2StatusLabel.setText(" "));
        UiKit.onChange(newPasswordField, () -> step2StatusLabel.setText(" "));
        UiKit.onChange(confirmPasswordField, () -> step2StatusLabel.setText(" "));

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                emailField.requestFocusInWindow();
            }

            @Override
            public void windowClosed(WindowEvent e) {
                if (cooldownTimer != null) {
                    cooldownTimer.stop();
                }
            }
        });

        pack();
        setLocationRelativeTo(null);
    }

    public void setEmail(String email) {
        if (email != null && !email.isBlank()) {
            emailField.setText(email.trim());
            targetEmail = email.trim().toLowerCase();
        }
    }

    private JPanel buildStep1Panel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);

        JLabel stepLabel = UiKit.label("Step 1 of 2: Request Verification Code", Font.BOLD, 12, AppTheme.ACCENT_CYAN, SwingConstants.LEFT);
        stepLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(stepLabel);
        p.add(Box.createVerticalStrut(10));

        // Informational guidance callout
        JPanel infoBox = new JPanel(new BorderLayout(8, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                AppTheme.applyQualityRendering(g2);
                g2.setColor(AppTheme.INFO_BG);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                g2.setColor(AppTheme.BORDER);
                g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1f, getHeight() - 1f, 10, 10));
                g2.dispose();
            }
        };
        infoBox.setOpaque(false);
        infoBox.setBorder(new EmptyBorder(10, 12, 10, 12));
        infoBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        infoBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));

        JLabel infoText = new JLabel("<html>We'll send a 6-digit verification code to this address. The code expires in 5 minutes.</html>");
        infoText.setFont(AppTheme.font(Font.PLAIN, 12f));
        infoText.setForeground(AppTheme.INFO_TEXT);
        infoBox.add(infoText, BorderLayout.CENTER);
        p.add(infoBox);
        p.add(Box.createVerticalStrut(14));

        // Standard 44px email input
        UiKit.InputBox emailBox = UiKit.textBox(emailField);
        emailBox.setPreferredSize(new Dimension(380, 44));
        emailBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        JPanel emailGroup = UiKit.labeled("Registered Email Address", emailBox);
        emailGroup.setAlignmentX(Component.LEFT_ALIGNMENT);
        emailGroup.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        p.add(emailGroup);
        p.add(Box.createVerticalStrut(6));

        step1StatusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(step1StatusLabel);
        p.add(Box.createVerticalStrut(14));

        sendCodeBtn.setPreferredSize(new Dimension(380, 44));
        sendCodeBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        sendCodeBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(sendCodeBtn);

        p.add(Box.createVerticalGlue()); // Absorbs any extra space so inputs NEVER expand!

        return p;
    }

    private JPanel buildStep2Panel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);

        // Emerald Success Notification Banner
        JPanel banner = new JPanel(new BorderLayout(8, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                AppTheme.applyQualityRendering(g2);
                g2.setColor(AppTheme.SUCCESS_BG);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                g2.setColor(AppTheme.SUCCESS);
                g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1f, getHeight() - 1f, 10, 10));
                g2.dispose();
            }
        };
        banner.setOpaque(false);
        banner.setBorder(new EmptyBorder(8, 12, 8, 12));
        banner.setAlignmentX(Component.LEFT_ALIGNMENT);
        banner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));

        JLabel checkIcon = new JLabel("✓");
        checkIcon.setFont(AppTheme.font(Font.BOLD, 16f));
        checkIcon.setForeground(AppTheme.SUCCESS);

        step2EmailBadge.setFont(AppTheme.font(Font.PLAIN, 12f));
        step2EmailBadge.setForeground(AppTheme.SUCCESS_TEXT);
        step2EmailBadge.setText("Verification code sent to your email!");

        banner.add(checkIcon, BorderLayout.WEST);
        banner.add(step2EmailBadge, BorderLayout.CENTER);
        p.add(banner);
        p.add(Box.createVerticalStrut(14));

        // OTP Field Row with Resend Link
        JPanel otpHeader = new JPanel(new BorderLayout());
        otpHeader.setOpaque(false);
        otpHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
        otpHeader.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));

        JLabel otpLabel = UiKit.label("6-Digit Verification Code", Font.BOLD, 12, AppTheme.TEXT_SECONDARY, SwingConstants.LEFT);
        resendBtn.setFont(AppTheme.font(Font.BOLD, 11.5f));
        resendBtn.setForeground(AppTheme.ACCENT_CYAN);
        resendBtn.setContentAreaFilled(false);
        resendBtn.setBorderPainted(false);
        resendBtn.setFocusPainted(false);
        resendBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        otpHeader.add(otpLabel, BorderLayout.WEST);
        otpHeader.add(resendBtn, BorderLayout.EAST);
        p.add(otpHeader);
        p.add(Box.createVerticalStrut(6));

        // Stylized OTP Box with large centered font
        otpField.setHorizontalAlignment(JTextField.CENTER);
        otpField.setFont(new Font(Font.MONOSPACED, Font.BOLD, 20));
        otpField.setForeground(AppTheme.ACCENT_CYAN);
        otpField.setCaretColor(AppTheme.ACCENT_CYAN);
        UiKit.InputBox otpBox = UiKit.textBox(otpField);
        otpBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        otpBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        p.add(otpBox);
        p.add(Box.createVerticalStrut(10));

        // Password Fields
        JPanel passGroup = UiKit.labeled("New Password (min 8 characters)", UiKit.passwordBox(newPasswordField));
        passGroup.setAlignmentX(Component.LEFT_ALIGNMENT);
        passGroup.setMaximumSize(new Dimension(Integer.MAX_VALUE, 68));
        p.add(passGroup);
        p.add(Box.createVerticalStrut(8));

        JPanel confirmGroup = UiKit.labeled("Confirm New Password", UiKit.passwordBox(confirmPasswordField));
        confirmGroup.setAlignmentX(Component.LEFT_ALIGNMENT);
        confirmGroup.setMaximumSize(new Dimension(Integer.MAX_VALUE, 68));
        p.add(confirmGroup);
        p.add(Box.createVerticalStrut(6));

        step2StatusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(step2StatusLabel);
        p.add(Box.createVerticalStrut(12));

        resetPasswordBtn.setPreferredSize(new Dimension(380, 44));
        resetPasswordBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        resetPasswordBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(resetPasswordBtn);

        p.add(Box.createVerticalGlue());

        return p;
    }

    private void onSendCodeClicked() {
        if (busy) return;

        String email = emailField.getText().trim();
        if (email.isEmpty() || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            step1StatusLabel.setForeground(AppTheme.DANGER);
            step1StatusLabel.setText("Please enter a valid email address.");
            return;
        }

        this.targetEmail = email;
        busy = true;
        sendCodeBtn.setEnabled(false);
        sendCodeBtn.setText("Dispatching code...");
        step1StatusLabel.setForeground(AppTheme.TEXT_MUTED);
        step1StatusLabel.setText("Connecting to secure verification service...");
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        // Background worker handles OTP creation + SMTP dispatch in a SINGLE background thread
        new SwingWorker<DispatchResult, Void>() {
            @Override
            protected DispatchResult doInBackground() {
                try {
                    PasswordResetDAO.RequestResult req = resetDAO.requestOtp(email);
                    if (req.getStatus() == PasswordResetDAO.RequestStatus.COOLDOWN_ACTIVE) {
                        return new DispatchResult(false, "Please wait " + req.getCooldownSecondsRemaining() + "s before requesting another code.", req.getCooldownSecondsRemaining());
                    }
                    if (req.getStatus() == PasswordResetDAO.RequestStatus.EMAIL_NOT_FOUND) {
                        // Anti-enumeration: still transition as if dispatched
                        return new DispatchResult(true, "If registered, a verification code was sent to " + email, 60);
                    }

                    // Send email if SMTP is configured
                    if (EmailConfig.isConfigured()) {
                        EmailService.sendOtpEmail(email, req.getPlainOtp());
                        return new DispatchResult(true, "Verification code sent to " + email, 60);
                    } else {
                        // Demo / Local mode notice
                        return new DispatchResult(true, "Code generated (SMTP unconfigured, OTP: " + req.getPlainOtp() + ")", 60);
                    }
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    return new DispatchResult(false, EmailService.getFriendlyErrorMessage(cause), 0);
                }
            }

            @Override
            protected void done() {
                busy = false;
                sendCodeBtn.setEnabled(true);
                sendCodeBtn.setText("Send Verification Code");
                setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

                try {
                    DispatchResult result = get();
                    if (result.success) {
                        step2EmailBadge.setText("<html>Code dispatched to <b>" + targetEmail + "</b> (5m expiry)</html>");
                        cardLayout.show(stepContainer, "STEP_2");
                        pack();
                        setLocationRelativeTo(null);
                        startCooldown(result.cooldownSeconds > 0 ? result.cooldownSeconds : 60);
                        otpField.requestFocusInWindow();
                    } else {
                        step1StatusLabel.setForeground(AppTheme.DANGER);
                        step1StatusLabel.setText(result.message);
                    }
                } catch (Exception ex) {
                    step1StatusLabel.setForeground(AppTheme.DANGER);
                    step1StatusLabel.setText("Error: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void onResendClicked() {
        if (busy || cooldownSeconds > 0 || targetEmail.isEmpty()) return;

        busy = true;
        resendBtn.setEnabled(false);
        resendBtn.setText("Sending...");
        step2StatusLabel.setForeground(AppTheme.TEXT_MUTED);
        step2StatusLabel.setText("Resending verification code...");

        new SwingWorker<DispatchResult, Void>() {
            @Override
            protected DispatchResult doInBackground() {
                try {
                    PasswordResetDAO.RequestResult req = resetDAO.requestOtp(targetEmail);
                    if (req.getStatus() == PasswordResetDAO.RequestStatus.COOLDOWN_ACTIVE) {
                        return new DispatchResult(false, "Cooldown active. Please wait " + req.getCooldownSecondsRemaining() + "s.", req.getCooldownSecondsRemaining());
                    }
                    if (EmailConfig.isConfigured()) {
                        EmailService.sendOtpEmail(targetEmail, req.getPlainOtp());
                        return new DispatchResult(true, "New code sent to " + targetEmail, 60);
                    } else {
                        return new DispatchResult(true, "New code generated (OTP: " + req.getPlainOtp() + ")", 60);
                    }
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    return new DispatchResult(false, EmailService.getFriendlyErrorMessage(cause), 0);
                }
            }

            @Override
            protected void done() {
                busy = false;
                try {
                    DispatchResult result = get();
                    if (result.success) {
                        step2StatusLabel.setForeground(AppTheme.SUCCESS);
                        step2StatusLabel.setText("✓ New code sent! Check your inbox.");
                        startCooldown(result.cooldownSeconds > 0 ? result.cooldownSeconds : 60);
                    } else {
                        step2StatusLabel.setForeground(AppTheme.DANGER);
                        step2StatusLabel.setText(result.message);
                        if (result.cooldownSeconds > 0) {
                            startCooldown(result.cooldownSeconds);
                        } else {
                            resendBtn.setEnabled(true);
                            resendBtn.setText("Resend Code");
                        }
                    }
                } catch (Exception ex) {
                    step2StatusLabel.setForeground(AppTheme.DANGER);
                    step2StatusLabel.setText("Resend error: " + ex.getMessage());
                    resendBtn.setEnabled(true);
                    resendBtn.setText("Resend Code");
                }
            }
        }.execute();
    }

    private void startCooldown(int seconds) {
        if (cooldownTimer != null) {
            cooldownTimer.stop();
        }
        cooldownSeconds = seconds;
        resendBtn.setEnabled(false);
        resendBtn.setText("Resend in " + cooldownSeconds + "s");

        cooldownTimer = new Timer(1000, e -> {
            cooldownSeconds--;
            if (cooldownSeconds <= 0) {
                cooldownTimer.stop();
                resendBtn.setText("Resend Code");
                resendBtn.setEnabled(true);
            } else {
                resendBtn.setText("Resend in " + cooldownSeconds + "s");
            }
        });
        cooldownTimer.start();
    }

    private void onResetPasswordClicked() {
        if (busy) return;

        String otp = otpField.getText().trim();
        char[] passChars = newPasswordField.getPassword();
        char[] confirmChars = confirmPasswordField.getPassword();

        if (otp.isEmpty() || passChars.length == 0 || confirmChars.length == 0) {
            step2StatusLabel.setForeground(AppTheme.DANGER);
            step2StatusLabel.setText("Please fill in all fields.");
            Arrays.fill(passChars, '\0');
            Arrays.fill(confirmChars, '\0');
            return;
        }

        if (otp.length() != 6 || !otp.matches("\\d{6}")) {
            step2StatusLabel.setForeground(AppTheme.DANGER);
            step2StatusLabel.setText("OTP must be exactly 6 digits.");
            Arrays.fill(passChars, '\0');
            Arrays.fill(confirmChars, '\0');
            return;
        }

        if (passChars.length < 8) {
            step2StatusLabel.setForeground(AppTheme.DANGER);
            step2StatusLabel.setText("Password must contain at least 8 characters.");
            Arrays.fill(passChars, '\0');
            Arrays.fill(confirmChars, '\0');
            return;
        }

        if (!Arrays.equals(passChars, confirmChars)) {
            step2StatusLabel.setForeground(AppTheme.DANGER);
            step2StatusLabel.setText("Passwords do not match.");
            Arrays.fill(passChars, '\0');
            Arrays.fill(confirmChars, '\0');
            return;
        }

        String newPassword = new String(passChars);
        Arrays.fill(passChars, '\0');
        Arrays.fill(confirmChars, '\0');

        busy = true;
        resetPasswordBtn.setEnabled(false);
        resetPasswordBtn.setText("Updating password...");
        step2StatusLabel.setForeground(AppTheme.TEXT_MUTED);
        step2StatusLabel.setText("Verifying code & encrypting new password...");
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                return resetDAO.resetPassword(targetEmail, otp, newPassword);
            }

            @Override
            protected void done() {
                busy = false;
                setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

                try {
                    boolean success = get();
                    if (success) {
                        step2StatusLabel.setForeground(AppTheme.SUCCESS);
                        step2StatusLabel.setText("✓ Password updated successfully! Closing in 2s...");
                        resetPasswordBtn.setText("Password Reset Complete ✓");
                        resetPasswordBtn.setEnabled(false);

                        // Auto-close dialog smoothly after 2 seconds
                        Timer closeTimer = new Timer(2000, ev -> dispose());
                        closeTimer.setRepeats(false);
                        closeTimer.start();
                    } else {
                        resetPasswordBtn.setEnabled(true);
                        resetPasswordBtn.setText("Reset Password");
                        step2StatusLabel.setForeground(AppTheme.DANGER);
                        step2StatusLabel.setText("Invalid or expired code. Please recheck or resend.");
                    }
                } catch (ExecutionException ex) {
                    resetPasswordBtn.setEnabled(true);
                    resetPasswordBtn.setText("Reset Password");
                    step2StatusLabel.setForeground(AppTheme.DANGER);
                    step2StatusLabel.setText("Reset error: " + ex.getCause().getMessage());
                } catch (Exception ex) {
                    resetPasswordBtn.setEnabled(true);
                    resetPasswordBtn.setText("Reset Password");
                    step2StatusLabel.setForeground(AppTheme.DANGER);
                    step2StatusLabel.setText("Error: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private static class DispatchResult {
        final boolean success;
        final String message;
        final int cooldownSeconds;

        DispatchResult(boolean success, String message, int cooldownSeconds) {
            this.success = success;
            this.message = message;
            this.cooldownSeconds = cooldownSeconds;
        }
    }
}
