package com.findit.ui;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Custom table cell renderer that displays modern, SaaS-style rounded pill badges
 * with a high-contrast pastel palette for optimal legibility.
 */
public class StatusBadgeRenderer extends DefaultTableCellRenderer {

    // High-Contrast Pastel Badge Colors
    public static final Color BADGE_SUCCESS_BG   = new Color(0xDC, 0xFC, 0xE7); // Soft mint #DCFCE7
    public static final Color BADGE_SUCCESS_FG   = new Color(0x15, 0x80, 0x3D); // Deep forest green #15803D
    public static final Color BADGE_SUCCESS_DOT  = new Color(0x16, 0xA3, 0x4A); // Emerald dot #16A34A

    public static final Color BADGE_WARNING_BG   = new Color(0xFE, 0xF3, 0xC7); // Soft amber #FEF3C7
    public static final Color BADGE_WARNING_FG   = new Color(0x92, 0x40, 0x0E); // Deep warm amber #92400E
    public static final Color BADGE_WARNING_DOT  = new Color(0xD9, 0x77, 0x06); // Amber dot #D97706

    public static final Color BADGE_PURPLE_BG    = new Color(0xF3, 0xE8, 0xFF); // Soft lilac #F3E8FF
    public static final Color BADGE_PURPLE_FG    = new Color(0x6B, 0x21, 0xA8); // Deep royal purple #6B21A8
    public static final Color BADGE_PURPLE_DOT   = new Color(0x93, 0x33, 0xEA); // Purple dot #9333EA

    public static final Color BADGE_DANGER_BG    = new Color(0xFF, 0xE4, 0xE6); // Soft rose #FFE4E6
    public static final Color BADGE_DANGER_FG    = new Color(0x9F, 0x12, 0x39); // Deep rose crimson #9F1239

    public static final Color BADGE_INFO_BG      = new Color(0xE0, 0xF2, 0xFE); // Soft sky #E0F2FE
    public static final Color BADGE_INFO_FG      = new Color(0x03, 0x69, 0xA1); // Deep sky blue #0369A1

    public static final Color BADGE_RETURNED_BG  = new Color(0xF1, 0xF5, 0xF9); // Soft slate #F1F5F9
    public static final Color BADGE_RETURNED_FG  = new Color(0x47, 0x55, 0x69); // Slate text #475569
    public static final Color BADGE_RETURNED_DOT = new Color(0x94, 0xA3, 0xB8);

    public static class BadgeComponent extends JPanel {
        private String text = "";
        private Color bgColor = BADGE_RETURNED_BG;
        private Color fgColor = BADGE_RETURNED_FG;
        private Color dotColor = null;
        private final Font badgeFont = AppTheme.font(Font.BOLD, 11.5f);

        public BadgeComponent() {
            setOpaque(false);
            setLayout(new GridBagLayout());
        }

        public void configure(String text, Color bg, Color fg, Color dot) {
            this.text = text != null ? text.replace('_', ' ') : "";
            this.bgColor = bg;
            this.fgColor = fg;
            this.dotColor = dot;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (text.isEmpty()) return;

            Graphics2D g2 = (Graphics2D) g.create();
            AppTheme.applyQualityRendering(g2);
            g2.setFont(badgeFont);

            FontMetrics fm = g2.getFontMetrics();
            int textWidth = fm.stringWidth(text);

            int dotSize = dotColor != null ? 7 : 0;
            int dotGap = dotColor != null ? 6 : 0;

            int badgeWidth = textWidth + dotSize + dotGap + 20;
            int badgeHeight = 24;

            int x = (getWidth() - badgeWidth) / 2;
            int y = (getHeight() - badgeHeight) / 2;

            // Draw pill background with subtle border
            g2.setColor(bgColor);
            g2.fill(new RoundRectangle2D.Float(x, y, badgeWidth, badgeHeight, 12, 12));

            int curX = x + 10;
            // Draw dot if present
            if (dotColor != null) {
                g2.setColor(dotColor);
                int dotY = y + (badgeHeight - dotSize) / 2;
                g2.fillOval(curX, dotY, dotSize, dotSize);
                curX += dotSize + dotGap;
            }

            // Draw text
            g2.setColor(fgColor);
            int textY = y + ((badgeHeight - fm.getHeight()) / 2) + fm.getAscent();
            g2.drawString(text, curX, textY);

            g2.dispose();
        }
    }

    private final BadgeComponent badge = new BadgeComponent();
    private final boolean isTypeRenderer;

    public StatusBadgeRenderer() {
        this(false);
    }

    public StatusBadgeRenderer(boolean isTypeRenderer) {
        this.isTypeRenderer = isTypeRenderer;
    }

    @Override
    public Component getTableCellRendererComponent(
            JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

        String val = value != null ? value.toString().trim() : "";

        // Paint matching row background
        if (isSelected) {
            badge.setBackground(ModernTable.SELECTION_BG);
        } else {
            badge.setBackground(row % 2 == 0 ? ModernTable.TABLE_BG : ModernTable.TABLE_ROW_ALT);
        }
        badge.setOpaque(true);

        if (isTypeRenderer) {
            if ("LOST".equalsIgnoreCase(val)) {
                badge.configure("LOST", BADGE_DANGER_BG, BADGE_DANGER_FG, null);
            } else if ("FOUND".equalsIgnoreCase(val)) {
                badge.configure("FOUND", BADGE_INFO_BG, BADGE_INFO_FG, null);
            } else {
                badge.configure(val, BADGE_RETURNED_BG, BADGE_RETURNED_FG, null);
            }
        } else {
            switch (val.toUpperCase()) {
                case "OPEN" -> badge.configure(
                        "OPEN",
                        BADGE_SUCCESS_BG,
                        BADGE_SUCCESS_FG,
                        BADGE_SUCCESS_DOT
                );
                case "CLAIM_PENDING", "PENDING" -> badge.configure(
                        "CLAIM PENDING",
                        BADGE_WARNING_BG,
                        BADGE_WARNING_FG,
                        BADGE_WARNING_DOT
                );
                case "CLAIMED" -> badge.configure(
                        "CLAIMED",
                        BADGE_PURPLE_BG,
                        BADGE_PURPLE_FG,
                        BADGE_PURPLE_DOT
                );
                case "APPROVED" -> badge.configure(
                        "APPROVED",
                        BADGE_SUCCESS_BG,
                        BADGE_SUCCESS_FG,
                        BADGE_SUCCESS_DOT
                );
                case "REJECTED" -> badge.configure(
                        "REJECTED",
                        BADGE_DANGER_BG,
                        BADGE_DANGER_FG,
                        null
                );
                case "RETURNED" -> badge.configure(
                        "RETURNED",
                        BADGE_RETURNED_BG,
                        BADGE_RETURNED_FG,
                        BADGE_RETURNED_DOT
                );
                default -> badge.configure(val, BADGE_RETURNED_BG, BADGE_RETURNED_FG, null);
            }
        }

        return badge;
    }
}
