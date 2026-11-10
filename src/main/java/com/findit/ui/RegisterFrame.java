package com.findit.ui;

import com.findit.dao.UserDAO;
import com.findit.model.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

/**
 * Modern SaaS registration screen for FindIt.
 */
public class RegisterFrame extends JFrame {

    private static final String PASSWORD_HINT = "Password must contain at least 8 characters.";

    private final JTextField nameField = new JTextField();
    private final JTextField emailField = new JTextField();
    private final JPasswordField passwordField = new JPasswordField();
    private final JPasswordField confirmPasswordField = new JPasswordField();

    private final JLabel statusLabel = UiKit.label(
            PASSWORD_HINT, Font.PLAIN, 12, AppTheme.TEXT_MUTED, SwingConstants.LEFT);

    private final RoundedButton registerButton =
            new RoundedButton("Create Account", RoundedButton.Style.PRIMARY);
    private final RoundedButton backButton =
            new RoundedButton("Back to Login", RoundedButton.Style.SECONDARY);

    private final UserDAO userDAO = new UserDAO();
    private boolean busy = false;

    public RegisterFrame() {
        setTitle("FINDIT - Create Account");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setResizable(false);

        // Content Column
        UiKit.Column column = new UiKit.Column(360);

        // Top Row: Theme Toggle
        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);
        RoundedButton themeToggleBtn = new RoundedButton(
                AppTheme.isLight() ? "Dark" : "Light",
                AppTheme.isLight() ? UiKit.createMoonIcon(14, null) : UiKit.createSunIcon(14, null),
                RoundedButton.Style.SECONDARY
        );
        themeToggleBtn.setPreferredSize(new Dimension(84, 28));
        themeToggleBtn.setFont(AppTheme.font(Font.BOLD, 11f));
        topRow.add(themeToggleBtn, BorderLayout.EAST);
        column.addRow(topRow, 0);

        // Logo & Branding
        column.addRow(new UiKit.LogoBadge(), 2);

        JLabel brandLabel = UiKit.label("FINDIT", Font.BOLD, 24, AppTheme.PRIMARY_INDIGO, SwingConstants.CENTER);
        column.addRow(brandLabel, 8);

        JLabel heading = UiKit.label("Create your account", Font.BOLD, 20, AppTheme.TEXT_PRIMARY, SwingConstants.CENTER);
        column.addRow(heading, 8);

        JLabel subtitle = UiKit.label("Join the smart campus lost & found network", Font.PLAIN, 13, AppTheme.TEXT_SECONDARY, SwingConstants.CENTER);
        column.addRow(subtitle, 4);

        column.addRow(UiKit.labeled("Full Name", UiKit.textBox(nameField)), 20);
        column.addRow(UiKit.labeled("Email Address", UiKit.textBox(emailField)), 12);
        column.addRow(UiKit.labeled("Password", UiKit.passwordBox(passwordField)), 12);
        column.addRow(UiKit.labeled("Confirm Password", UiKit.passwordBox(confirmPasswordField)), 12);
        column.addRow(statusLabel, 8);

        registerButton.setPreferredSize(new Dimension(360, 44));
        backButton.setPreferredSize(new Dimension(360, 44));

        column.addRow(registerButton, 10);
        column.addRow(backButton, 8);

        // Card Panel & Gradient Background
        UiKit.CardPanel card = new UiKit.CardPanel(24, 32);
        card.add(column, BorderLayout.CENTER);

        UiKit.GradientPanel root = new UiKit.GradientPanel(new GridBagLayout());
        root.setBorder(new EmptyBorder(20, 20, 20, 20));
        root.add(card);
        setContentPane(root);

        // Behaviour
        registerButton.addActionListener(e -> registerUser());
        backButton.addActionListener(e -> dispose());
        getRootPane().setDefaultButton(registerButton);

        // Reset status on edits
        Runnable resetStatus = this::resetStatus;
        UiKit.onChange(nameField, resetStatus);
        UiKit.onChange(emailField, resetStatus);
        UiKit.onChange(passwordField, resetStatus);
        UiKit.onChange(confirmPasswordField, resetStatus);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                nameField.requestFocusInWindow();
            }
        });

        pack();
        setLocationRelativeTo(null);
    }

    private void showError(String message) {
        statusLabel.setForeground(AppTheme.DANGER);
        statusLabel.setText(message);
    }

    private void resetStatus() {
        statusLabel.setForeground(AppTheme.TEXT_MUTED);
        statusLabel.setText(PASSWORD_HINT);
    }

    private void setBusy(boolean value) {
        busy = value;
        registerButton.setEnabled(!value);
        backButton.setEnabled(!value);
        registerButton.setText(value ? "Creating account..." : "Create Account");
        setCursor(Cursor.getPredefinedCursor(value ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    private String validate(String name, String email, char[] password, char[] confirm) {
        if (name.isEmpty() || email.isEmpty() || password.length == 0 || confirm.length == 0) {
            return "Please fill in all required fields.";
        }
        if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            return "Please enter a valid email address.";
        }
        if (password.length < 8) {
            return "Password must contain at least 8 characters.";
        }
        if (!Arrays.equals(password, confirm)) {
            return "Passwords do not match.";
        }
        return null;
    }

    private void registerUser() {
        if (busy) {
            return;
        }

        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        char[] passwordChars = passwordField.getPassword();
        char[] confirmChars = confirmPasswordField.getPassword();

        String error = validate(name, email, passwordChars, confirmChars);
        String password = (error == null) ? new String(passwordChars) : null;

        Arrays.fill(passwordChars, '\0');
        Arrays.fill(confirmChars, '\0');

        if (error != null) {
            showError(error);
            return;
        }

        User user = new User();
        user.setName(name);
        user.setEmail(email);
        // Public registration must always create a regular USER account.
        user.setRole("USER");

        setBusy(true);

        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                return userDAO.registerSecureUser(user, password);
            }

            @Override
            protected void done() {
                setBusy(false);
                try {
                    if (Boolean.TRUE.equals(get())) {
                        DialogUtils.showSuccess(
                                RegisterFrame.this,
                                "Account Created",
                                "Your account has been created successfully. You can now sign in."
                        );
                        dispose();
                    } else {
                        showError("Registration failed. Please try again.");
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    showError("Registration failed. This email may already be registered.");
                }
            }
        }.execute();
    }
}
