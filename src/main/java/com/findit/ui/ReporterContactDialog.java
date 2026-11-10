package com.findit.ui;

import com.findit.service.ImageStorageService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URI;
import javax.imageio.ImageIO;

/**
 * Premium modal dialog displaying the reporter's profile picture, full name,
 * role, and verified email address with 1-click clipboard copy and direct mail actions.
 */
public class ReporterContactDialog extends JDialog {

    public ReporterContactDialog(Window parent, String reporterName, String reporterEmail, String profileImagePath, String role) {
        super(parent, "FINDIT - Reporter Information", ModalityType.APPLICATION_MODAL);

        setSize(480, 440);
        setMinimumSize(new Dimension(420, 380));
        setLocationRelativeTo(parent);
        setResizable(false);

        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBackground(AppTheme.BG_MAIN);
        root.setBorder(new EmptyBorder(22, 24, 22, 24));

        // Card Container
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                AppTheme.applyQualityRendering(g2);
                int w = getWidth();
                int h = getHeight();
                g2.setColor(AppTheme.BG_CARD);
                g2.fillRoundRect(0, 0, w, h, 14, 14);
                g2.setColor(AppTheme.BORDER);
                g2.drawRoundRect(0, 0, w - 1, h - 1, 14, 14);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(20, 22, 20, 22));

        // 1. Avatar (70x70)
        int avatarSize = 70;
        JComponent avatarComponent = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                AppTheme.applyQualityRendering(g2);

                boolean drawn = false;
                if (profileImagePath != null && !profileImagePath.isBlank()) {
                    File f = new File(profileImagePath);
                    if (f.exists() && f.isFile()) {
                        try {
                            BufferedImage orig = ImageIO.read(f);
                            if (orig != null) {
                                int w = orig.getWidth();
                                int h = orig.getHeight();
                                int minDim = Math.min(w, h);
                                int x = (w - minDim) / 2;
                                int y = (h - minDim) / 2;
                                BufferedImage square = orig.getSubimage(x, y, minDim, minDim);

                                g2.setClip(new Ellipse2D.Float(2, 2, avatarSize - 4, avatarSize - 4));
                                g2.drawImage(square, 2, 2, avatarSize - 4, avatarSize - 4, null);
                                g2.setClip(null);

                                g2.setColor(AppTheme.ACCENT_CYAN);
                                g2.setStroke(new BasicStroke(2.2f));
                                g2.draw(new Ellipse2D.Float(2, 2, avatarSize - 4, avatarSize - 4));
                                drawn = true;
                            }
                        } catch (Exception ignored) {}
                    }
                }

                if (!drawn) {
                    g2.setPaint(new GradientPaint(0, 0, AppTheme.PRIMARY_INDIGO, avatarSize, avatarSize, AppTheme.ACCENT_CYAN));
                    g2.fill(new Ellipse2D.Float(0, 0, avatarSize, avatarSize));

                    String initial = (reporterName != null && !reporterName.isBlank())
                            ? reporterName.substring(0, 1).toUpperCase() : "U";
                    g2.setColor(Color.WHITE);
                    g2.setFont(AppTheme.font(Font.BOLD, 26f));
                    FontMetrics fm = g2.getFontMetrics();
                    int tx = (avatarSize - fm.stringWidth(initial)) / 2;
                    int ty = ((avatarSize - fm.getHeight()) / 2) + fm.getAscent();
                    g2.drawString(initial, tx, ty);
                }

