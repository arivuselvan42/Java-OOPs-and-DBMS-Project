package com.findit.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.Ellipse2D;

/**
 * Modern SaaS styled modal dialogs for notifications, confirmations, and alerts.
 * Features high-contrast, crystal-clear typography and distinct status icons.
 */
public final class DialogUtils {

    private DialogUtils() {}

    public enum Type {
        SUCCESS,
        INFO,
        WARNING,
        ERROR
    }

    public static void showSuccess(Component parent, String title, String message) {
        showDialog(parent, title, message, Type.SUCCESS, false);
    }

    public static void showError(Component parent, String title, String message) {
        showDialog(parent, title, message, Type.ERROR, false);
    }

    public static void showWarning(Component parent, String title, String message) {
        showDialog(parent, title, message, Type.WARNING, false);
    }

    public static void showInfo(Component parent, String title, String message) {
        showDialog(parent, title, message, Type.INFO, false);
    }

    public static boolean showConfirm(Component parent, String title, String message) {
        return showDialog(parent, title, message, Type.WARNING, true);
    }

    private static boolean showDialog(
            Component parent, String title, String message, Type type, boolean isConfirm) {

        Window parentWindow = parent != null ? SwingUtilities.getWindowAncestor(parent) : null;
        if (parentWindow == null && parent instanceof Window) {
            parentWindow = (Window) parent;
        }

        JDialog dialog = new JDialog(parentWindow, Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setTitle(title);
        dialog.setUndecorated(true);
        dialog.setBackground(new Color(0, 0, 0, 0));

        boolean[] result = new boolean[]{false};

        JPanel card = new JPanel(new BorderLayout(0, 16)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                AppTheme.applyQualityRendering(g2);
                int w = getWidth();
                int h = getHeight();

                // Drop shadow
                g2.setColor(AppTheme.isLight() ? new Color(15, 23, 42, 35) : new Color(0, 0, 0, 110));
                g2.fillRoundRect(0, 0, w, h, 18, 18);

                // Elevated card surface
                g2.setColor(AppTheme.BG_CARD);
                g2.fillRoundRect(1, 1, w - 2, h - 2, 16, 16);

                // Distinct border
                g2.setColor(AppTheme.BORDER);
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(1, 1, w - 3, h - 3, 16, 16);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(22, 24, 20, 24));
        card.setMinimumSize(new Dimension(540, 200));
        card.setPreferredSize(new Dimension(560, 220));

        // Header: Icon badge + Title + Message
        JPanel header = new JPanel(new BorderLayout(16, 0));
        header.setOpaque(false);

        JComponent iconBadge = createIconBadge(type);
        header.add(iconBadge, BorderLayout.WEST);

        JPanel titleDescPanel = new JPanel();
        titleDescPanel.setLayout(new BoxLayout(titleDescPanel, BoxLayout.Y_AXIS));
        titleDescPanel.setOpaque(false);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(AppTheme.font(Font.BOLD, 17f));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // High contrast colors for title according to type
        if (AppTheme.isLight()) {
            switch (type) {
                case WARNING -> titleLabel.setForeground(new Color(0xB4, 0x53, 0x09)); // Deep warm amber
                case ERROR   -> titleLabel.setForeground(new Color(0xBE, 0x12, 0x3C)); // Deep rose crimson
                case SUCCESS -> titleLabel.setForeground(new Color(0x04, 0x78, 0x57)); // Deep emerald
                default      -> titleLabel.setForeground(new Color(0x03, 0x69, 0xA1)); // Deep sky blue
            }
        } else {
            switch (type) {
                case WARNING -> titleLabel.setForeground(new Color(0xFB, 0xBF, 0x24)); // Warm Amber #FBBF24
                case ERROR   -> titleLabel.setForeground(new Color(0xFD, 0xA4, 0xAF)); // Rose #FDA4AF
                case SUCCESS -> titleLabel.setForeground(new Color(0x34, 0xD3, 0x99)); // Mint #34D399
                default      -> titleLabel.setForeground(new Color(0x7D, 0xD3, 0xFC)); // Sky Cyan #7DD3FC
            }
        }

        // High-contrast, bright, crystal-clear message text with full word-wrap (no clippings!)
        JTextArea descArea = new JTextArea(message);
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        descArea.setOpaque(false);
        descArea.setEditable(false);
        descArea.setFocusable(false);
        descArea.setFont(AppTheme.font(Font.PLAIN, 13.5f));
        descArea.setForeground(AppTheme.TEXT_PRIMARY);
        descArea.setBorder(null);
        descArea.setAlignmentX(Component.LEFT_ALIGNMENT);

        titleDescPanel.add(titleLabel);
        titleDescPanel.add(Box.createVerticalStrut(8));
        titleDescPanel.add(descArea);
        header.add(titleDescPanel, BorderLayout.CENTER);

        // Buttons
        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonRow.setOpaque(false);

        if (isConfirm) {
            RoundedButton cancelBtn = new RoundedButton("Cancel", RoundedButton.Style.SECONDARY);
            cancelBtn.setPreferredSize(new Dimension(95, 36));
            cancelBtn.addActionListener(e -> {
                result[0] = false;
                dialog.dispose();
            });

            RoundedButton confirmBtn = new RoundedButton(
                    type == Type.ERROR ? "Delete" : "Confirm",
                    type == Type.ERROR ? RoundedButton.Style.DANGER : RoundedButton.Style.PRIMARY);
            confirmBtn.setPreferredSize(new Dimension(105, 36));
            confirmBtn.addActionListener(e -> {
                result[0] = true;
                dialog.dispose();
            });

            buttonRow.add(cancelBtn);
            buttonRow.add(confirmBtn);
        } else {
            RoundedButton okBtn = new RoundedButton(
                    "OK",
                    type == Type.ERROR ? RoundedButton.Style.DANGER : RoundedButton.Style.PRIMARY);
            okBtn.setPreferredSize(new Dimension(95, 36));
            okBtn.addActionListener(e -> dialog.dispose());
            buttonRow.add(okBtn);
        }

        card.add(header, BorderLayout.CENTER);
        card.add(buttonRow, BorderLayout.SOUTH);

        dialog.setContentPane(card);
        dialog.pack();
        dialog.setLocationRelativeTo(parentWindow);
        dialog.setVisible(true);

        return result[0];
    }

    private static JComponent createIconBadge(Type type) {
        return new JComponent() {
            {
                setPreferredSize(new Dimension(44, 44));
                setMinimumSize(new Dimension(44, 44));
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                AppTheme.applyQualityRendering(g2);
                int size = 44;

                Color bg;
                Color border;
                Color fg;

                switch (type) {
                    case SUCCESS -> {
                        bg = new Color(0x06, 0x4E, 0x3B);
                        border = new Color(0x10, 0xB9, 0x81);
                        fg = new Color(0x34, 0xD3, 0x99);
                    }
                    case ERROR -> {
                        bg = new Color(0x4C, 0x05, 0x19);
                        border = new Color(0xF4, 0x3F, 0x5E);
                        fg = new Color(0xFD, 0xA4, 0xAF);
                    }
                    case WARNING -> {
                        bg = new Color(0x45, 0x1A, 0x03);
                        border = new Color(0xF5, 0x9E, 0x0B);
                        fg = new Color(0xFB, 0xBF, 0x24);
                    }
                    default -> {
                        bg = new Color(0x0C, 0x4A, 0x6E);
                        border = new Color(0x0E, 0xA5, 0xE9);
                        fg = new Color(0x7D, 0xD3, 0xFC);
                    }
                }

                // Fill circle background
                g2.setColor(bg);
                g2.fill(new Ellipse2D.Float(0, 0, size, size));

                // Border ring
                g2.setColor(border);
                g2.setStroke(new BasicStroke(1.5f));
                g2.draw(new Ellipse2D.Float(0.75f, 0.75f, size - 1.5f, size - 1.5f));

                // Draw high-contrast vector icon
                g2.setColor(fg);
                g2.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                if (type == Type.SUCCESS) {
                    // Checkmark
                    g2.drawLine(13, 23, 20, 30);
                    g2.drawLine(20, 30, 31, 16);
                } else if (type == Type.ERROR) {
                    // Cross
                    g2.drawLine(15, 15, 29, 29);
                    g2.drawLine(29, 15, 15, 29);
                } else if (type == Type.WARNING) {
                    // Exclamation point
                    g2.drawLine(22, 13, 22, 24);
                    g2.fillOval(20, 28, 4, 4);
                } else {
                    // Info "i"
                    g2.fillOval(20, 13, 4, 4);
                    g2.drawLine(22, 20, 22, 30);
                }

                g2.dispose();
            }
        };
    }

    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\n", "<br/>");
    }
}
