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
 * Modern SaaS login screen for FindIt.
 */
public class LoginFrame extends JFrame {

    private static final String BLANK_MSG = " ";

    private final JTextField emailField = new JTextField();
    private final JPasswordField passwordField = new JPasswordField();
    private final JLabel errorLabel = UiKit.label(BLANK_MSG, Font.PLAIN, 12, AppTheme.DANGER, SwingConstants.LEFT);

    private final RoundedButton loginButton = new RoundedButton("Sign In", RoundedButton.Style.PRIMARY);
    private final RoundedButton registerButton = new RoundedButton("Create an account", RoundedButton.Style.SECONDARY);

    private boolean busy = false;

    public LoginFrame() {
        setTitle("FINDIT - Smart Lost & Found Management System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
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

        JLabel brandLabel = UiKit.label("FINDIT", Font.BOLD, 26, AppTheme.PRIMARY_INDIGO, SwingConstants.CENTER);
        column.addRow(brandLabel, 10);

        JLabel taglineLabel = UiKit.label("Smart Lost & Found Management System", Font.PLAIN, 12, AppTheme.TEXT_SECONDARY, SwingConstants.CENTER);
        column.addRow(taglineLabel, 3);

        // Heading
        JLabel welcomeLabel = UiKit.label("Welcome back", Font.BOLD, 20, AppTheme.TEXT_PRIMARY, SwingConstants.CENTER);
        column.addRow(welcomeLabel, 24);

        JLabel subLabel = UiKit.label("Sign in to manage and track lost & found items", Font.PLAIN, 12, AppTheme.TEXT_MUTED, SwingConstants.CENTER);
        column.addRow(subLabel, 4);

        // Inputs
        emailField.setUI(new javax.swing.plaf.basic.BasicTextFieldUI());
        emailField.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        emailField.setOpaque(false);

        passwordField.setUI(new javax.swing.plaf.basic.BasicPasswordFieldUI());
        passwordField.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        passwordField.setOpaque(false);

        column.addRow(UiKit.labeled("Email Address", UiKit.textBox(emailField)), 20);
        column.addRow(UiKit.labeled("Password", UiKit.passwordBox(passwordField)), 14);

        // Forgot Password link row
        JPanel forgotRow = new JPanel(new BorderLayout());
        forgotRow.setOpaque(false);
        JButton forgotBtn = new JButton("Forgot password?");
        forgotBtn.setFont(AppTheme.font(Font.BOLD, 12f));
        forgotBtn.setForeground(AppTheme.ACCENT_CYAN);
        forgotBtn.setContentAreaFilled(false);
        forgotBtn.setBorderPainted(false);
        forgotBtn.setFocusPainted(false);
        forgotBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        forgotBtn.addActionListener(e -> new ForgotPasswordFrame().setVisible(true));
        forgotRow.add(forgotBtn, BorderLayout.EAST);
        column.addRow(forgotRow, 4);

        column.addRow(errorLabel, 6);

        // Buttons
        loginButton.setPreferredSize(new Dimension(360, 44));
        registerButton.setPreferredSize(new Dimension(360, 44));

        column.addRow(loginButton, 6);
        column.addRow(UiKit.divider("New to FindIt?"), 16);
        column.addRow(registerButton, 12);

        // Card Panel & Gradient Background
        UiKit.CardPanel card = new UiKit.CardPanel(24, 32);
        card.add(column, BorderLayout.CENTER);

        UiKit.GradientPanel root = new UiKit.GradientPanel(new GridBagLayout());
        root.setBorder(new EmptyBorder(24, 24, 24, 24));
        root.add(card);
        setContentPane(root);

        // Actions
        themeToggleBtn.addActionListener(e -> {
            AppTheme.toggleTheme();
            themeToggleBtn.setText(AppTheme.isLight() ? "Dark" : "Light");
            themeToggleBtn.setLeadingIcon(AppTheme.isLight() ? UiKit.createMoonIcon(14, null) : UiKit.createSunIcon(14, null));
            welcomeLabel.setForeground(AppTheme.TEXT_PRIMARY);
            subLabel.setForeground(AppTheme.TEXT_MUTED);
            taglineLabel.setForeground(AppTheme.TEXT_SECONDARY);
            card.repaint();
            root.repaint();
        });
        loginButton.addActionListener(e -> login());
        registerButton.addActionListener(e -> {
            new RegisterFrame().setVisible(true);
        });

        getRootPane().setDefaultButton(loginButton);

        // Clear error on user typing
        UiKit.onChange(emailField, this::clearError);
        UiKit.onChange(passwordField, this::clearError);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                emailField.requestFocusInWindow();
            }
        });

        pack();
        setLocationRelativeTo(null);
    }

    private void showError(String message) {
        errorLabel.setForeground(AppTheme.DANGER);
        errorLabel.setText(message);
    }

    private void clearError() {
        if (!BLANK_MSG.equals(errorLabel.getText())) {
            errorLabel.setText(BLANK_MSG);
        }
    }

    private void setBusy(boolean value) {
        busy = value;
        loginButton.setEnabled(!value);
        registerButton.setEnabled(!value);
        loginButton.setText(value ? "Signing in..." : "Sign In");
        setCursor(Cursor.getPredefinedCursor(value ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    private void login() {
        if (busy) {
            return;
        }

        String email = emailField.getText().trim();
        char[] passwordChars = passwordField.getPassword();

        if (email.isEmpty() || passwordChars.length == 0) {
            Arrays.fill(passwordChars, '\0');
            showError("Please enter your email and password.");
            return;
        }

        String password = new String(passwordChars);
        Arrays.fill(passwordChars, '\0');

        setBusy(true);

        // Background authentication via SwingWorker
        new SwingWorker<User, Void>() {
            @Override
            protected User doInBackground() throws Exception {
                return new UserDAO().authenticate(email, password);
            }

            @Override
            protected void done() {
                setBusy(false);
                try {
                    User user = get();

                    if (user == null) {
                        passwordField.setText("");
                        passwordField.requestFocusInWindow();
                        showError("Invalid email or password.");
                        return;
                    }

                    dispose();
                    new DashboardFrame(user).setVisible(true);

                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    showError("Unable to connect to the database. Please try again.");
                }
            }
        }.execute();
    }
}