                g2.dispose();
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(avatarSize, avatarSize);
            }

            @Override
            public Dimension getMaximumSize() {
                return new Dimension(avatarSize, avatarSize);
            }
        };
        avatarComponent.setAlignmentX(Component.CENTER_ALIGNMENT);

        // 2. Name & Role
        JLabel nameLabel = new JLabel(reporterName != null ? reporterName : "Anonymous Reporter");
        nameLabel.setFont(AppTheme.font(Font.BOLD, 18f));
        nameLabel.setForeground(Color.WHITE);
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        boolean isAdmin = "ADMIN".equalsIgnoreCase(role);
        JLabel roleBadge = new JLabel(" " + (isAdmin ? "ADMINISTRATOR" : "VERIFIED REPORTER") + " ");
        roleBadge.setFont(AppTheme.font(Font.BOLD, 11f));
        roleBadge.setOpaque(true);
        if (isAdmin) {
            roleBadge.setBackground(AppTheme.PURPLE_BG);
            roleBadge.setForeground(AppTheme.PURPLE_TEXT);
            roleBadge.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(AppTheme.PURPLE, 1, true),
                    BorderFactory.createEmptyBorder(2, 8, 2, 8)
            ));
        } else {
            roleBadge.setBackground(AppTheme.INFO_BG);
            roleBadge.setForeground(AppTheme.INFO_TEXT);
            roleBadge.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(AppTheme.INFO, 1, true),
                    BorderFactory.createEmptyBorder(2, 8, 2, 8)
            ));
        }
        roleBadge.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(avatarComponent);
        card.add(Box.createVerticalStrut(10));
        card.add(nameLabel);
        card.add(Box.createVerticalStrut(6));
        card.add(roleBadge);
        card.add(Box.createVerticalStrut(16));

        // 3. Email Section
        JPanel emailBox = new JPanel(new BorderLayout(10, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                AppTheme.applyQualityRendering(g2);
                g2.setColor(AppTheme.FIELD_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(AppTheme.FIELD_BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
            }
        };
        emailBox.setOpaque(false);
        emailBox.setBorder(new EmptyBorder(8, 12, 8, 8));
        emailBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));

        String emailText = (reporterEmail != null && !reporterEmail.isBlank()) ? reporterEmail : "No email listed";
        JLabel emailLabel = new JLabel(emailText);
        emailLabel.setFont(AppTheme.font(Font.BOLD, 13.5f));
        emailLabel.setForeground(new Color(0x38, 0xBD, 0xF8)); // Bright cyan

        RoundedButton copyBtn = new RoundedButton("Copy", RoundedButton.Style.PRIMARY);
        copyBtn.setPreferredSize(new Dimension(75, 30));
        copyBtn.setFont(AppTheme.font(Font.BOLD, 11.5f));
        copyBtn.addActionListener(e -> {
            if (reporterEmail != null && !reporterEmail.isBlank()) {
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(reporterEmail), null);
                copyBtn.setText("Copied!");
                copyBtn.setStyle(RoundedButton.Style.SECONDARY);
                Timer timer = new Timer(1800, ev -> {
                    copyBtn.setText("Copy");
                    copyBtn.setStyle(RoundedButton.Style.PRIMARY);
                });
                timer.setRepeats(false);
                timer.start();
            }
        });

        emailBox.add(emailLabel, BorderLayout.CENTER);
        if (reporterEmail != null && !reporterEmail.isBlank()) {
            emailBox.add(copyBtn, BorderLayout.EAST);
        }

        JLabel emailHeader = new JLabel("Contact Email Address");
        emailHeader.setFont(AppTheme.font(Font.BOLD, 11.5f));
        emailHeader.setForeground(AppTheme.TEXT_SECONDARY);
        emailHeader.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel emailSection = new JPanel();
        emailSection.setLayout(new BoxLayout(emailSection, BoxLayout.Y_AXIS));
        emailSection.setOpaque(false);
        emailSection.setAlignmentX(Component.CENTER_ALIGNMENT);
        emailSection.add(emailHeader);
        emailSection.add(Box.createVerticalStrut(4));
        emailSection.add(emailBox);

        card.add(emailSection);
        card.add(Box.createVerticalStrut(14));

        // Helpful tip
        JLabel tipLabel = new JLabel("<html><center style='color:#94A3B8; font-size:11px;'>"
                + "You can reach out directly via email to coordinate item verification and safe return."
                + "</center></html>");
        tipLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(tipLabel);

        root.add(card, BorderLayout.CENTER);

        // Bottom Controls
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        footer.setOpaque(false);

        if (reporterEmail != null && !reporterEmail.isBlank() && Desktop.isDesktopSupported()
                && Desktop.getDesktop().isSupported(Desktop.Action.MAIL)) {
            RoundedButton mailBtn = new RoundedButton("Send Email", RoundedButton.Style.PRIMARY);
            mailBtn.setPreferredSize(new Dimension(115, 36));
            mailBtn.setFont(AppTheme.font(Font.BOLD, 12f));
            mailBtn.addActionListener(e -> {
                try {
                    Desktop.getDesktop().mail(new URI("mailto:" + reporterEmail + "?subject=FINDIT%20Item%20Inquiry"));
                } catch (Exception ex) {
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(reporterEmail), null);
                    DialogUtils.showInfo(this, "Email Client", "Copied " + reporterEmail + " to clipboard.");
                }
            });
            footer.add(mailBtn);
        }

        RoundedButton closeBtn = new RoundedButton("Close", RoundedButton.Style.SECONDARY);
        closeBtn.setPreferredSize(new Dimension(90, 36));
        closeBtn.setFont(AppTheme.font(Font.BOLD, 12f));
        closeBtn.addActionListener(e -> dispose());
        footer.add(closeBtn);

        root.add(footer, BorderLayout.SOUTH);

        setContentPane(root);
    }
}

