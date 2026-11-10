package com.findit.ui;

import com.findit.model.Report;
import com.findit.model.User;
import com.findit.service.SmartMatchingService;
import com.findit.service.SmartMatchingService.MatchResult;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.List;

/**
 * Premium dark-themed dialog presenting AI/heuristic smart match results.
 */
public class SmartMatchesDialog extends JDialog {

    private final Report targetReport;
    private final User currentUser;
    private final List<MatchResult> matches;
    private final Runnable onClaimSubmitted;

    public SmartMatchesDialog(Window parent, Report targetReport, User currentUser,
                              List<MatchResult> matches, Runnable onClaimSubmitted) {
        super(parent, "FINDIT - Smart Match Suggestions", ModalityType.APPLICATION_MODAL);
        this.targetReport = targetReport;
        this.currentUser = currentUser;
        this.matches = matches;
        this.onClaimSubmitted = onClaimSubmitted;

        setSize(860, 620);
        setMinimumSize(new Dimension(720, 480));
        setLocationRelativeTo(parent);

        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBackground(AppTheme.BG_MAIN);
        root.setBorder(new EmptyBorder(20, 24, 20, 24));

        // Header
        JPanel header = new JPanel(new BorderLayout(14, 0));
        header.setOpaque(false);

        JPanel headerStack = new JPanel(new GridLayout(2, 1, 0, 3));
        headerStack.setOpaque(false);

        String targetName = targetReport.getItem() != null ? targetReport.getItem().getItemName() : "Item";
        JLabel title = new JLabel("Smart Matches for: " + targetName + " (" + targetReport.getReportType() + ")");
        title.setFont(AppTheme.font(Font.BOLD, 18f));
        title.setForeground(AppTheme.TEXT_PRIMARY);

        JLabel sub = new JLabel("Found " + matches.size() + " potential match(es) ranked by heuristic similarity score");
        sub.setFont(AppTheme.font(Font.PLAIN, 12.5f));
        sub.setForeground(AppTheme.TEXT_SECONDARY);

        headerStack.add(title);
        headerStack.add(sub);
        header.add(headerStack, BorderLayout.WEST);

        root.add(header, BorderLayout.NORTH);

        // Center: Scrollable list of match cards
        ScrollableListPanel listPanel = new ScrollableListPanel();

        if (matches.isEmpty()) {
            JPanel emptyPanel = new JPanel(new GridBagLayout());
            emptyPanel.setOpaque(false);
            JLabel emptyLabel = new JLabel("No matching reports found with score >= 35%. Check back later!");
            emptyLabel.setFont(AppTheme.font(Font.ITALIC, 14f));
            emptyLabel.setForeground(AppTheme.TEXT_MUTED);
            emptyPanel.add(emptyLabel);
            listPanel.add(emptyPanel);
        } else {
            for (MatchResult match : matches) {
                listPanel.add(buildMatchCard(match));
                listPanel.add(Box.createVerticalStrut(12));
            }
        }

        JScrollPane scrollPane = new JScrollPane(listPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(AppTheme.BG_MAIN);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(14);
        root.add(scrollPane, BorderLayout.CENTER);

        // Bottom Controls
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        footer.setOpaque(false);

        RoundedButton closeBtn = new RoundedButton("Close", RoundedButton.Style.SECONDARY);
        closeBtn.setPreferredSize(new Dimension(100, 38));
        closeBtn.addActionListener(e -> dispose());
        footer.add(closeBtn);

        root.add(footer, BorderLayout.SOUTH);

        setContentPane(root);
    }

    /**
     * Scrollable panel that tracks viewport width so match cards stretch
     * to the exact window width with zero horizontal scrolling.
     */
    private static class ScrollableListPanel extends JPanel implements Scrollable {
        public ScrollableListPanel() {
            super();
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setOpaque(false);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 64;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true; // Match viewport width exactly; no horizontal scrollbar
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    private JPanel buildMatchCard(MatchResult match) {
        Report report = match.getReport();
        int score = match.getScore();

        JPanel card = new JPanel(new BorderLayout(14, 0)) {
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
        card.setBorder(new EmptyBorder(14, 16, 14, 16));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));
        card.setPreferredSize(new Dimension(0, 118));

        // Left: Score Pill
        JPanel scorePanel = new JPanel(new GridBagLayout());
        scorePanel.setOpaque(false);
        scorePanel.setPreferredSize(new Dimension(85, 0));

        JLabel scorePill = new JLabel(score + "%", SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                AppTheme.applyQualityRendering(g2);
                Color bg = score >= 75 ? AppTheme.SUCCESS_BG : (score >= 50 ? AppTheme.INFO_BG : AppTheme.WARNING_BG);
                Color border = score >= 75 ? AppTheme.SUCCESS : (score >= 50 ? AppTheme.ACCENT_CYAN : AppTheme.WARNING);
                g2.setColor(bg);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
                g2.setColor(border);
                g2.setStroke(new BasicStroke(1.2f));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1f, getHeight() - 1f, 12, 12));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        scorePill.setPreferredSize(new Dimension(72, 32));
        scorePill.setFont(AppTheme.font(Font.BOLD, 13f));
        scorePill.setForeground(score >= 75 ? AppTheme.SUCCESS_TEXT : (score >= 50 ? AppTheme.INFO_TEXT : AppTheme.WARNING_TEXT));
        scorePanel.add(scorePill);

        card.add(scorePanel, BorderLayout.WEST);

        // Center: Item Details & Reasons
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setOpaque(false);

        String itemName = report.getItem() != null ? report.getItem().getItemName() : "Unnamed";
        String catName = report.getItem() != null && report.getItem().getCategoryName() != null ? report.getItem().getCategoryName() : "General";

        JLabel nameLabel = new JLabel("#" + report.getReportId() + " " + itemName + "  |  " + report.getReportType() + " in " + catName);
        nameLabel.setFont(AppTheme.font(Font.BOLD, 14f));
        nameLabel.setForeground(AppTheme.TEXT_PRIMARY);

        JLabel metaLabel = new JLabel("Location: " + report.getLocation() + "  |  Date: " + report.getReportDate() + "  |  Status: " + report.getStatus());
        metaLabel.setFont(AppTheme.font(Font.PLAIN, 12f));
        metaLabel.setForeground(AppTheme.TEXT_SECONDARY);

        // Reasons summary with word-wrapping width constraint
        String reasonsStr = String.join("  |  ", match.getReasons());
        JLabel reasonsLabel = new JLabel("<html><body style='width: 440px;'><span style='color: #06B6D4;'><b>Match factors:</b></span> " + reasonsStr + "</body></html>");
        reasonsLabel.setFont(AppTheme.font(Font.PLAIN, 11.5f));
        reasonsLabel.setForeground(AppTheme.TEXT_MUTED);

        centerPanel.add(nameLabel);
        centerPanel.add(Box.createVerticalStrut(3));
        centerPanel.add(metaLabel);
        centerPanel.add(Box.createVerticalStrut(4));
        centerPanel.add(reasonsLabel);

        card.add(centerPanel, BorderLayout.CENTER);

        // Right Action: View / Claim (pinned to EAST so always in view without scrolling)
        boolean hasImage = report.getItem() != null && report.getItem().getImagePath() != null && !report.getItem().getImagePath().isBlank();
        boolean canClaim = "FOUND".equalsIgnoreCase(report.getReportType()) && "OPEN".equalsIgnoreCase(report.getStatus()) && report.getUserId() != currentUser.getUserId();

        int actionsWidth = (hasImage && canClaim) ? 175 : ((hasImage || canClaim) ? 95 : 10);
        JPanel rightActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 12));
        rightActions.setOpaque(false);
        rightActions.setPreferredSize(new Dimension(actionsWidth, 44));

        if (hasImage) {
            RoundedButton imgBtn = new RoundedButton("Image", RoundedButton.Style.SECONDARY);
            imgBtn.setPreferredSize(new Dimension(75, 34));
            imgBtn.addActionListener(e -> new ImageViewerDialog(this, itemName, report.getItem().getImagePath()).setVisible(true));
            rightActions.add(imgBtn);
        }

        if (canClaim) {
            RoundedButton claimBtn = new RoundedButton("Claim", RoundedButton.Style.PRIMARY);
            claimBtn.setPreferredSize(new Dimension(85, 34));
            claimBtn.addActionListener(e -> {
                ClaimSubmissionFrame claimFrame = new ClaimSubmissionFrame(this, report, currentUser);
                claimFrame.setVisible(true);
                if (claimFrame.isClaimSubmitted()) {
                    if (onClaimSubmitted != null) {
                        onClaimSubmitted.run();
                    }
                    dispose();
                }
            });
            rightActions.add(claimBtn);
        }

        card.add(rightActions, BorderLayout.EAST);

        return card;
    }
}

