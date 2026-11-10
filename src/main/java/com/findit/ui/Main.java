package com.findit.ui;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Main application launcher for FindIt — Smart Lost & Found Management System.
 */
public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(
                        UIManager.getSystemLookAndFeelClassName()
                );
            } catch (Exception ignored) {
            }

            new LoginFrame().setVisible(true);
        });
    }
}
