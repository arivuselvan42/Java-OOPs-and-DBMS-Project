package com.findit.ui;

import com.findit.dao.ClaimDAO;
import com.findit.model.Claim;
import com.findit.model.Report;
import com.findit.model.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.concurrent.ExecutionException;

/**
 * Modern SaaS dialog for submitting ownership claims on found items in FindIt.
 */
public class ClaimSubmissionFrame extends JDialog {

    private final Report report;
    private final User currentUser;
    private final JTextArea detailsArea = new JTextArea(6, 25);

    private final RoundedButton submitBtn = new RoundedButton("Submit Claim", RoundedButton.Style.PRIMARY);
    private final RoundedButton cancelBtn = new RoundedButton("Cancel", RoundedButton.Style.SECONDARY);

    private boolean busy = false;
    private boolean claimSubmitted = false;

    public boolean isClaimSubmitted() {
        return claimSubmitted;
    }

    public ClaimSubmissionFrame(Report report, User user) {
        this(null, report, user);
    }

    public ClaimSubmissionFrame(Window owner, Report report, User user) {
        super(owner, "FINDIT - Submit Ownership Claim", ModalityType.APPLICATION_MODAL);
        this.report = report;
        this.currentUser = user;

        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setSize(560, 560);
        setMinimumSize(new Dimension(500, 500));
        setLocationRelativeTo(owner);
        setResizable(false);

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(AppTheme.BG_MAIN);
        root.setBorder(new EmptyBorder(20, 24, 20, 24));

        // Card Panel
        JPanel card = new JPanel(new BorderLayout(0, 16)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                AppTheme.applyQualityRendering(g2);
                int w = getWidth();
                int h = getHeight();
                g2.setColor(AppTheme.BG_CARD);
                g2.fillRoundRect(0, 0, w, h, 16, 16);
                g2.setColor(AppTheme.BORDER);
                g2.drawRoundRect(0, 0, w - 1, h - 1, 16, 16);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(22, 26, 22, 26));

        // Header
        JPanel header = new JPanel(new GridLayout(2, 1, 0, 4));
        header.setOpaque(false);

        String itemName = report.getItem() != null ? report.getItem().getItemName() : "Found Item";
        JLabel title = new JLabel("Claim Item: " + itemName);
        title.setFont(AppTheme.font(Font.BOLD, 18f));
        title.setForeground(AppTheme.TEXT_PRIMARY);

        JLabel subtitle = new JLabel("Report #" + report.getReportId() + " | Found at: " + report.getLocation() + " | " + report.getReportDate());
        subtitle.setFont(AppTheme.font(Font.PLAIN, 12f));
        subtitle.setForeground(AppTheme.TEXT_SECONDARY);

        header.add(title);
        header.add(subtitle);

        // Center Content: Info Banner + Details input
        JPanel center = new JPanel(new BorderLayout(0, 14));
        center.setOpaque(false);

        // Information Callout Banner
        JPanel banner = new JPanel(new BorderLayout(10, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                AppTheme.applyQualityRendering(g2);
                g2.setColor(AppTheme.INFO_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(AppTheme.ACCENT_CYAN);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
            }
        };
        banner.setOpaque(false);
        banner.setBorder(new EmptyBorder(12, 14, 12, 14));

        JLabel infoText = new JLabel("<html><body style='width: 380px;'>"
                + "<b>Proof of Ownership:</b><br/>"
                + "Provide distinguishing details such as serial numbers, unique marks, stickers, color combinations, contents, or purchase details to verify your claim."
                + "</body></html>");
        infoText.setFont(AppTheme.font(Font.PLAIN, 12f));
        infoText.setForeground(AppTheme.INFO_TEXT);
        banner.add(infoText, BorderLayout.CENTER);

        // Text area
        detailsArea.setLineWrap(true);
        detailsArea.setWrapStyleWord(true);
        detailsArea.setFont(AppTheme.font(Font.PLAIN, 13f));
        detailsArea.setBackground(AppTheme.FIELD_BG);
        detailsArea.setForeground(AppTheme.TEXT_PRIMARY);
        detailsArea.setCaretColor(AppTheme.ACCENT_CYAN);
        detailsArea.setBorder(new EmptyBorder(10, 12, 10, 12));

        JScrollPane scrollPane = new JScrollPane(detailsArea);
        scrollPane.setBorder(BorderFactory.createLineBorder(AppTheme.BORDER, 1, true));

        JPanel inputGroup = new JPanel(new BorderLayout(0, 6));
        inputGroup.setOpaque(false);
        JLabel inputLabel = new JLabel("Your Explanation & Identification Details");
        inputLabel.setFont(AppTheme.font(Font.BOLD, 12f));
        inputLabel.setForeground(AppTheme.TEXT_PRIMARY);

        inputGroup.add(inputLabel, BorderLayout.NORTH);
        inputGroup.add(scrollPane, BorderLayout.CENTER);

        center.add(banner, BorderLayout.NORTH);
        center.add(inputGroup, BorderLayout.CENTER);

        // Buttons
        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonRow.setOpaque(false);

        cancelBtn.setPreferredSize(new Dimension(100, 38));
        submitBtn.setPreferredSize(new Dimension(135, 38));

        buttonRow.add(cancelBtn);
        buttonRow.add(submitBtn);

        card.add(header, BorderLayout.NORTH);
        card.add(center, BorderLayout.CENTER);
        card.add(buttonRow, BorderLayout.SOUTH);

        root.add(card, BorderLayout.CENTER);
        setContentPane(root);

        // Actions
        submitBtn.addActionListener(e -> submitClaim());
        cancelBtn.addActionListener(e -> dispose());
    }

    private void setBusy(boolean busy) {
        this.busy = busy;
        submitBtn.setEnabled(!busy);
        cancelBtn.setEnabled(!busy);
        submitBtn.setText(busy ? "Submitting..." : "Submit Claim");
        setCursor(Cursor.getPredefinedCursor(busy ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    private void submitClaim() {
        if (busy) return;

        String details = detailsArea.getText().trim();
        if (details.isEmpty()) {
            DialogUtils.showWarning(this, "Missing Details", "Please provide proof and details to identify the item.");
            return;
        }

        if (!"OPEN".equalsIgnoreCase(report.getStatus())) {
            DialogUtils.showWarning(this, "Unavailable", "This report is no longer available for claims.");
            return;
        }

        if (!"FOUND".equalsIgnoreCase(report.getReportType())) {
            DialogUtils.showWarning(this, "Invalid Report", "Claims can only be submitted for found items.");
            return;
        }

        Claim claim = new Claim();
        claim.setReportId(report.getReportId());
        claim.setClaimantId(currentUser.getUserId());
        claim.setClaimDetails(details);
        claim.setClaimStatus("PENDING");

        setBusy(true);

        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                return new ClaimDAO().submitClaim(claim);
            }

            @Override
            protected void done() {
                setBusy(false);
                try {
                    if (Boolean.TRUE.equals(get())) {
                        // Notify report owner
                        try {
                            String itemName = report.getItem() != null ? report.getItem().getItemName() : "Item #" + report.getReportId();
                            new com.findit.dao.NotificationDAO().createNotification(
                                    report.getUserId(),
                                    "New Claim Submitted",
                                    currentUser.getName() + " submitted a claim for your reported item '" + itemName + "'.",
                                    report.getReportId()
                            );
                        } catch (Exception ignored) {}

                        DialogUtils.showSuccess(
                                ClaimSubmissionFrame.this,
                                "Claim Submitted",
                                "Your claim has been submitted with status PENDING. An administrator will review your evidence."
                        );
                        claimSubmitted = true;
                        dispose();
                    } else {
                        DialogUtils.showError(ClaimSubmissionFrame.this, "Submission Failed", "The claim could not be processed.");
                    }
                } catch (Exception ex) {
                    DialogUtils.showError(ClaimSubmissionFrame.this, "Database Error", ex.getMessage());
                }
            }
        }.execute();
    }
}
